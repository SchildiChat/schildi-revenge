package chat.schildi.revenge.model.spaces

import androidx.compose.runtime.Immutable
import chat.schildi.preferences.RevengePrefs
import chat.schildi.preferences.ScPreferencesStore
import chat.schildi.preferences.ScPrefs
import chat.schildi.revenge.UiState
import chat.schildi.revenge.compose.util.throttleLatest
import chat.schildi.revenge.model.ScopedRoomSummary
import chat.schildi.revenge.model.spaces.SpaceAggregationDataSource.SpaceUnreadCounts
import dev.zacsweers.metro.Inject
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.room.CurrentUserMembership
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn

data class SpaceAggregationState(
    val enrichedSpaces: ImmutableList<SpaceListDataSource.AbstractSpaceHierarchyItem>? = null,
    val totalUnreadCounts: SpaceUnreadCounts? = null,
)

@Inject
class SpaceAggregationDataSource(
    allSpacesHierarchical: Flow<List<SpaceListDataSource.AbstractSpaceHierarchyItem>>,
    allRooms: Flow<List<ScopedRoomSummary>>,
    sessionIdComparatorFlow: Flow<Comparator<SessionId>> = UiState.sessionIdComparator,
    scPreferencesStore: ScPreferencesStore = RevengePrefs,
) {

    val allSpacesHierarchicalMerged = combine(
        allSpacesHierarchical,
        sessionIdComparatorFlow,
    ) { it, comparator ->
        val pseudoSpaces = it.mapNotNull { it as? SpaceListDataSource.PseudoSpaceItem }
        val realSpaces = it.mapNotNull { it as? SpaceListDataSource.SpaceHierarchyItem }
        val mergedSpaces = realSpaces.mergeSpaceSessionDuplicates(comparator)
        (pseudoSpaces + mergedSpaces).toImmutableList()
    }.flowOn(Dispatchers.IO)

    val state = combine(
        allRooms.throttleLatest(300),
        allSpacesHierarchicalMerged.throttleLatest(300),
        scPreferencesStore.settingFlow(ScPrefs.CLIENT_GENERATED_UNREAD_COUNTS),
    ) { allRoomsValue, rootSpaces, useClientGeneratedCounts ->
        val totalUnreadCounts = getAggregatedUnreadCounts(allRoomsValue, useClientGeneratedCounts)
        val newEnrichedSpaces = rootSpaces.map { space ->
            space.enrich {
                getUnreadCountsForSpace(it, allRoomsValue, useClientGeneratedCounts)
            }
        }.toImmutableList()
        SpaceAggregationState(newEnrichedSpaces, totalUnreadCounts)
    }.flowOn(Dispatchers.IO)

    private fun getUnreadCountsForSpace(
        space: SpaceListDataSource.AbstractSpaceHierarchyItem,
        allRooms: List<ScopedRoomSummary>,
        useClientGeneratedUnreadCounts: Boolean,
    ) = getAggregatedUnreadCounts(
        space.applyFilter(allRooms),
        useClientGeneratedUnreadCounts,
    )

    private fun getAggregatedUnreadCounts(
        rooms: List<ScopedRoomSummary>,
        useClientGeneratedUnreadCounts: Boolean,
    ): SpaceUnreadCounts {
        var unread = SpaceUnreadCounts()
        for (room in rooms) {
            val info = room.summary.info
            unread = unread.add(
                if (useClientGeneratedUnreadCounts) info.numUnreadMentions else info.highlightCount,
                if (useClientGeneratedUnreadCounts) info.numUnreadNotifications else info.notificationCount,
                if (useClientGeneratedUnreadCounts) info.numUnreadMessages else info.unreadCount,
                info.isMarkedUnread,
                info.currentUserMembership == CurrentUserMembership.INVITED,
                false,
            )
        }
        return unread
    }

    @Immutable
    data class SpaceUnreadCounts(
        val mentionedMessages: Long = 0,
        val notifiedMessages: Long = 0,
        val unreadMessages: Long = 0,
        val mentionedChats: Long = 0,
        val notifiedChats: Long = 0,
        val unreadChats: Long = 0,
        val markedUnreadChats: Long = 0,
        val inviteCount: Long = 0,
        val isEmptySpace: Boolean = true,
    )
}

private fun SpaceUnreadCounts.add(
    mentions: Long,
    notifications: Long,
    unread: Long,
    markedUnread: Boolean,
    isInvite: Boolean,
    isEmpty: Boolean,
): SpaceUnreadCounts = if (isInvite) {
    copy(
        notifiedMessages = this.notifiedMessages + 1,
        unreadMessages = this.unreadMessages + 1,
        notifiedChats = this.notifiedChats + 1,
        unreadChats = this.unreadChats + 1,
        inviteCount = this.inviteCount + 1,
        isEmptySpace = false,
    )
} else {
    SpaceUnreadCounts(
        this.mentionedMessages + mentions,
        this.notifiedMessages + notifications,
        this.unreadMessages + unread,
        this.mentionedChats + if (mentions > 0) 1 else 0,
        this.notifiedChats + if (notifications > 0) 1 else 0,
        this.unreadChats + if (unread > 0) 1 else 0,
        this.markedUnreadChats + if (markedUnread) 1 else 0,
        this.inviteCount,
        this.isEmptySpace && isEmpty
    )
}

private fun SpaceUnreadCounts.add(
    other: SpaceUnreadCounts?,
): SpaceUnreadCounts = if (other == null) {
    this
} else {
    SpaceUnreadCounts(
        this.mentionedMessages + other.mentionedMessages,
        this.notifiedMessages + other.notifiedMessages,
        this.unreadMessages + other.unreadMessages,
        this.mentionedChats + other.mentionedChats,
        this.notifiedChats + other.notifiedChats,
        this.unreadChats + other.unreadChats,
        this.markedUnreadChats + other.markedUnreadChats,
        this.inviteCount + other.inviteCount,
        this.isEmptySpace && other.isEmptySpace,
    )
}

private fun List<SpaceListDataSource.SpaceHierarchyItem>.mergeSpaceSessionDuplicates(
    sessionIdComparator: Comparator<SessionId>,
): List<SpaceListDataSource.SpaceHierarchyItem> {
    return groupBy {
        it.room.summary.roomId
    }.map { (_, duplicates) ->
        if (duplicates.size == 1) {
            duplicates.first()
        } else {
            val prioritized = duplicates.sortedWith { left, right ->
                sessionIdComparator.compare(left.room.sessionId, right.room.sessionId)
            }
            val mainSpace = prioritized.first()
            val mergedRooms = prioritized.subList(1, prioritized.size)
            var unreadCount = SpaceUnreadCounts()
            prioritized.forEach { other ->
                unreadCount = unreadCount.add(other.unreadCounts)
            }
            // TODO is this a good heuristic for sort order?
            val sortOrder = duplicates.mapNotNull { it.order }.takeIf { it.isNotEmpty() }?.min()
            SpaceListDataSource.SpaceHierarchyItem(
                room = mainSpace.room,
                order = sortOrder,
                spaces = prioritized.flatMap { it.spaces }.mergeSpaceSessionDuplicates(sessionIdComparator).toImmutableList(),
                directChildren = prioritized.flatMap { it.directChildren }.toImmutableSet(),
                flattenedRooms = prioritized.flatMap { it.flattenedRooms }.toImmutableSet(),
                unreadCounts = unreadCount,
                mergedRooms = mergedRooms.map { it.room }.toImmutableList(),
            )
        }
    }.sortedWith(SpaceComparator(sessionIdComparator))
}
