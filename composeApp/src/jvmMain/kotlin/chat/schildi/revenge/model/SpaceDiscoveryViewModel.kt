package chat.schildi.revenge.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import chat.schildi.revenge.Destination
import chat.schildi.revenge.GlobalActionsScope
import chat.schildi.revenge.TitleProvider
import chat.schildi.revenge.UiState
import chat.schildi.revenge.actions.ActionContext
import chat.schildi.revenge.actions.ActionResult
import chat.schildi.revenge.actions.KeyboardActionProvider
import chat.schildi.revenge.actions.launchActionAsync
import chat.schildi.revenge.actions.toActionResult
import chat.schildi.revenge.compose.search.SearchProvider
import chat.schildi.revenge.config.keybindings.Action
import chat.schildi.resources.ComposableStringHolder
import chat.schildi.resources.StringResourceHolder
import chat.schildi.resources.toStringHolder
import chat.schildi.revenge.util.flowClosable
import io.element.android.libraries.matrix.api.core.RoomAlias
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.core.toRoomIdOrAlias
import io.element.android.libraries.matrix.api.spaces.SpaceRoom
import io.element.android.libraries.matrix.api.spaces.SpaceRoomList
import io.element.android.libraries.matrix.api.spaces.loadAllIncrementally
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import shire.res.generated.resources.Res
import shire.res.generated.resources.dual_title_format
import kotlin.jvm.optionals.getOrNull

data class SpaceDiscoveryRooms(
    val rooms: List<SpaceRoom>,
    val searchTerm: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class SpaceDiscoveryViewModel(
    val sessionId: SessionId,
    val roomId: RoomId,
) : ViewModel(), TitleProvider, SearchProvider {
    private val loadStateHolder = LoadStateHolder(
        LoadCheckPoint.Client(sessionId),
        LoadCheckPoint.Room,
    )
    val loadState = loadStateHolder.state

    private val clientFlow = UiState.selectClient(sessionId, viewModelScope, loadStateHolder)

    private val spaceRoomList: StateFlow<SpaceRoomList?> = clientFlow
        .map { client ->
            client?.spaceService?.spaceRoomList(roomId).also {
                loadStateHolder.set(LoadCheckPoint.Room, it.asCheckpointLoadedOrPending())
                it?.loadAllIncrementally(viewModelScope)
            }
        }
        .flowClosable()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val spaceRooms: StateFlow<List<SpaceRoom>?> = spaceRoomList
        .flatMapLatest { it?.spaceRoomsFlow ?: flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val paginationStatus: StateFlow<SpaceRoomList.PaginationStatus?> = spaceRoomList
        .flatMapLatest {
            it?.paginationStatusFlow ?: flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val searchQuery = MutableStateFlow<String?>(null)

    val filteredSpaceRooms: StateFlow<SpaceDiscoveryRooms?> = combine(
        spaceRooms,
        searchQuery,
    ) { rooms, query ->
        if (rooms == null) {
            null
        } else if (query.isNullOrBlank()) {
            SpaceDiscoveryRooms(rooms)
        } else {
            val lowerQuery = query.lowercase()
            SpaceDiscoveryRooms(
                rooms = rooms.filter { room ->
                    room.displayName.lowercase().contains(lowerQuery) ||
                            room.rawName?.lowercase()?.contains(lowerQuery) == true ||
                            room.canonicalAlias?.value?.lowercase()?.contains(lowerQuery) == true
                },
                searchTerm = query,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    override fun onSearchType(query: String) {
        searchQuery.value = query
    }

    override fun onSearchEnter(query: String) = onSearchType(query)

    override fun onSearchCleared() {
        searchQuery.value = null
    }

    val currentSpace = spaceRoomList.flatMapLatest { it?.currentSpaceFlow ?: flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    override val windowTitle: Flow<ComposableStringHolder?> = currentSpace
        .map { windowTitle(roomId, sessionId, it?.getOrNull()?.displayName) }

    fun joinRoom(context: ActionContext, room: SpaceRoom): ActionResult {
        val client = UiState.currentClientFor(sessionId)
            ?: return ActionResult.Failure("Client not ready for $sessionId")
        return context.launchActionAsync(
            "joinRoom",
            GlobalActionsScope,
            Dispatchers.IO,
            "joinRoom",
            notifyProcessing = true,
        ) {
            if (room.via.isEmpty() && room.canonicalAlias == null) {
                client.joinRoomTracked(room.roomId).toActionResult()
            } else {
                client.joinRoomByIdAndAliasTracked(
                    room.roomId,
                    room.canonicalAlias?.toRoomIdOrAlias() ?: room.roomId.toRoomIdOrAlias(),
                    room.via,
                ).toActionResult()
            }
        }
    }

    fun getKeyboardActionProviderForRoom(
        sessionId: SessionId,
        roomId: RoomId,
        isInvite: Boolean,
        isJoined: Boolean,
        alias: RoomAlias?,
        joinServerNames: List<String>,
    ): KeyboardActionProvider<Action.Room> = RoomActionProvider(
        sessionId = sessionId,
        roomId = roomId,
        alias = alias,
        joinServerNames = joinServerNames,
        isInvite = isInvite,
        isJoined = isJoined,
        peekClient = { UiState.currentClientFor(sessionId) },
        peekRoom = null,
    )

    override fun verifyDestination(destination: Destination) =
        destination is Destination.SpaceDiscovery && destination.sessionId == sessionId && destination.roomId == roomId

    companion object {
        fun factory(
            sessionId: SessionId,
            roomId: RoomId,
        ) = viewModelFactory {
            initializer {
                SpaceDiscoveryViewModel(sessionId, roomId)
            }
        }

        fun windowTitle(
            roomId: RoomId,
            sessionId: SessionId,
            spaceName: String?,
        ) = StringResourceHolder(
            Res.string.dual_title_format,
            (spaceName ?: roomId.value).toStringHolder(),
            sessionId.value.toStringHolder(),
        )
    }
}
