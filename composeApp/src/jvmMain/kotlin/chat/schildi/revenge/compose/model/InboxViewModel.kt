package chat.schildi.revenge.compose.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chat.schildi.revenge.CombinedSessions
import chat.schildi.revenge.UiState
import chat.schildi.revenge.flatMerge
import chat.schildi.revenge.util.mergeLists
import co.touchlab.kermit.Logger
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.roomlist.RoomListFilter
import io.element.android.libraries.matrix.api.roomlist.RoomSummary
import io.element.android.libraries.matrix.api.roomlist.ScSdkInboxSettings
import io.element.android.libraries.matrix.api.roomlist.ScSdkRoomSortOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ScopedRoomSummary(
    val sessionId: SessionId,
    val summary: RoomSummary,
)

class InboxViewModel(
    combinedSessions: CombinedSessions = UiState.combinedSessions,
) : ViewModel() {
    private val log = Logger.withTag("Inbox")

    init {
        log.d { "Init" }
    }

    // TODO settings
    val settings = ScSdkInboxSettings(
        ScSdkRoomSortOrder(
            byUnread = true,
            pinFavourites = false,
            buryLowPriority = true,
            clientSideUnreadCounts = true,
            withSilentUnread = true,
        )
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val allRooms = combinedSessions.flatMerge(
        map = { input ->
            input.client.roomListService.allRooms.summaries.map {
                it.map {
                    ScopedRoomSummary(input.client.sessionId, it)
                }
            }
        },
        onUpdatedInput = {
            it.forEach {
                log.v("Init for ${it.client.sessionId}")
                it.client.roomListService.allRooms.updateSettings(settings)
                it.client.roomListService.allRooms.updateFilter(RoomListFilter.All(emptyList()))
                it.client.roomListService.allRooms.loadMore()
            }
        },
        merge = {
            log.v("Merging room lists [${it.joinToString { it.size.toString() }}]")
            mergeLists(
                *it,
                key = { it },
                comparator = settings.sortOrder.toComparator { it.summary },
            )
        }
    ).stateIn(viewModelScope, SharingStarted.Lazily, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val allStates = combinedSessions.flatMerge(
        map = {
            combine(
                it.client.userProfile,
                it.client.roomListService.state,
            ) { a, b ->
                Pair(a, b)
            }
        },
        merge = {
            it.toList()
        }
    ).stateIn(viewModelScope, SharingStarted.Lazily, null)
}
