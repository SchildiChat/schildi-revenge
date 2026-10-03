package chat.schildi.revenge.model.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import chat.schildi.resources.StringResourceHolder
import chat.schildi.resources.toStringHolder
import chat.schildi.revenge.Destination
import chat.schildi.revenge.NavigationPreference
import chat.schildi.revenge.TitleProvider
import chat.schildi.revenge.UiState
import chat.schildi.revenge.actions.ActionContext
import chat.schildi.revenge.actions.ActionResult
import chat.schildi.revenge.actions.AppMessage
import chat.schildi.revenge.compose.search.SearchProvider
import co.touchlab.kermit.Logger
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.MatrixPatterns
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.room.StartDMResult
import io.element.android.libraries.matrix.api.room.startDM
import io.element.android.libraries.matrix.api.user.MatrixUser
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import shire.res.generated.resources.Res
import shire.res.generated.resources.start_chat
import shire.res.generated.resources.toast_start_chat_error
import shire.res.generated.resources.toast_start_chat_success

private const val USER_SEARCH_LIMIT = 20L
private const val USER_SEARCH_DEBOUNCE_MILLIS = 300L

data class StartChatCandidate(
    val user: MatrixUser,
    /** Not returned by the user directory, but offered because the search term is a full MXID. */
    val isTypedUserId: Boolean,
)

sealed interface StartChatSearchState {
    data object Idle : StartChatSearchState
    data object Loading : StartChatSearchState
    data class Results(
        val candidates: ImmutableList<StartChatCandidate>,
        val limited: Boolean,
        /** Results for a previous search term are kept visible while a new search is running. */
        val loading: Boolean = false,
    ) : StartChatSearchState
    data class Error(val message: String?) : StartChatSearchState
}

sealed interface StartChatState {
    data object Idle : StartChatState
    data object InProgress : StartChatState
}

/**
 * Combine user directory results with the typed search term: the directory usually only knows users of the own
 * homeserver, so a full MXID is offered on top even if the directory didn't return it.
 */
