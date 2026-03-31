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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class UserDetailsViewModel(
    val sessionId: SessionId,
    val userId: UserId,
    val roomId: RoomId?,
) : ViewModel(), UserIdSuggestionsProvider {
    private val client = UiState.selectClient(sessionId, viewModelScope)

    val globalUserInfo = client.map {
        it?.getProfile(userId)?.getOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val room = if (roomId == null) flowOf(null) else client.map { client ->
        client?.getRoom(roomId)
    }

    val roomMember = room.map { room ->
        room?.getUpdatedMember(userId)?.getOrNull()
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    override val userIdInRoomSuggestions: Flow<List<UserIdSuggestion>> = combine(
        globalUserInfo,
        roomMember,
    ) { info, member ->
        listOf(
            UserIdSuggestion(userId, info?.displayName ?: member?.displayName)
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
