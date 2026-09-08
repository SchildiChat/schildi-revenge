package chat.schildi.revenge.push

import chat.schildi.lib.preferences.ScPrefs
import chat.schildi.matrixsdk.FULLY_READ_ACCOUNT_DATA_TYPE
import chat.schildi.matrixsdk.FullyReadContent
import chat.schildi.revenge.RevengeAppGraph
import chat.schildi.revenge.ScCoroutines
import chat.schildi.revenge.database.push.PushNotificationEventEntity
import chat.schildi.revenge.database.revengeDatabase
import chat.schildi.revenge.notification.NotificationId
import chat.schildi.revenge.notification.platformActiveNotificationRooms
import chat.schildi.revenge.notification.platformAutoDismissNotification
import chat.schildi.revenge.notification.platformNotify
import chat.schildi.revenge.notification.platformNotifyMessage
import chat.schildi.revenge.preferences.RevengePrefs
import co.touchlab.kermit.Logger
import io.element.android.libraries.core.coroutine.suspendLazy
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.exception.NotificationResolverException
import io.element.android.libraries.matrix.api.timeline.ReceiptType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

enum class PushResolutionOutcome {
    Done,
    TransientFailure,
    PermanentFailure,
}

object PushNotificationHandler {
    private const val MAX_FAILURES = 4

    private val log = Logger.withTag("PushNotificationHandler")
    private val scope = ScCoroutines.scope(Dispatchers.IO, "PushNotificationHandler")
    private val json = Json { ignoreUnknownKeys = true }

    private val pushDao = revengeDatabase.pushNotificationDao()


    fun onPushReceived(content: ByteArray, instance: String) {
        scope.launch {
            val sessionId = pushDao.getSessionId(instance)
            val payload = deserializeUnifiedPushPayload(content)
            if (sessionId == null) {
                log.w { "Ignoring push payload for unknown session via $instance" }
                return@launch
            }
            if (payload == null) {
                log.w { "Ignoring empty push payload for $instance / $sessionId" }
                return@launch
            }
            if (payload.notification == null) {
                log.w { "Ignoring missing push notification payload for $instance / $sessionId" }
                return@launch
            }
            val notification = payload.notification
            if (notification.roomId == null || notification.eventId == null) {
                // Commonly sent when the total unread count changes (e.g. the user read messages).
                // We don't really get any better signals to dismiss notifications,
                // so just iterate existing notifications to re-evaluate based on what we can without syncing.
                // (This is just a heuristic, doing it relyably would probably either require spec updates or syncing.)
                if (notification.counts?.unread != null) {
                    val activeRooms = platformActiveNotificationRooms().filter { it.sessionId == sessionId }
                    log.d { "Event-less push with unread count for $instance / $sessionId, re-evaluate ${activeRooms.size} notification" }
                    activeRooms.forEach { room ->
                        schedulePushResolutionWork(room.sessionId, room.roomId)
                    }
                } else {
                    log.d { "Ignoring event-less push notification for $instance / $sessionId" }
                }
                return@launch
            }
            pushDao.insertPush(
                PushNotificationEventEntity(
                    sessionId = sessionId,
                    roomId = notification.roomId,
                    eventId = notification.eventId,
                    pushSource = instance,
                    timestamp = System.currentTimeMillis(),
                )
            )
            if (RevengePrefs.getSetting(ScPrefs.DEBUG_NOTIFICATIONS)) {
                val notificationId = NotificationId.Event(
                    SessionId(sessionId),
                    RoomId(notification.roomId),
                    EventId(notification.eventId)
                )
                platformNotify(
                    notificationId,
                    title = "$sessionId ${notification.roomId}",
                    message = notification.eventId,
                )
            }
            schedulePushResolutionWork(sessionId, notification.roomId)
        }
    }