private fun mergeStartChatCandidates(
    searchTerm: String,
    directoryResults: List<MatrixUser>,
    ownUserId: UserId,
): ImmutableList<StartChatCandidate> {
    val term = searchTerm.trim()
    val directoryCandidates = directoryResults
        .filter { it.userId != ownUserId }
        // Used as list keys
        .distinctBy { it.userId }
        .map { StartChatCandidate(it, isTypedUserId = false) }
    // MXIDs are case-sensitive in theory, but a directory hit only differing in case
    // is almost certainly the intended user, so don't offer a second candidate for it.
    val offerTypedUserId = MatrixPatterns.isUserId(term) &&
            UserId(term) != ownUserId &&
            directoryCandidates.none { it.user.userId.value.equals(term, ignoreCase = true) }
    return if (offerTypedUserId) {
        listOf(StartChatCandidate(MatrixUser(UserId(term)), isTypedUserId = true)) + directoryCandidates
    } else {
        directoryCandidates
    }.toPersistentList()
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class StartChatViewModel(
    initialSessionId: SessionId? = null,
) : ViewModel(), TitleProvider, SearchProvider {

    private val log = Logger.withTag("StartChat")

    private val _sessionId = MutableStateFlow(initialSessionId)
    val sessionId = _sessionId.asStateFlow()

    private val query = MutableStateFlow("")

    private val _state = MutableStateFlow<StartChatState>(StartChatState.Idle)
    val state = _state.asStateFlow()

    private val _searchState = MutableStateFlow<StartChatSearchState>(StartChatSearchState.Idle)
    val searchState = _searchState.asStateFlow()

    val availableSessionIds = combine(
        UiState.matrixClients,
        UiState.sessionIdComparator,
    ) { clients, comparator ->
        clients.keys.sortedWith(comparator).toPersistentList().also { sessionIds ->
            if (sessionIds.isNotEmpty() && sessionId.value == null) {
                _sessionId.update { it ?: sessionIds.first() }
            }
        }
    }

    private val clientFlow = sessionId.flatMapLatest { sessionId ->
        sessionId ?: return@flatMapLatest flowOf(null)
        UiState.selectClient(sessionId, viewModelScope)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        // Only debounce typing, switching accounts should search again right away
        val debouncedSearchTerm = query
            .map(String::trim)
            .distinctUntilChanged()
            .debounce { if (it.isEmpty()) 0L else USER_SEARCH_DEBOUNCE_MILLIS }
        viewModelScope.launch(Dispatchers.IO) {
            // collectLatest cancels a still running search when the term or the account changes
            combine(clientFlow, debouncedSearchTerm, ::Pair).collectLatest { (client, term) ->
                search(client, term)
            }
        }
    }

    private suspend fun search(client: MatrixClient?, term: String) {
        if (term.isEmpty()) {
            _searchState.value = StartChatSearchState.Idle
            return
        }
        _searchState.update {
            (it as? StartChatSearchState.Results)?.copy(loading = true) ?: StartChatSearchState.Loading
        }
        // Keep showing the loading state until the client is ready
        client ?: return
        val directoryResults = client.searchUsers(term, USER_SEARCH_LIMIT).getOrElse {
            log.w("User directory search failed", it)
            // A full MXID can still be offered without the directory
            if (!MatrixPatterns.isUserId(term)) {
                _searchState.value = StartChatSearchState.Error(it.message)
                return
            }
            null
        }
        val candidates = mergeStartChatCandidates(
            term,
            directoryResults?.results.orEmpty(),
            client.sessionId,
        )
        val limited = directoryResults?.limited == true
        _searchState.value = StartChatSearchState.Results(candidates, limited)

        // Best effort: show the profile of a typed MXID the directory didn't know
        val typedCandidate = candidates.firstOrNull()?.takeIf { it.isTypedUserId } ?: return
        val profile = client.getProfile(typedCandidate.user.userId)
            .onFailure { log.d("No profile for ${typedCandidate.user.userId}: ${it.message}") }
            .getOrNull() ?: return
        _searchState.value = StartChatSearchState.Results(
            candidates.toPersistentList().replacingAt(0, typedCandidate.copy(user = profile)),
            limited,
        )
    }

    override val windowTitle = flowOf(windowTitle())

    override fun verifyDestination(destination: Destination) = destination is Destination.StartChat

    override fun onSearchType(query: String) {
        this.query.value = query
    }

    override fun onSearchEnter(query: String) = onSearchType(query)

    override fun onSearchCleared() {
        query.value = ""
    }

    fun setSessionId(sessionId: SessionId): ActionResult {
        _sessionId.value = sessionId
        return ActionResult.Success()
    }

    /**
     * Open the DM with [userId], creating it if necessary, in place of this screen.
     * [onChatOpened] runs on the main thread after navigating, e.g. to leave the search mode of this screen.
     */
    fun startChat(userId: UserId, context: ActionContext, onChatOpened: () -> Unit = {}) {
        val sessionId = sessionId.value
        val client = clientFlow.value?.takeIf { it.sessionId == sessionId } ?: run {
            log.e("Cannot start chat with $userId without client for $sessionId")
            publishStartChatError(context)
            return
        }
        var wasStarting = false
        _state.update {
            wasStarting = it is StartChatState.InProgress
            StartChatState.InProgress
        }
        if (wasStarting) {
            log.w("Ignore start chat request while already in progress")
            return
        }
        log.i("Start chat with $userId for $sessionId")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                when (val result = client.startDM(userId, createIfDmDoesNotExist = true, isEncryped = true)) {
                    is StartDMResult.Success -> {
                        log.i("Open ${if (result.isNew) "new" else "existing"} chat ${result.roomId} with $userId")
                        withContext(Dispatchers.Main) {
                            openChat(client.sessionId, result.roomId, context)
                            onChatOpened()
                        }
                    }
                    is StartDMResult.Failure -> {
                        log.e("Failed to start chat with $userId", result.throwable)
                        publishStartChatError(context, result.throwable.message)
                    }
                    // Not expected with createIfDmDoesNotExist
                    StartDMResult.DmDoesNotExist -> {
                        log.e("No DM with $userId found although requested to create one")
                        publishStartChatError(context)
                    }
                }
            } finally {
                _state.value = StartChatState.Idle
            }
        }
    }

    private fun openChat(sessionId: SessionId, roomId: RoomId, context: ActionContext) {
        val destinationStateHolder = context.destinationStateHolder
        if (destinationStateHolder == null) {
            log.w("No destination to open chat $roomId in")
            context.publishMessage(
                AppMessage(
                    message = StringResourceHolder(
                        Res.string.toast_start_chat_success,
                        roomId.value.toStringHolder(),
                    ),
                )
            )
            return
        }
        // Replace this screen, like opening a room from the inbox does
        destinationStateHolder.navigate(Destination.Conversation(sessionId, roomId), NavigationPreference.REPLACE)
    }

    private fun publishStartChatError(context: ActionContext, message: String? = null) {
        context.publishMessage(
            AppMessage(
                message = message?.toStringHolder() ?: Res.string.toast_start_chat_error.toStringHolder(),
                isError = true,
            )
        )
    }

    companion object {
        fun factory(initialSessionId: SessionId?) = viewModelFactory {
            initializer {
                StartChatViewModel(initialSessionId)
            }
        }

        fun windowTitle() = Res.string.start_chat.toStringHolder()
    }
}
