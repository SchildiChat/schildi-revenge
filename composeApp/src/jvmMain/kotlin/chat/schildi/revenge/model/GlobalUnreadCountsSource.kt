package chat.schildi.revenge.model

import chat.schildi.revenge.UiState
import chat.schildi.revenge.compose.util.toStringHolder
import chat.schildi.revenge.model.spaces.SpaceAggregationDataSource
import chat.schildi.revenge.model.spaces.SpaceListDataSource
import chat.schildi.revenge.model.spaces.add
import io.element.android.libraries.matrix.api.core.SessionId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

object GlobalUnreadCountsSource {
    private val notifyingSessions = combine(
        UiState.currentValidSessionIds,
        UiState.mutedAccounts,
    ) { allSessions, muted ->
        allSessions?.mapNotNull {
            SessionId(it).takeIf { it !in muted }
        }.orEmpty()
    }
    private val unreadCountsDataSource = SpaceAggregationDataSource(
        allSpacesHierarchical = notifyingSessions.map { sessions ->
            sessions.map { sessionId ->
                SpaceListDataSource.SessionIdPseudoSpaceItem(
                    sessionId = sessionId,
                    avatarUrl = null,
                    enabled = true,
                    name = sessionId.value.toStringHolder(),
                    unreadCounts = null,
                )
            }
        },
        allRooms = RevengeRoomListDataSource.allRooms
    )

    val globalUnreadCounts = unreadCountsDataSource.state.map { state ->
        var unreadCounts = SpaceAggregationDataSource.SpaceUnreadCounts()
        state.enrichedSpaces?.forEach {
            if (it is SpaceListDataSource.SessionIdPseudoSpaceItem) {
                unreadCounts = unreadCounts.add(it.unreadCounts)
            }
        }
        unreadCounts
    }.flowOn(Dispatchers.IO)
}
