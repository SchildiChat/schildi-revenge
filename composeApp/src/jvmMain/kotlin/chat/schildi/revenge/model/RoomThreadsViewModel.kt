package chat.schildi.revenge.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import chat.schildi.revenge.Destination
import chat.schildi.revenge.TitleProvider
import chat.schildi.revenge.UiState
import chat.schildi.revenge.actions.RoomContextSuggestionsProvider
import chat.schildi.revenge.compose.search.SearchProvider
import chat.schildi.revenge.util.flowClosable
import chat.schildi.resources.ComposableStringHolder
import chat.schildi.resources.toStringHolder
import chat.schildi.revenge.MessageFormatDefaults
import chat.schildi.revenge.model.conversation.MessageMetadata
import chat.schildi.revenge.model.conversation.matchesSearch
import chat.schildi.revenge.model.conversation.messageMetadata
import co.touchlab.kermit.Logger
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.room.threads.ThreadListItem
import io.element.android.libraries.matrix.api.room.threads.ThreadListPaginationStatus
import io.element.android.libraries.matrix.api.room.threads.ThreadsListService
import io.element.android.libraries.matrix.api.timeline.item.event.getDisplayName
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
import kotlinx.coroutines.launch
import shire.res.generated.resources.Res
import shire.res.generated.resources.dual_title_format
import shire.res.generated.resources.dual_title_format_colon
import shire.res.generated.resources.threads_title
import kotlin.collections.map

data class RoomThreadsState(
    val items: ImmutableList<ScThreadListItem>,
    val searchTerm: String?,
)

data class ScThreadListItem(
    val item: ThreadListItem,
    val rootEventMessageMetadata: MessageMetadata? = item.rootEvent.content?.messageMetadata(
        style = MessageFormatDefaults.parseStyleForStrippedFormatting,
    ),
    val latestEventMessageMetadata: MessageMetadata? = item.latestEvent?.content?.messageMetadata(
        style = MessageFormatDefaults.parseStyleForStrippedFormatting,
    ),
)

@OptIn(ExperimentalCoroutinesApi::class)
class RoomThreadsViewModel(
    val sessionId: SessionId,
    val roomId: RoomId,
) : ViewModel(), TitleProvider, SearchProvider {

    private val log = Logger.withTag("RoomThreads")

    private val loadStateHolder = LoadStateHolder(
        LoadCheckPoint.Client(sessionId),
        LoadCheckPoint.Room,
        LoadCheckPoint.RoomThreads,
    )
    val loadState = loadStateHolder.state

    private val clientFlow = UiState.selectClient(sessionId, viewModelScope, loadStateHolder)

    private val joinedRoomFlow = clientFlow.map { client ->
        client ?: return@map null
        client.getJoinedRoom(roomId).also {
            loadStateHolder.set(LoadCheckPoint.Room, it.asCheckpointLoadedOrFailed())
        }
    }.flowClosable().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val roomInfo = joinedRoomFlow.flatMapLatest { room ->
        room?.roomInfoFlow ?: flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val threadsItems = joinedRoomFlow.flatMapLatest { room ->
        room?.threadsListService?.subscribeToItemUpdates()?.map {
            it.map(::ScThreadListItem)
        } ?: flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val paginationStatus: StateFlow<ThreadListPaginationStatus?> = joinedRoomFlow
        .flatMapLatest { room ->
            room?.threadsListService?.subscribeToPaginationUpdates() ?: flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val searchTerm = MutableStateFlow<String?>(null)

    val state: StateFlow<RoomThreadsState?> = combine(
        threadsItems,
        searchTerm,
    ) { items, searchTerm ->
        if (items == null) {
            null
        } else if (searchTerm.isNullOrEmpty()) {
            RoomThreadsState(
                items = items.toImmutableList(),
                searchTerm = null,
            )
        } else {
            RoomThreadsState(
                items = items.filter {
                    it.item.rootEvent.content?.matchesSearch(searchTerm) == true ||
                            it.item.rootEvent.senderProfile.getDisplayName()?.contains(searchTerm, ignoreCase = true) == true ||
                            it.item.rootEvent.senderId.value.contains(searchTerm, ignoreCase = true)
                }.toImmutableList(),
                searchTerm = searchTerm,
            )
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val isLoading: StateFlow<Boolean> = combine(
        state,
        paginationStatus,
        loadState,
    ) { state, paginationStatus, loadStateEntries ->
        val firstPageState = loadStateEntries.firstOrNull { it.checkpoint == LoadCheckPoint.RoomThreads }?.state
        when {
            state == null -> true
            state.items.isEmpty() && (
                firstPageState != CheckpointLoadState.LOADED ||
                        paginationStatus is ThreadListPaginationStatus.Loading
                ) -> true
            else -> false
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val userProfile = clientFlow.flatMapLatest { it?.userProfile ?: flowOf(null) }

    override val windowTitle: Flow<ComposableStringHolder?> = combine(
        roomInfo,
        userProfile
    ) { info, user ->
        windowTitle(roomId, info?.name, user?.displayName, sessionId)
    }

    val actionProvider = RoomActionProvider(
        sessionId = sessionId,
        roomId = roomId,
        isInvite = false,
        peekClient = { clientFlow.value },
        peekRoom = { joinedRoomFlow.value },
    )

    val roomContextSuggestionsProvider = RoomContextSuggestionsProvider(
        sessionId = sessionId,
        roomId = roomId,
        peekRoom = { joinedRoomFlow.value },
    )

    init {
        joinedRoomFlow.onEach { room ->
            val service = room?.threadsListService ?: return@onEach
            val result = paginate(service)
            if (result.isSuccess) {
                loadStateHolder.set(LoadCheckPoint.RoomThreads, CheckpointLoadState.LOADED)
            }
        }.launchIn(viewModelScope)
    }

    fun loadMore() {
        when (val status = paginationStatus.value) {
            null,
            is ThreadListPaginationStatus.Loading -> return
            is ThreadListPaginationStatus.Idle -> if (!status.hasMoreToLoad) return
        }
        viewModelScope.launch {
            joinedRoomFlow.value?.threadsListService?.let { paginate(it) }
        }
    }

    private suspend fun paginate(service: ThreadsListService): Result<Unit> {
        return service.paginate().onFailure {
            log.e("Failed to load threads", it)
            loadStateHolder.set(LoadCheckPoint.RoomThreads, CheckpointLoadState.FAILED)
        }
    }

    override fun onSearchType(query: String) {
        searchTerm.value = query
    }

    override fun onSearchEnter(query: String) = onSearchType(query)

    override fun onSearchCleared() {
        searchTerm.value = null
    }

    override fun verifyDestination(destination: Destination): Boolean {
        return destination is Destination.RoomThreads &&
                destination.sessionId == sessionId &&
                destination.roomId == roomId
    }

    companion object {
        fun factory(
            sessionId: SessionId,
            roomId: RoomId,
        ) = viewModelFactory {
            initializer {
                RoomThreadsViewModel(sessionId, roomId)
            }
        }

        fun windowTitle(
            roomId: RoomId,
            roomName: String?,
            userDisplayName: String? = null,
            sessionId: SessionId,
        ) = Res.string.dual_title_format.toStringHolder(
            Res.string.dual_title_format_colon.toStringHolder(
                Res.string.threads_title.toStringHolder(),
                (roomName ?: roomId.value).toStringHolder(),
            ),
            (userDisplayName ?: sessionId.value).toStringHolder(),
        )
    }
}
