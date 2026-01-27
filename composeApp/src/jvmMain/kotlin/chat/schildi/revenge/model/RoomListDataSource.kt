package chat.schildi.revenge.model

import chat.schildi.preferences.RevengePrefs
import chat.schildi.preferences.ScPref
import chat.schildi.preferences.ScPreferencesStore
import chat.schildi.preferences.ScPrefs
import chat.schildi.preferences.safeLookup
import chat.schildi.revenge.CombinedSessions
import chat.schildi.revenge.UiState
import chat.schildi.revenge.flatMergeCombinedWith
import chat.schildi.revenge.model.conversation.messageMetadata
import chat.schildi.revenge.util.mergeLists
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.Inject
import io.element.android.libraries.matrix.api.roomlist.LatestEventValue
import io.element.android.libraries.matrix.api.roomlist.RoomListFilter
import io.element.android.libraries.matrix.api.roomlist.ScSdkInboxSettings
import io.element.android.libraries.matrix.api.roomlist.ScSdkRoomSortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

private fun buildScSdkInboxSettings(lookup: (ScPref<*>) -> Any?) = ScSdkInboxSettings(
    sortOrder = ScSdkRoomSortOrder(
        byUnread = ScPrefs.SORT_BY_UNREAD.safeLookup(lookup),
        pinFavourites = ScPrefs.PIN_FAVORITES.safeLookup(lookup),
        buryLowPriority = ScPrefs.BURY_LOW_PRIORITY.safeLookup(lookup),
        clientSideUnreadCounts = ScPrefs.CLIENT_GENERATED_UNREAD_COUNTS.safeLookup(lookup),
        withSilentUnread = ScPrefs.SORT_WITH_SILENT_UNREAD.safeLookup(lookup),
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

    /**
     * All rooms for the current account selection, merged together with appropriate sort order.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val allRooms = combinedSessions.flatMergeCombinedWith(
        map = { input, _ ->
            input.client.roomListService.allRooms.summaries.map {
                it.map {
                    val latestEventContent = when (val event = it.latestEvent) {
                        is LatestEventValue.Local -> event.content
                        is LatestEventValue.Remote -> event.content
                        LatestEventValue.None -> null
                    }
                    ScopedRoomSummary(
                        input.client.sessionId,
                        it,
                        latestEventContent?.messageMetadata())
                }
            }
        },
        onUpdatedInput = { it, settings ->
            it.forEach {
                it.client.roomListService.allRooms.updateSettings(settings)
                it.client.roomListService.allRooms.updateFilter(RoomListFilter.All(emptyList()))
                it.client.roomListService.allRooms.loadMore()
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
    ).flowOn(Dispatchers.IO)

}
