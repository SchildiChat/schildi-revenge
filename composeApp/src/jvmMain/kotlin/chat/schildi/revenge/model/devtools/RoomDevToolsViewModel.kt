package chat.schildi.revenge.model.devtools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import chat.schildi.revenge.Destination
import chat.schildi.revenge.PrettyJson
import chat.schildi.revenge.TitleProvider
import chat.schildi.revenge.UiState
import chat.schildi.revenge.actions.RoomContextSuggestionsProvider
import chat.schildi.revenge.compose.search.SearchProvider
import chat.schildi.revenge.compose.util.StringResourceHolder
import chat.schildi.revenge.compose.util.toStringHolder
import chat.schildi.revenge.model.LoadCheckPoint
import chat.schildi.revenge.model.LoadStateHolder
import chat.schildi.revenge.model.RoomActionProvider
import chat.schildi.revenge.model.asCheckpointLoadedOrFailed
import chat.schildi.revenge.toPrettyJson
import chat.schildi.revenge.util.flowClosable
import co.touchlab.kermit.Logger
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.room.BaseRoom
import io.element.android.libraries.matrix.api.room.StateEventType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.dual_title_format
import shire.composeapp.generated.resources.hint_room_account_data
import shire.composeapp.generated.resources.hint_room_state
import shire.composeapp.generated.resources.room_dev_tools_title
import kotlin.collections.map

