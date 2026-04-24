package chat.schildi.revenge.model.userlist

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import chat.schildi.revenge.Destination
import chat.schildi.revenge.TitleProvider
import chat.schildi.revenge.UiState
import chat.schildi.revenge.actions.RoomContextSuggestionsProvider
import chat.schildi.revenge.compose.util.ComposableStringHolder
import chat.schildi.revenge.compose.util.toStringHolder
import chat.schildi.revenge.model.LoadCheckPoint
import chat.schildi.revenge.model.LoadStateHolder
import chat.schildi.revenge.model.RoomActionProvider
import chat.schildi.revenge.model.asCheckpointLoadState
import chat.schildi.revenge.model.asCheckpointLoadedOrFailed
import chat.schildi.revenge.util.flowClosable
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.room.RoomInfo
import io.element.android.libraries.matrix.api.room.RoomMember
import io.element.android.libraries.matrix.api.room.RoomMembersState
import io.element.android.libraries.matrix.api.room.RoomMembershipState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.room_members_title_in_room_loaded
import shire.composeapp.generated.resources.room_members_title_loading

data class RoomMemberItem(
    val value: RoomMember
) : UserListItem {
    override val userId: UserId
        get() = value.userId
    override val displayName: String?
        get() = value.displayName
    override val avatarUrl: String?
        get() = value.avatarUrl
    override val membership: RoomMembershipState
        get() = value.membership
}

class RoomMemberListViewModel(
    override val sessionId: SessionId,
    override val roomId: RoomId,
) : AbstractUserListViewModel<RoomMemberItem>(), TitleProvider {

    private val loadStateHolder = LoadStateHolder(
        LoadCheckPoint.Client(sessionId),
        LoadCheckPoint.Room,
        LoadCheckPoint.RoomMembers,
    )
    val loadState = loadStateHolder.state

    private val clientFlow = UiState.selectClient(sessionId, viewModelScope, loadStateHolder)

    private val roomFlow = clientFlow.map { client ->
        client ?: return@map null
        (client.getJoinedRoom(roomId) ?: client.getRoom(roomId)).also {
            loadStateHolder.set(LoadCheckPoint.Room, it.asCheckpointLoadedOrFailed())
        }
    }.flowClosable().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val roomInfo: Flow<RoomInfo?> = roomFlow.flatMapLatest {
        it?.roomInfoFlow ?: flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val roomMembersState = roomFlow.flatMapLatest { room ->
        room?.membersStateFlow ?: flowOf()
    }.onEach {
        loadStateHolder.set(LoadCheckPoint.RoomMembers, it.asCheckpointLoadState())
    }

    override val allEntries: StateFlow<ImmutableList<RoomMemberItem>?> = roomMembersState.map { state ->
        val members = when (state) {
            is RoomMembersState.Ready -> state.roomMembers
            is RoomMembersState.Pending -> state.prevRoomMembers?.takeIf { it.isNotEmpty() }
            is RoomMembersState.Error -> state.prevRoomMembers
            is RoomMembersState.Unknown -> null
        } ?: return@map null
        members
            .filter {
                it.membership == RoomMembershipState.JOIN ||
                        it.membership == RoomMembershipState.KNOCK ||
                        it.membership == RoomMembershipState.INVITE
            }.sortedWith(
                compareBy(
                    {
                        when (it.membership) {
                            RoomMembershipState.JOIN -> 0
                            RoomMembershipState.KNOCK -> 1
                            RoomMembershipState.INVITE -> 2
                            RoomMembershipState.LEAVE -> 4
                            RoomMembershipState.BAN -> 5
                        }
                    },
                    { -it.powerLevel },
                    { it.displayName == null },
                    { it.displayName?.lowercase() },
                    { it.userId.value.lowercase() }
                )
            ).map(::RoomMemberItem).toImmutableList()
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        null,
    )

    val entries = filteredEntriesFlow().stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        null,
    )

    override val userIdInRoomSuggestions = userIdInRoomSuggestionsFlow()

    val actionProvider = RoomActionProvider(
        sessionId = sessionId,
        roomId = roomId,
        isInvite = false,
        peekClient = { clientFlow.value },
        peekRoom = { roomFlow.value },
    )

    val roomContextSuggestionsProvider = RoomContextSuggestionsProvider(
        sessionId = sessionId,
        roomId = roomId,
        peekRoom = { roomFlow.value },
    )

    override val windowTitle: Flow<ComposableStringHolder?> = combine(
        roomInfo,
        allEntries,
    ) { info, allMembers ->
        windowTitle(roomId, info, allMembers?.size)
    }

    init {
        roomFlow.onEach { room ->
            room?.updateMembers()
            room?.subscribeToSync()
        }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
    }

    override fun verifyDestination(destination: Destination): Boolean {
        return destination is Destination.RoomMembers && destination.sessionId == sessionId && destination.roomId == roomId
    }

    companion object {
        fun factory(
            sessionId: SessionId,
            roomId: RoomId,
        ) = viewModelFactory {
            initializer {
                RoomMemberListViewModel(sessionId, roomId)
            }
        }

        fun windowTitle(
            roomId: RoomId,
            roomInfo: RoomInfo?,
            memberCount: Int?,
        ): ComposableStringHolder? {
            return if (roomInfo == null || memberCount == null) {
                Res.string.room_members_title_loading.toStringHolder()
            } else {
                Res.plurals.room_members_title_in_room_loaded.toStringHolder(
                    memberCount,
                    memberCount.toString().toStringHolder(),
                    (roomInfo.name ?: roomId.value).toStringHolder(),
                )
            }
        }
    }
}
