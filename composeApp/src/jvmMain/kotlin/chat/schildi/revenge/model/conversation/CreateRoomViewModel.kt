package chat.schildi.revenge.model.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import chat.schildi.resources.toStringHolder
import chat.schildi.revenge.Destination
import chat.schildi.revenge.MessageFormatDefaults
import chat.schildi.revenge.TitleProvider
import chat.schildi.revenge.UiState
import chat.schildi.revenge.actions.ActionContext
import chat.schildi.revenge.actions.ActionResult
import chat.schildi.revenge.actions.AppMessage
import chat.schildi.revenge.actions.toActionResult
import co.touchlab.kermit.Logger
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.createroom.CreateRoomParameters
import io.element.android.libraries.matrix.api.createroom.RoomPreset
import io.element.android.libraries.matrix.api.room.history.RoomHistoryVisibility
import io.element.android.libraries.matrix.api.room.join.JoinRule
import io.element.android.libraries.matrix.api.roomdirectory.RoomVisibility
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import shire.res.generated.resources.Res
import shire.res.generated.resources.create_room
import shire.res.generated.resources.toast_room_creation_error
import shire.res.generated.resources.toast_room_creation_success
import kotlin.text.isNotEmpty

data class CreateRoomSettings(
    val sessionId: SessionId? = null,
    val params: CreateRoomParameters = CreateRoomParameters(
        name = null,
        isEncrypted = true,
        visibility = RoomVisibility.Private,
        preset = RoomPreset.PRIVATE_CHAT,
    ),
)

sealed interface CreateRoomState {
    data object Idle : CreateRoomState
    data object InProgress : CreateRoomState
    sealed interface Result : CreateRoomState
    //data class Success(val roomId: RoomId?) : Result
    data class Failure(val exception: Throwable?) : Result
}

@OptIn(ExperimentalCoroutinesApi::class)
class CreateRoomViewModel(
    initialSessionId: SessionId? = null,
): ViewModel(), TitleProvider {

    private val log = Logger.withTag("CreateRoom")

    private val _settings = MutableStateFlow(CreateRoomSettings(initialSessionId))
    val settings = _settings.asStateFlow()

    private val _state = MutableStateFlow<CreateRoomState>(CreateRoomState.Idle)
    val state = _state.asStateFlow()

    val renderedTopic = settings.mapLatest { settings ->
        val topic = settings.params.topic?.takeIf(String::isNotEmpty) ?: return@mapLatest null
        MessageFormatDefaults.parser.parsePlaintext(
            topic,
            MessageFormatDefaults.parseStyle,
            allowRoomMention = false,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val availableSessionIds = combine(
        UiState.matrixClients,
        UiState.sessionIdComparator,
    ) { clients, comparator ->
        clients.keys.sortedWith(comparator).toPersistentList().also { sessionIds ->
            if (sessionIds.isNotEmpty() && settings.value.sessionId == null) {
                _settings.update {
                    it.copy(sessionId = it.sessionId ?: sessionIds.first())
                }
            }
        }
    }

    private val clientFlow = settings.map { it.sessionId }.distinctUntilChanged().flatMapLatest { sessionId ->
        sessionId ?: return@flatMapLatest flowOf(null)
        UiState.selectClient(sessionId, viewModelScope)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val availableRoomVersions = clientFlow.map { client ->
        client?.homeserverCapabilities()?.roomVersions()
            ?.onFailure { log.e("Failed to fetch available room versions", it) }
            ?.getOrNull()
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    override val windowTitle = flowOf(windowTitle())

    override fun verifyDestination(destination: Destination) = destination is Destination.CreateRoom

    suspend fun setSessionId(sessionId: SessionId): ActionResult {
        _settings.update {
            it.copy(
                sessionId = sessionId,
            )
        }
        return ActionResult.Success()
    }

    suspend fun setRoomName(name: String) = updateSettings {
        it.copy(name = name.takeIf(String::isNotBlank)?.trim())
    }

    suspend fun setRoomTopic(topic: String) = updateSettings {
        it.copy(topic = topic.takeIf(String::isNotBlank)?.trim())
    }

    suspend fun setPreset(preset: RoomPreset) = updateSettings {
        it.copy(
            preset = preset,
            isEncrypted = when (preset) {
                RoomPreset.PUBLIC_CHAT -> false
                RoomPreset.PRIVATE_CHAT,
                RoomPreset.TRUSTED_PRIVATE_CHAT -> true
            }
        )
    }.toActionResult()

    fun setEncrypted(isEncrypted: Boolean) = updateSettings {
        it.copy(isEncrypted = isEncrypted)
    }.toActionResult()

    suspend fun setVisibility(visibility: RoomVisibility) = updateSettings {
        it.copy(visibility = visibility)
    }.toActionResult()

    suspend fun setRoomJoinRule(joinRule: JoinRule?) = updateSettings {
        it.copy(joinRuleOverride = joinRule)
    }.toActionResult()

    suspend fun setRoomHistoryVisibility(historyVisibility: RoomHistoryVisibility?) = updateSettings {
        it.copy(historyVisibilityOverride = historyVisibility)
    }.toActionResult()

    suspend fun setRoomVersion(roomVersion: String?) = updateSettings {
        it.copy(roomVersion = roomVersion)
    }.toActionResult()

    private fun updateSettings(mapParameters: (CreateRoomParameters) -> CreateRoomParameters): Result<Unit> {
        _settings.update {
            it.copy(
                params = mapParameters(it.params)
            )
        }
        return Result.success(Unit)
    }

    fun createRoom(context: ActionContext) {
        val parameters = settings.value
        val sessionId = parameters.sessionId ?: run {
            log.e("Cannot create room without session ID")
            return
        }
        val client = clientFlow.value?.takeIf { it.sessionId == sessionId } ?: run {
            log.e("Cannot create room without client for $sessionId")
            return
        }
        var wasCreating = false
        _state.update {
            wasCreating = it is CreateRoomState.InProgress
            CreateRoomState.InProgress
        }
        if (wasCreating) {
            log.w("Ignore create room request while already in progress")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val result = client.createRoom(parameters.params)
            _state.value = if (result.isSuccess) {
                val roomId = result.getOrNull()
                if (roomId == null || context.destinationStateHolder == null) {
                    context.publishMessage(
                        AppMessage(
                            message = Res.string.toast_room_creation_success.toStringHolder(),
                        )
                    )
                } else {
                    context.destinationStateHolder?.navigate(Destination.Conversation(sessionId, roomId))
                }
                _settings.value = CreateRoomSettings(sessionId)
                CreateRoomState.Idle
            } else {
                val exception = result.exceptionOrNull()
                context.publishMessage(
                    AppMessage(
                        message = exception?.message?.toStringHolder()
                            ?: Res.string.toast_room_creation_error.toStringHolder(),
                        isError = true,
                    )
                )
                CreateRoomState.Failure(exception)
            }
        }
    }

    companion object {
        fun factory(initialSessionId: SessionId?) = viewModelFactory {
            initializer {
                CreateRoomViewModel(initialSessionId)
            }
        }

        fun windowTitle() = Res.string.create_room.toStringHolder()
    }
}
