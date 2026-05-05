package chat.schildi.revenge.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import chat.schildi.matrixsdk.ROOM_ACCOUNT_DATA_PERSONAL_ROOM_NAME
import chat.schildi.matrixsdk.RoomNamePrivateContent
import chat.schildi.revenge.Destination
import chat.schildi.revenge.MessageFormatDefaults
import chat.schildi.revenge.TitleProvider
import chat.schildi.revenge.UiState
import chat.schildi.revenge.actions.RoomContextSuggestionsProvider
import chat.schildi.revenge.compose.util.ComposableStringHolder
import chat.schildi.revenge.model.conversation.ConversationViewModel
import chat.schildi.revenge.util.flowClosable
import co.touchlab.kermit.Logger
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
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
import kotlinx.serialization.json.Json

data class RoomSettingsPermissions(
    val canEditName: Boolean = false,
    val canEditTopic: Boolean = false,
    val canEditAvatar: Boolean = false,
)

class RoomDetailsViewModel(
    val sessionId: SessionId,
    val roomId: RoomId,
) : ViewModel(), TitleProvider {

    private val log = Logger.withTag("RoomDetails")

    private val loadStateHolder = LoadStateHolder(
        LoadCheckPoint.Client(sessionId),
        LoadCheckPoint.Room,
    )
    val loadState = loadStateHolder.state

    private val client = UiState.selectClient(sessionId, viewModelScope, loadStateHolder)

    private val joinedRoom = client.map {
        it ?: return@map null
        it.getJoinedRoom(roomId).also { room ->
            loadStateHolder.set(LoadCheckPoint.Room, room.asCheckpointLoadedOrFailed())
        }
    }
        .flowClosable()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val notJoinedRoom = combine(client, joinedRoom) { client, joined ->
        if (joined == null) {
            client ?: return@combine null
            client.getRoom(roomId).also {
                loadStateHolder.set(LoadCheckPoint.Room, it.asCheckpointLoadedOrFailed())
            }
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
        val topic = info?.topic?.takeIf(String::isNotEmpty) ?: return@mapLatest null
        MessageFormatDefaults.parser.parsePlaintext(
            topic,
            MessageFormatDefaults.parseStyle,
            allowRoomMention = false,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val userProfile = client.flatMapLatest { it?.userProfile ?: flowOf(null) }

    private val ownUserRole = joinedRoom.map { room ->
        room?.userRole(sessionId)
            ?.onFailure { log.e("Failed to get own user role", it) }
            ?.getOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val ownUser = joinedRoom.map { room ->
        room?.getUpdatedMember(sessionId)
            ?.onFailure { log.e("Failed to get own room user", it) }
            ?.getOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val powerLevels = joinedRoom.map { room ->
        room?.powerLevels()
            ?.onFailure { log.e("Failed to get room power levels", it) }
            ?.getOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val roomSettingsPermissions = combine(
        powerLevels,
        ownUserRole,
    ) { pls, ownRole ->
        pls ?: return@combine null
        ownRole ?: return@combine null
        RoomSettingsPermissions(
            canEditName = ownRole.powerLevel >= pls.roomName,
            canEditTopic = ownRole.powerLevel >= pls.roomTopic,
            canEditAvatar = ownRole.powerLevel >= pls.roomAvatar,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

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
        ownUser,
    ) { info, user, roomUser ->
        ConversationViewModel.windowTitle(
            roomInfo = info,
            accountUserDisplayName = user?.displayName,
            roomUserDisplayName = roomUser?.displayName,
            sessionId = sessionId,
        )
    }.filterNotNull()

    suspend fun setRoomName(name: String): Result<Unit> {
        val room = joinedRoom.value ?: return Result.failure(IllegalStateException("Room not joined"))
        return room.setName(name)
    }

    suspend fun setPrivateRoomName(name: String): Result<Unit> {
        val client = client.value ?: return Result.failure(IllegalStateException("Client not ready"))
        val content = if (name.isBlank()) {
            "{}"
        } else {
            Json.encodeToString(RoomNamePrivateContent(name))
        }
        return client.setRoomAccountData(
            roomId,
            ROOM_ACCOUNT_DATA_PERSONAL_ROOM_NAME,
            content,
        )
    }

    suspend fun setRoomTopic(topic: String): Result<Unit> {
        val room = joinedRoom.value ?: return Result.failure(IllegalStateException("Room not joined"))
        return room.setTopic(topic)
    }

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
