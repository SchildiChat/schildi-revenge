package chat.schildi.revenge.model

import chat.schildi.revenge.preferences.RevengePrefs
import chat.schildi.lib.preferences.ScPref
import chat.schildi.lib.preferences.ScPreferencesStore
import chat.schildi.lib.preferences.ScPrefs
import chat.schildi.lib.preferences.safeLookup
import chat.schildi.revenge.CombinedSessions
import chat.schildi.revenge.MessageFormatDefaults
import chat.schildi.revenge.UiState
import chat.schildi.revenge.flatMerge
import chat.schildi.revenge.flatMergeCombinedWith
import chat.schildi.revenge.model.conversation.messageMetadata
import chat.schildi.revenge.util.mergeLists
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.Inject
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.roomlist.LatestEventValue
import io.element.android.libraries.matrix.api.roomlist.RoomListFilter
import io.element.android.libraries.matrix.api.roomlist.ScSdkInboxSettings
import io.element.android.libraries.matrix.api.roomlist.ScSdkRoomSortOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlin.time.Duration.Companion.milliseconds

private fun buildScSdkInboxSettings(lookup: (ScPref<*>) -> Any?) = ScSdkInboxSettings(
    sortOrder = ScSdkRoomSortOrder(
        byUnread = ScPrefs.SORT_BY_UNREAD.safeLookup(lookup),
        pinFavourites = ScPrefs.PIN_FAVORITES.safeLookup(lookup),
        buryLowPriority = ScPrefs.BURY_LOW_PRIORITY.safeLookup(lookup),
        clientSideUnreadCounts = ScPrefs.CLIENT_GENERATED_UNREAD_COUNTS.safeLookup(lookup),
        withSilentUnread = ScPrefs.SORT_WITH_SILENT_UNREAD.safeLookup(lookup),
        withInaccurateSilentUnread = ScPrefs.INDICATE_UNREAD_COUNT_UNDERESTIMATES.safeLookup(lookup),
    )
)

val RevengeRoomListDataSource = RoomListDataSource()

@Inject
class RoomListDataSource(
    private val combinedSessions: CombinedSessions = UiState.combinedSessions,
    private val scPreferencesStore: ScPreferencesStore = RevengePrefs,
) {

    private val log = Logger.withTag("RoomListDataSource")

    val sdkSettings = scPreferencesStore.combinedSettingFlow { lookup ->
        buildScSdkInboxSettings(lookup)
    }

    @OptIn(FlowPreview::class)
    fun observeInvalidationSignals(scope: CoroutineScope) {
        // Room summaries need manual invalidation for notification settings changes
        combinedSessions.flatMerge(
            map = { session ->
                session.client.notificationSettingsService.notificationSettingsChangeFlow
                    .debounce(500.milliseconds)
                    .onEach { session.client.roomListService.allRooms.rebuildSummaries() }
            },
            merge = { },
            onEmpty = { },
        )
            .onStart {
                // Rebuild once to account for lost updates, which is a real problem for some reason,
                // initial room lists are emitted without settings?
                combinedSessions.value.forEach {
                    try {
                        it.client.roomListService.allRooms.rebuildSummaries()
                    } catch (e: Exception) {
                        log.e("Failed to rebuild summaries for ${it.client.sessionId}", e)
                    }
                }
            }
            .flowOn(Dispatchers.Default)
            .launchIn(scope)
    }

    /**
     * All rooms for the current account selection, merged together with appropriate sort order.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val allRooms = combinedSessions.flatMergeCombinedWith(
        map = { input, _ ->
            // New list input often means that only few room summaries actually changed.
            // If we cache previous built scoped summaries, we can decrease memory allocations significantly.
            // Can e.g. observe memory consumption via `visualvm` while getting spammed in a single room
            var cachedSummaries = emptyMap<RoomId, ScopedRoomSummary>()
            input.client.roomListService.allRooms.summaries.map { summaries ->
                summaries.map { summary ->
                    val previous = cachedSummaries[summary.roomId]
                    if (previous?.summary === summary) {
                        previous
                    } else {
                        val latestEventMessageMetadata = if (previous?.summary?.latestEvent == summary.latestEvent) {
                            previous.latestEventMessageMetadata
                        } else {
                            when (val event = summary.latestEvent) {
                                is LatestEventValue.Local -> event.content
                                is LatestEventValue.Remote -> event.content
                                is LatestEventValue.RoomInvite,
                                LatestEventValue.None -> null
                            }?.messageMetadata(
                                style = MessageFormatDefaults.parseStyleForStrippedFormatting,
                            )
                        }
                        ScopedRoomSummary(
                            input.client.sessionId,
                            summary,
                            latestEventMessageMetadata,
                        )
                    }
                }.also { scopedSummaries ->
                    cachedSummaries = scopedSummaries.associateBy { it.summary.roomId }
                }
            }.distinctUntilChanged()
        },
        onUpdatedInput = { it, settings ->
            it.forEach {
                try {
                    it.client.roomListService.allRooms.updateSettings(RoomListFilter.All(emptyList()), settings)
                } catch (e: Exception) {
                    log.e("Failed to update settings for ${it.client.sessionId}", e)
                }
            }
        },
        merge = { it, settings ->
            mergeLists(
                // In theory the SDK should have already sorted them for us... but it's somewhat bad at it sometimes?
                *it.map { it.sortedWith(settings.sortOrder.toComparator { it.summary }) }.toTypedArray(),
                key = { it },
                comparator = settings.sortOrder.toComparator { it.summary },
            )
        },
        onEmpty = { emptyList() },
        other = sdkSettings,
    )
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)

}