@OptIn(ExperimentalCoroutinesApi::class)
class RoomDevToolsViewModel(
    private val sessionId: SessionId,
    private val roomId: RoomId,
) : ViewModel(), TitleProvider, SearchProvider {
    private val log = Logger.withTag("RoomDevTools")

    private val loadStateHolder = LoadStateHolder(
        LoadCheckPoint.Client(sessionId),
        LoadCheckPoint.RoomAccountData,
        LoadCheckPoint.Room,
        LoadCheckPoint.RoomState,
    )
    val loadState = loadStateHolder.state

    private val searchTerm = MutableStateFlow<String?>(null)
    override fun onSearchType(query: String) {
        searchTerm.value = query
    }
    override fun onSearchEnter(query: String) = onSearchType(query)
    override fun onSearchCleared() {
        searchTerm.value = null
    }

    private val roomAccountDataList = MutableStateFlow<ImmutableList<DevToolsStateLikeEventContent<StateLikeType.RoomAccountData>>?>(null)
    private val roomStateList = MutableStateFlow<ImmutableList<DevToolsStateLikeEventContent<StateLikeType.RoomState>>?>(null)

    val sectionedList = combine(
        roomAccountDataList,
        roomStateList,
        searchTerm,
    ) { roomAccountData, roomState, search ->
        roomAccountData ?: return@combine null
        roomState ?: return@combine null
        val searchLower = search?.lowercase()
        persistentListOf(
            DevToolsSection.EventList(
                Res.string.hint_room_account_data.toStringHolder(),
                roomAccountData.filterForSearch(searchLower),
                searchLower,
            ),
            DevToolsSection.EventList(
                Res.string.hint_room_state.toStringHolder(),
                roomState.filterForSearch(searchLower),
                searchLower,
            ),
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val clientFlow = UiState.selectClient(sessionId, viewModelScope, loadStateHolder)

    private val roomFlow = clientFlow.map {
        it ?: return@map null
        it.getRoom(roomId).also { room ->
            loadStateHolder.set(LoadCheckPoint.Room, room.asCheckpointLoadedOrFailed())
        }
    }
        .flowClosable()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val roomInfo = roomFlow.flatMapLatest {
        it?.roomInfoFlow ?: flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val roomContextSuggestionsProvider = RoomContextSuggestionsProvider(
        sessionId = sessionId,
        roomId = roomId,
        peekRoom = { roomFlow.value },
    )

    val roomActionProvider = RoomActionProvider(
        sessionId = sessionId,
        roomId = roomId,
        isInvite = false,
        peekClient = { clientFlow.value },
        peekRoom = { roomFlow.value },
    )

    override val windowTitle = roomInfo.map { info ->
        windowTitle(
            roomId = roomId,
            sessionId = sessionId,
            roomTitle = info?.privateRoomName ?: info?.name,
        )
    }

    init {
        clientFlow.onEach { client ->
            client?.let(::refreshRoomAccountData)
        }.launchIn(viewModelScope)
        roomFlow.onEach { client ->
            client?.let(::refreshRoomState)
        }.launchIn(viewModelScope)
    }

    fun refresh() {
        clientFlow.value?.let(::refreshRoomAccountData)
        roomFlow.value?.let(::refreshRoomState)
    }

    private fun refreshRoomAccountData(client: MatrixClient) {
        viewModelScope.launch {
            client.getRoomAccountData(roomId).also {
                log.d { "Room account data refresh successful" }
                loadStateHolder.handleResult(LoadCheckPoint.RoomAccountData, it)
            }.onFailure {
                log.e("Failed to fetch account data", it)
            }.getOrNull()?.let { accountDataRawEvents ->
                roomAccountDataList.value = accountDataRawEvents.map { event ->
                    DevToolsStateLikeEventContent(
                        type = StateLikeType.RoomAccountData(roomId, event.eventType),
                        content = event.content.toPrettyJson {
                            log.e("Failed to prettify json for event ${event.eventType}", it)
                        },
                    )
                }.sortedBy { it.type.eventType }.toPersistentList()
            }
        }
    }

    private fun refreshRoomState(room: BaseRoom) {
        viewModelScope.launch {
            room.fetchFullRoomState().also {
                log.d { "Room account data refresh successful" }
                loadStateHolder.handleResult(LoadCheckPoint.RoomAccountData, it)
            }.onFailure {
                log.e("Failed to fetch account data", it)
            }.getOrNull()?.let { accountDataRawEvents ->
                val permissions = room.roomPermissions().onFailure {
                    log.e("Failed to fetch room permissions", it)
                }.getOrNull()
                roomStateList.value = accountDataRawEvents.mapNotNull { rawEvent ->
                    try {
                        val parsed = Json.parseToJsonElement(rawEvent).jsonObject
                        val content = parsed["content"]?.let { PrettyJson.encodeToString(it) } ?: ""
                        val type = parsed["type"]!!.jsonPrimitive.contentOrNull!!
                        val stateKey = parsed["state_key"]!!.jsonPrimitive.contentOrNull!!
                        DevToolsStateLikeEventContent(
                            type = StateLikeType.RoomState(roomId, type, stateKey),
                            content = content,
                            canEdit = permissions?.canOwnUserSendState(StateEventType.Custom(type)) == true,
                        )
                    } catch (e: Exception) {
                        log.e("Failed to parse state event $rawEvent", e)
                        null
                    }
                }.sortedBy { it.type.eventType }.toPersistentList()
            }
        }
    }

    suspend fun persist(type: StateLikeType, content: String): Result<Unit> {
        return when (type) {
            is StateLikeType.AccountData ->
                Result.failure(IllegalArgumentException("Tried to edit incompatible type $type"))
            is StateLikeType.RoomAccountData -> {
                val client = clientFlow.value ?: return Result.failure(IllegalStateException("Client not ready"))
                client.setRoomAccountData(type.roomId, type.eventType, content).also {
                    if (it.isSuccess) {
                        // Why do I need that? :(
                        delay(500)
                        refreshRoomAccountData(client)
                    } else {
                        log.e("Failed to set room account data ${type.eventType}", it.exceptionOrNull())
                    }
                }
            }
            is StateLikeType.RoomState -> {
                if (roomId != type.roomId) {
                    return Result.failure(IllegalArgumentException("RoomId mismatch"))
                }
                val room = roomFlow.value ?: return Result.failure(IllegalStateException("Room not ready"))
                room.sendRawState(type.eventType, type.stateKey, content).map { }.also {
                    if (it.isSuccess) {
                        // Why do I need that? :(
                        delay(500)
                        refreshRoomState(room)
                    } else {
                        log.e("Failed to set room state ${type.eventType}", it.exceptionOrNull())
                    }
                }
            }
        }
    }

    companion object {
        fun factory(
            sessionId: SessionId,
            roomId: RoomId,
        ) = viewModelFactory {
            initializer {
                RoomDevToolsViewModel(sessionId, roomId)
            }
        }

        fun windowTitle(
            roomId: RoomId,
            sessionId: SessionId,
            roomTitle: String?,
        ) = StringResourceHolder(
            Res.string.dual_title_format,
            Res.string.room_dev_tools_title.toStringHolder(),
            StringResourceHolder(
                Res.string.dual_title_format,
                (roomTitle ?: roomId.value).toStringHolder(),
                sessionId.value.toStringHolder(),
            )
        )
    }

    override fun verifyDestination(destination: Destination) =
        destination is Destination.RoomDevTools && destination.sessionId == sessionId && destination.roomId == roomId
}
