package chat.schildi.revenge.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import chat.schildi.revenge.Destination
import chat.schildi.revenge.MessageFormatDefaults
import chat.schildi.revenge.TitleProvider
import chat.schildi.revenge.UiState
import chat.schildi.revenge.actions.RoomContextSuggestionsProvider
import chat.schildi.revenge.actions.UserIdSuggestion
import chat.schildi.revenge.actions.UserIdSuggestionsProvider
import chat.schildi.revenge.compose.util.ComposableStringHolder
import chat.schildi.revenge.model.conversation.ConversationViewModel
import chat.schildi.revenge.util.flowClosable
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.room.roomMembers
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentHashMap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlin.collections.map

class RoomDetailsViewModel(
    val sessionId: SessionId,
    val roomId: RoomId,
) : ViewModel(), TitleProvider, UserIdSuggestionsProvider {

    private val client = UiState.selectClient(sessionId, viewModelScope)

    private val joinedRoom = client.map {
        it?.getJoinedRoom(roomId)
    }
        .flowClosable()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val notJoinedRoom = combine(client, joinedRoom) { client, joined ->
        if (joined == null) {
            client?.getRoom(roomId)
        } else {
            // Unnecessary
            null
        }
    }.flowClosable()

    private val baseRoom = combine(
        joinedRoom,
        notJoinedRoom
    ) { joined, notJoined ->
        joined ?: notJoined
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val roomInfo = baseRoom.flatMapLatest {
        it?.roomInfoFlow ?: flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val topic = roomInfo.mapLatest { info ->
        val topic = info?.topic ?: return@mapLatest null
        MessageFormatDefaults.parser.parsePlaintext(
            topic,
            MessageFormatDefaults.parseStyle,
            allowRoomMention = false,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val roomMembersState = joinedRoom.flatMapLatest { joined ->
        joined?.membersStateFlow ?: flowOf()
    }

    val roomMembers = roomMembersState.map {
        it.roomMembers()?.toImmutableList() ?: persistentListOf()
    }.stateIn(viewModelScope, SharingStarted.Lazily, persistentListOf())

    val roomMembersById = roomMembers.map {
        it.associateBy { it.userId }.toPersistentHashMap()
    }.stateIn(viewModelScope, SharingStarted.Lazily, persistentHashMapOf())

    override val userIdInRoomSuggestions: Flow<List<UserIdSuggestion>> = roomMembers.map {
        it.map {
            UserIdSuggestion(it.userId, it.displayName)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val userProfile = client.flatMapLatest { it?.userProfile ?: flowOf(null) }

    val actionProvider = RoomActionProvider(
        sessionId = sessionId,
        roomId = roomId,
        isInvite = false,
        peekClient = { client.value },
        peekRoom = { baseRoom.value },
    )

    val roomContextSuggestionsProvider = RoomContextSuggestionsProvider(
        sessionId = sessionId,
        roomId = roomId,
        threadId = null,
        peekRoom = { baseRoom.value },
    )

    override val windowTitle: Flow<ComposableStringHolder?> = combine(
        roomInfo,
        userProfile,
        roomMembersById,
    ) { info, user, roomMembers ->
        ConversationViewModel.windowTitle(
            roomInfo = info,
            accountUserDisplayName = user?.displayName,
            roomUserDisplayName = roomMembers[sessionId]?.displayName,
            sessionId = sessionId,
        )
    }.filterNotNull()

    companion object {
        fun factory(
            sessionId: SessionId,
            roomId: RoomId,
        ) = viewModelFactory {
            initializer {
                RoomDetailsViewModel(sessionId, roomId)
            }
        }
    }

    override fun verifyDestination(destination: Destination): Boolean {
        return destination is Destination.RoomDetails && destination.sessionId == sessionId && destination.roomId == roomId
    }
}
