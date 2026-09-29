/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 * Copyright 2026 SchildiChat
 *
 * SPDX-License-Identifier: AGPL-3.0-only.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline

import chat.schildi.lib.preferences.ScPreferencesStore
import chat.schildi.lib.preferences.ScPrefs
import co.touchlab.kermit.Logger
import io.element.android.features.messages.impl.timeline.di.LiveTimeline
import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import io.element.android.libraries.core.coroutine.childScope
import io.element.android.libraries.di.annotations.RoomCoroutineScope
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.ThreadId
import io.element.android.libraries.matrix.api.core.UniqueId
import io.element.android.libraries.matrix.api.room.CreateTimelineParams
import io.element.android.libraries.matrix.api.room.JoinedRoom
import io.element.android.libraries.matrix.api.timeline.MatrixTimelineItem
import io.element.android.libraries.matrix.api.timeline.Timeline
import io.element.android.libraries.matrix.api.timeline.TimelineProvider
import io.element.android.libraries.matrix.api.timeline.item.virtual.VirtualTimelineItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.Closeable
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

data class TimelineItemsState(
    val items: List<MatrixTimelineItem>,
    val sourceTimelines: List<Timeline>,
    val isLive: Boolean,
    val mergeOffset: Int = if (isLive) -1 else 0,
) {
    val preferredTimeline: Timeline
        get() = sourceTimelines.first()
    val detachedSize = mergeOffset.coerceAtLeast(0)
    val liveSize = if (isLive && mergeOffset >= 0) items.size - mergeOffset else 0
}

sealed interface DedupeId {
    val preferLive: Boolean
    data class Event(val value: EventId) : DedupeId {
        override val preferLive = true
    }
    data class Virtual(
        val value: UniqueId,
        override val preferLive: Boolean,
    ) : DedupeId
}

/**
 * This controller is responsible for using the right timeline to display messages and make associated actions.
 * It can be focused on the live timeline or on a detached timeline (focusing an unknown event), or a merge of both.
 */
