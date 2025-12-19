package chat.schildi.revenge.model.spaces

import androidx.compose.runtime.Immutable
import chat.schildi.preferences.RevengePrefs
import chat.schildi.preferences.ScPreferencesStore
import chat.schildi.preferences.ScPrefs
import chat.schildi.revenge.model.ScopedRoomSummary
import chat.schildi.revenge.model.spaces.SpaceAggregationDataSource.SpaceUnreadCounts
import dev.zacsweers.metro.Inject
import io.element.android.libraries.matrix.api.room.CurrentUserMembership
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

data class SpaceAggregationState(
    val enrichedSpaces: ImmutableList<SpaceListDataSource.AbstractSpaceHierarchyItem>? = null,
    val totalUnreadCounts: SpaceUnreadCounts? = null,
)

// TODO also use for merging spaces from multiple accounts together
@Inject
class SpaceAggregationDataSource(
    allSpacesHierarchical: Flow<List<SpaceListDataSource.AbstractSpaceHierarchyItem>>,
    allRooms: Flow<List<ScopedRoomSummary>>,
    scPreferencesStore: ScPreferencesStore = RevengePrefs,
) {

    val state = combine(
        allRooms.throttleLatest(300),
        allSpacesHierarchical.throttleLatest(300),
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
            )
        }
        return unread
    }

    private fun SpaceUnreadCounts.add(
        mentions: Long,
        notifications: Long,
        unread: Long,
        markedUnread: Boolean,
        isInvite: Boolean
    ): SpaceUnreadCounts = if (isInvite) {
        copy(
            notifiedMessages = this.notifiedMessages + 1,
            unreadMessages = this.unreadMessages + 1,
            notifiedChats = this.notifiedChats + 1,
            unreadChats = this.unreadChats + 1,
            inviteCount = this.inviteCount + 1,
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
        )
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
    )
}

// Emit immediately but delay too fast updates after that
fun <T> Flow<T>.throttleLatest(period: Long) = flow {
    conflate().collect {
        emit(it)
        delay(period)
    }
}
