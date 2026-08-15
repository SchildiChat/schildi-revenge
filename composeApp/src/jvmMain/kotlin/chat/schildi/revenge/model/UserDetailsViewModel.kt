package chat.schildi.revenge.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import chat.schildi.revenge.UiState
import chat.schildi.revenge.actions.UserIdSuggestion
import chat.schildi.revenge.actions.UserIdSuggestionsProvider
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.room.JoinedRoom
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class UserDetailsViewModel(
    val sessionId: SessionId,
    val userId: UserId,
    val roomId: RoomId?,
) : ViewModel(), UserIdSuggestionsProvider {
    private val loadStateHolder = LoadStateHolder(
        listOfNotNull(
            LoadCheckPoint.Client(sessionId),
            LoadCheckPoint.Room.takeIf { roomId != null },
            LoadCheckPoint.MemberProfile.takeIf { roomId != null },
            LoadCheckPoint.UserProfile,
        )
    )
    val loadState = loadStateHolder.state

    private val client = UiState.selectClient(sessionId, viewModelScope, loadStateHolder)

    val globalUserInfo = client.map {
        it?.getProfile(userId)
            .also { loadStateHolder.handleResult(LoadCheckPoint.UserProfile, it) }
            ?.getOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val room = if (roomId == null) flowOf(null) else client.map { client ->
        client ?: return@map null
        (client.getJoinedRoom(roomId) ?: client.getRoom(roomId)).also {
            loadStateHolder.set(LoadCheckPoint.Room, it.asCheckpointLoadedOrFailed())
        }
    }

    val roomMember = room.map { room ->
        room?.getUpdatedMember(userId)
            .also { loadStateHolder.handleResult(LoadCheckPoint.MemberProfile, it) }
            ?.getOrNull()
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val mutualRoomsProvider = MutualRoomsProvider(
        sessionId = sessionId,
        userId = flowOf(userId),
        scope = viewModelScope,
        client = client,
        loadStateHolder = loadStateHolder,
    )
    val mutualRooms = mutualRoomsProvider.mutualRooms
    val mutualRoomsPreview = mutualRoomsProvider.mutualRoomsPreview

    @OptIn(ExperimentalCoroutinesApi::class)
    private val identityChanges = room.flatMapLatest {
        (it as? JoinedRoom)?.identityStateChangesFlow ?: flowOf(null)
    }.map {
        it?.find { it.userId == userId }
    }

    val userIdentity = client.combine(identityChanges) { client, identityChange ->
        if (identityChange != null) {
            loadStateHolder.set(LoadCheckPoint.UserIdentity, CheckpointLoadState.LOADED)
            identityChange.identityState
        } else {
            client?.encryptionService?.getUserIdentity(userId)
                ?.also { loadStateHolder.handleResult(LoadCheckPoint.UserIdentity, it) }
                ?.getOrNull()
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    override val userIdInRoomSuggestions: Flow<List<UserIdSuggestion>> = combine(
        globalUserInfo,
        roomMember,
    ) { info, member ->
        listOf(
            UserIdSuggestion(userId, info?.displayName ?: member?.displayName, member?.membership)
        )
    }

    companion object {
        fun factory(
            sessionId: SessionId,
            userId: UserId,
            roomId: RoomId?,
        ) = viewModelFactory {
            initializer {
                UserDetailsViewModel(sessionId, userId, roomId)
            }
        }
    }
}