    suspend fun resolvePendingPushes(sessionId: SessionId, roomId: RoomId): PushResolutionOutcome {
        val pushes = pushDao.getPendingPushes(sessionId.value, roomId.value, maxFailures = MAX_FAILURES)
        if (pushes.isEmpty()) {
            log.d { "No pending pushes for $sessionId/$roomId, will still check for auto-dismiss" }
        } else {
            log.d { "Resolving ${pushes.size} pending pushes for $sessionId/$roomId" }
        }
        val isUnknownSession by suspendLazy { RevengeAppGraph.sessionStore.getSession(sessionId.value) == null }
        val client = RevengeAppGraph.sessionCache.getOrRestore(sessionId)
            .onFailure { e ->
                log.e("Failed to restore session for $sessionId", e)
                if (pushes.isNotEmpty()) {
                    pushDao.updatePushes(
                        pushes.map {
                            it.copy(
                                failureCount = it.failureCount + 1,
                                lastFailure = "Session failed to restore: $e",
                                resolvedTimestamp = if (isUnknownSession.await()) System.currentTimeMillis() else null,
                            )
                        }
                    )
                }
            }.getOrNull() ?: run {
                return if (isUnknownSession.await()) {
                    log.e("Unknown session $sessionId, fail permanently")
                    PushResolutionOutcome.PermanentFailure
                } else {
                    PushResolutionOutcome.TransientFailure
                }
            }

        var didNotify = false

        if (pushes.isNotEmpty()) {
            val result = client.notificationService.getNotifications(
                mapOf(roomId to pushes.map { EventId(it.eventId) })
            )
            if (result.isFailure) {
                val e = result.exceptionOrNull()
                log.e("Failed to get notifications for $sessionId/$roomId", e)
                pushDao.updatePushes(
                    pushes.map {
                        it.copy(
                            failureCount = it.failureCount + 1,
                            lastFailure = e.toString(),
                        )
                    }
                )
                return PushResolutionOutcome.TransientFailure
            }
            val results = result.getOrThrow()
            log.d { "Got ${results.size}/${pushes.size} notifications results for $sessionId/$roomId" }
            pushes.forEach { push ->
                val eventResult = results[EventId(push.eventId)]
                val data = eventResult?.getOrNull() ?: run {
                    val typedException = eventResult?.exceptionOrNull() as? NotificationResolverException
                    if (typedException != null && typedException !is NotificationResolverException.UnknownError) {
                        log.d("Got $typedException result for $sessionId/${push.roomId}/${push.eventId}")
                    } else {
                        log.w(
                            "Got no notification result for $sessionId/${push.roomId}/${push.eventId}",
                            eventResult?.exceptionOrNull()
                        )
                    }
                    val countsAsResolved = when (typedException) {
                        null -> false
                        NotificationResolverException.EventFilteredOut,
                        NotificationResolverException.EventNotFound,
                            // TODO dismiss existing notif if exists for redactions?
                        NotificationResolverException.EventRedacted -> true
                        is NotificationResolverException.UnknownError -> false
                    }
                    pushDao.updatePushes(
                        listOf(
                            push.copy(
                                failureCount = push.failureCount + 1,
                                lastFailure = eventResult?.exceptionOrNull()?.toString() ?: "SDK ignored the event",
                                resolvedTimestamp = if (countsAsResolved) System.currentTimeMillis() else null,
                            )
                        )
                    )
                    return@forEach
                }
                log.d("Got notification result for $sessionId/${push.roomId}/${push.eventId}")
                didNotify = true
                platformNotifyMessage(
                    id = NotificationId.Event(
                        sessionId = data.sessionId,
                        roomId = data.roomId,
                        eventId = data.eventId,
                    ),
                    data = data,
                )
                pushDao.markPushResolved(
                    sessionId = data.sessionId.value,
                    roomId = data.roomId.value,
                    eventId = data.eventId.value,
                )
                // TODO clean up DB for pushes older than certain timestamp
            }
        }

        // Check some heuristics to dismiss notifications that were fully read
        if (didNotify || platformActiveNotificationRooms().any {
            it.sessionId == sessionId.value && it.roomId == roomId.value
        }) {
            // Check current read receipts we already have synced. On push, we can't rely on sync having run yet,
            // so we try a best-effort live request for the fully read marker as fallback.
            // Read receipts would be preferred over the fully read marker for that, but it's not possible to fetch
            // those without sync, and we don't want to kick a full sync on each push.
            var receipts: List<EventId> = emptyList()
            val dismissedFromCachedReceipt = client.getRoom(roomId)?.use { room ->
                receipts = listOf(ReceiptType.READ, ReceiptType.READ_PRIVATE).mapNotNull { receiptType ->
                    room.getOwnReadReceipt(receiptType)
                        .onFailure { log.w("Failed to load local read receipt for $sessionId/$roomId, $receiptType", it) }
                        .getOrNull()
                }.distinct()

                log.d { "Checking for auto-dismiss on $sessionId, $roomId, [${receipts.joinToString()}]" }
                if (receipts.isNotEmpty()) {
                    platformAutoDismissNotification(sessionId, roomId, receipts)
                } else {
                    false
                }
            } ?: false
            if (!dismissedFromCachedReceipt) {
                // Read marker fetched from server (bypassing SDK cache from sync)
                client.fetchRoomAccountData(roomId, FULLY_READ_ACCOUNT_DATA_TYPE)
                    .onFailure { log.w("Failed to fetch read marker for $sessionId/$roomId", it) }
                    .getOrNull()
                    ?.let {
                        json.decodeFromString<FullyReadContent>(it).eventId?.let(::EventId)
                    }?.let { readMarker ->
                        log.d { "Checking for auto-dismiss on $sessionId, $roomId, $readMarker via re-fetched read marker" }
                        if (readMarker !in receipts) {
                            platformAutoDismissNotification(sessionId, roomId, listOf(readMarker))
                        }
                    }
            }
        }

        return PushResolutionOutcome.Done
    }
}
