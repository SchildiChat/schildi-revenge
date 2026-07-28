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
import chat.schildi.revenge.actions.ActionResult
import chat.schildi.revenge.actions.RoomContextSuggestionsProvider
import chat.schildi.revenge.actions.toActionResult
import chat.schildi.resources.ComposableStringHolder
import chat.schildi.revenge.model.conversation.ConversationViewModel
import chat.schildi.revenge.util.flowClosable
import chat.schildi.revenge.util.tryOrNull
import co.touchlab.kermit.Logger
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.room.StateEventType
import io.element.android.libraries.matrix.api.room.history.RoomHistoryVisibility
import io.element.android.libraries.matrix.api.room.join.AllowRule
import io.element.android.libraries.matrix.api.room.join.JoinRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class RoomSettingsPermissions(
    val canEditName: Boolean = false,
    val canEditTopic: Boolean = false,
    val canEditAvatar: Boolean = false,
    val canSetRoomHistoryVisibility: Boolean = false,
    val canSetJoinRule: Boolean = false,
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

    private val roomPermissions = joinedRoom.map { room ->
        room?.roomPermissions()
            ?.onFailure { log.e("Failed to get room permissions", it) }
            ?.getOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val roomSettingsPermissions = combine(
        powerLevels,
        ownUserRole,
        roomPermissions,
    ) { pls, ownRole, permissions ->
        pls ?: return@combine null
        ownRole ?: return@combine null
        RoomSettingsPermissions(
            canEditName = ownRole.powerLevel >= pls.roomName,
            canEditTopic = ownRole.powerLevel >= pls.roomTopic,
            canEditAvatar = ownRole.powerLevel >= pls.roomAvatar,
            canSetRoomHistoryVisibility = permissions?.canOwnUserSendState(StateEventType.Custom("m.room.history_visibility")) == true,
            canSetJoinRule = permissions?.canOwnUserSendState(StateEventType.Custom("m.room.join_rules")) == true,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val joinRuleRoomNames = combine(
        client,
        roomInfo.map { it?.joinRule }.distinctUntilChanged(),
    ) { client, joinRule ->
        client ?: return@combine null
        val roomIds = when (joinRule) {
            is JoinRule.Restricted -> joinRule.rules.mapNotNull { (it as? AllowRule.RoomMembership)?.roomId }
            is JoinRule.KnockRestricted -> joinRule.rules.mapNotNull { (it as? AllowRule.RoomMembership)?.roomId }
            else -> emptyList()
        }
        roomIds.associateWith { roomId ->
            client.getRoom(roomId)?.use { room ->
                room.info().let {
                    it.name ?: it.canonicalAlias?.value ?: it.aliases.firstOrNull()?.value
                }
            } ?: roomId.value
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val predecessorRoom = baseRoom.map { room ->
        room?: return@map null
        room.predecessorRoom()
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val roomType = baseRoom.map { room ->
        room ?: return@map null
        room.getRawState("m.room.create", "")
            .onFailure { log.e("Failed to get room creation event", it) }
            .getOrNull()
            ?.let { rawEvent ->
                tryOrNull {
                    Json.parseToJsonElement(rawEvent)
                        .jsonObject["content"]
                        ?.jsonObject
                        ?.get("type")
                        ?.jsonPrimitive
                        ?.contentOrNull
                }
            }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

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

    suspend fun setRoomHistoryVisibility(visibility: RoomHistoryVisibility): ActionResult {
        val room = joinedRoom.value ?: return ActionResult.Failure("Room not joined")
        return room.updateHistoryVisibility(visibility).toActionResult()
    }

    suspend fun setJoinRule(joinRule: JoinRule): ActionResult {
        val room = joinedRoom.value ?: return ActionResult.Failure("Room not joined")
        return room.updateJoinRule(joinRule).toActionResult()
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