class ScTimelineController(
    private val room: JoinedRoom,
    private val scPreferencesStore: ScPreferencesStore?,
    @LiveTimeline private val liveTimeline: Timeline = room.liveTimeline,
    private val initialDetachedTimeline: Timeline? = null,
    @RoomCoroutineScope private val roomCoroutineScope: CoroutineScope = room.roomCoroutineScope,
    dispatchers: CoroutineDispatchers = CoroutineDispatchers.Default,
) : Closeable, TimelineProvider {
    private val log = Logger.withTag("TimelineController")

    private val coroutineScope = roomCoroutineScope.childScope(dispatchers.computation, "TimelineController")

    private val detachedTimelineFlow = MutableStateFlow(Optional.ofNullable(initialDetachedTimeline))

    @OptIn(ExperimentalCoroutinesApi::class)
    val timelineState = combine(
        liveTimeline.timelineItems,
        detachedTimelineFlow.flatMapLatest {
            it.getOrNull()?.let { timeline -> timeline.timelineItems.map { Pair(timeline, it) } } ?: flowOf(null)
        },
        scPreferencesStore?.settingFlow(ScPrefs.ALLOW_LIVE_TIMELINE_MERGE) ?: flowOf(ScPrefs.ALLOW_LIVE_TIMELINE_MERGE.defaultValue),
    ) { liveItems, detachedItems, allowMerge ->
        if (detachedItems == null || detachedItems.second.isEmpty()) {
            TimelineItemsState(liveItems, listOf(liveTimeline), isLive = true)
        } else if (allowMerge) {
            mergeTimelines(
                liveItems,
                detachedItems.second,
                detachedItems.first,
            )
        } else {
            TimelineItemsState(
                detachedItems.second,
                listOf(detachedItems.first),
                isLive = false,
                mergeOffset = detachedItems.second.size,
            )
        }
    }.stateIn(roomCoroutineScope, SharingStarted.WhileSubscribed(), null)

    private fun MatrixTimelineItem.toDedupeId() = when (this) {
        is MatrixTimelineItem.Event -> eventId?.let(DedupeId::Event)
        is MatrixTimelineItem.Virtual -> DedupeId.Virtual(
            uniqueId,
            preferLive = when (virtual) {
                is VirtualTimelineItem.DayDivider -> false
                is VirtualTimelineItem.TypingNotification -> true
                VirtualTimelineItem.LastForwardIndicator -> true
                is VirtualTimelineItem.LoadingIndicator -> false
                VirtualTimelineItem.ReadMarker -> true
                VirtualTimelineItem.RoomBeginning -> false
            }
        )
        // No de-dupe, should be unused anyway probably?
        MatrixTimelineItem.Other -> null
    }

    private fun mergeTimelines(
        liveItems: List<MatrixTimelineItem>,
        detachedItems: List<MatrixTimelineItem>,
        detachedTimeline: Timeline,
    ): TimelineItemsState {
        val liveIds = liveItems.mapNotNull { it.toDedupeId() }.toSet()
        val liveEventIds = liveIds.mapNotNull { (it as? DedupeId.Event)?.value }
        val overlap = detachedItems.any { (it as? MatrixTimelineItem.Event)?.eventId in liveEventIds }
        return if (!overlap) {
            TimelineItemsState(detachedItems, listOf(detachedTimeline), isLive = false)
        } else if (detachedItems.any { it is MatrixTimelineItem.Event && it.eventId !in liveEventIds }) {
            // Merge!
            var hadEventSinceLastDateHeader = false
            var lastDateHeader = -1
            var detachedAddIndex = 0
            val dayHeadersToRemove = mutableListOf<Int>()
            val filteredDetachedItems = detachedItems.filter { item ->
                val allowedInDetached = item.toDedupeId()?.let { !it.preferLive || it !in liveIds } != false
                if (allowedInDetached && item is MatrixTimelineItem.Event) {
                    hadEventSinceLastDateHeader = true
                } else if ((item as? MatrixTimelineItem.Virtual)?.virtual is VirtualTimelineItem.DayDivider) {
                    if (!hadEventSinceLastDateHeader && lastDateHeader >= 0) {
                        dayHeadersToRemove.add(lastDateHeader)
                    }
                    lastDateHeader = detachedAddIndex
                    hadEventSinceLastDateHeader = false
                }
                if (allowedInDetached) {
                    detachedAddIndex++
                }
                allowedInDetached
            }.let {
                if (dayHeadersToRemove.isEmpty()) {
                    it
                } else {
                    it.toMutableList().apply {
                        if (!hadEventSinceLastDateHeader && lastDateHeader >= 0 && lastDateHeader !in dayHeadersToRemove) {
                            removeAt(lastDateHeader)
                        }
                        dayHeadersToRemove.asReversed().forEach { removeAt(it) }
                    }
                }
            }
            val detachedIds = detachedItems.mapNotNull { it.toDedupeId() }.toSet()

            // Filter out first day separator too if it's duplicated
            val firstLiveDay = (liveItems.firstOrNull {
                it is MatrixTimelineItem.Event || (it as? MatrixTimelineItem.Virtual)?.virtual is VirtualTimelineItem.DayDivider
            } as? MatrixTimelineItem.Virtual)?.virtual as? VirtualTimelineItem.DayDivider
            val lastDetachedTimestamp = filteredDetachedItems.asReversed().firstNotNullOfOrNull {
                it.toDateTimestamp(allowNonEventTimestamps = true)
            }

            var hasFilteredLiveDayHeader = firstLiveDay == null || lastDetachedTimestamp == null ||
                    firstLiveDay.timestamp.timestampToDedupable() != lastDetachedTimestamp.timestampToDedupable()
            val filteredLiveItems = liveItems.filter {
                if (!hasFilteredLiveDayHeader && (it as? MatrixTimelineItem.Virtual)?.virtual is VirtualTimelineItem.DayDivider) {
                    hasFilteredLiveDayHeader = true
                    false
                } else {
                    it.toDedupeId()?.let { it.preferLive || it !in detachedIds } != false
                }
            }

            val items = filteredDetachedItems + filteredLiveItems
            log.d { "Overlap in detached timeline at ${filteredLiveItems.size}/${items.size} via ${liveItems.size}+${detachedItems.size}" }
            TimelineItemsState(
                items,
                listOf(liveTimeline, detachedTimeline),
                isLive = true,
                mergeOffset = filteredDetachedItems.size,
            )
        } else {
            log.d { "Overlap in detached timeline fully covered by live timeline" }
            TimelineItemsState(liveItems, listOf(liveTimeline), isLive = true)
        }
    }

    // Don't dedupe timestamps by this unconditionally, just on the seam of two timelines!
    private fun Long.timestampToDedupable() = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy").format(
        LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    fun timelineItems(): Flow<List<MatrixTimelineItem>> = timelineState.map { it?.items.orEmpty() }.distinctUntilChanged()

    fun isLive(): Flow<Boolean> = timelineState.map { it?.isLive ?: (initialDetachedTimeline == null) }.distinctUntilChanged()

    suspend fun invokeOnCurrentTimeline(block: suspend (Timeline.() -> Unit)) {
        val timeline = detachedTimelineFlow.value.getOrNull()?.takeIf { timelineState.value?.isLive != true }
            ?: liveTimeline
        timeline.run {
            block(this)
        }
    }

    suspend fun focusOnEvent(
        eventId: EventId,
        threadRootId: ThreadId?,
        hideThreadedEvents: Boolean
    ): Result<EventFocusResult> {
        return if (threadRootId != null) {
            Result.success(EventFocusResult.IsInThread(threadRootId))
        } else {
            room.createTimeline(CreateTimelineParams.Focused(eventId), hideThreadedEvents)
                .onFailure {
                    if (it is CancellationException) {
                        throw it
                    }
                }
                .map { newDetachedTimeline ->
                    detachedTimelineFlow.getAndUpdate { current ->
                        if (current.isPresent) {
                            current.get().close()
                        }
                        Optional.of(newDetachedTimeline)
                    }
                    EventFocusResult.FocusedOnLive
                }
        }
    }

    /**
     * Makes sure the controller is focused on the live timeline.
     * This does close the detached timeline if any.
     */
    fun focusOnLive() {
        closeDetachedTimeline()
    }

    private fun closeDetachedTimeline() {
        detachedTimelineFlow.getAndUpdate {
            when {
                it.isPresent -> {
                    it.get().close()
                    Optional.empty()
                }
                else -> Optional.empty()
            }
        }
    }

    override fun close() {
        coroutineScope.cancel()
        closeDetachedTimeline()
    }

    suspend fun paginate(direction: Timeline.PaginationDirection, forceLiveTimeline: Boolean = false): Result<Boolean> {
        val preferLive = forceLiveTimeline || when (direction) {
            Timeline.PaginationDirection.BACKWARDS -> {
                // Prefer live if it almost consumed the detached one already anyway
                (timelineState.value?.detachedSize ?: 0) > 50
            }
            Timeline.PaginationDirection.FORWARDS -> {
                // Always prefer live for forwards, even though it's a no-op for the live timeline.
                isLive().first()
            }
        }
        val timeline = if (preferLive) liveTimeline else (detachedTimelineFlow.value.getOrNull() ?: liveTimeline)
        return timeline.paginate(direction)
            .onSuccess { hasReachedEnd ->
                if (direction == Timeline.PaginationDirection.FORWARDS && hasReachedEnd) {
                    log.i("Forward pagination reached end, live=$preferLive")
                    if (!preferLive) {
                        log.i("Forward pagination end reached while not live yet, paginate live backwards")
                        paginate(Timeline.PaginationDirection.BACKWARDS, forceLiveTimeline = true)
                    }
                }
            }
            .onFailure {
                log.i("Pagination failed, live=$preferLive, err=$it")
                if (direction == Timeline.PaginationDirection.FORWARDS && !preferLive) {
                    log.w("Forward pagination failed while not live yet, attempt paginate live backwards")
                    paginate(Timeline.PaginationDirection.BACKWARDS, forceLiveTimeline = true)
                }
            }
    }

    private val currentTimelineFlow = combine(detachedTimelineFlow, isLive()) { detached, isLive ->
        when {
            !isLive && detached.isPresent -> detached.get()
            else -> liveTimeline
        }
    }.stateIn(coroutineScope, SharingStarted.Eagerly, initialDetachedTimeline ?: liveTimeline)

    override fun activeTimelineFlow(): StateFlow<Timeline> {
        return currentTimelineFlow
    }
}

fun MatrixTimelineItem.toDateTimestamp(allowNonEventTimestamps: Boolean) =
    when (this) {
        is MatrixTimelineItem.Event -> event.timestamp
        is MatrixTimelineItem.Virtual -> if (allowNonEventTimestamps) {
            (virtual as? VirtualTimelineItem.DayDivider)?.timestamp
        } else {
            null
        }
        //MatrixTimelineItem.Other -> null
        else -> null
    }
