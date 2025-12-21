package chat.schildi.revenge.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chat.schildi.preferences.RevengePrefs
import chat.schildi.preferences.ScPreferencesStore
import chat.schildi.preferences.ScPrefs
import chat.schildi.preferences.safeLookup
import chat.schildi.revenge.CombinedSessions
import chat.schildi.revenge.Destination
import chat.schildi.revenge.TitleProvider
import chat.schildi.revenge.UiState
import chat.schildi.revenge.actions.ActionContext
import chat.schildi.revenge.actions.ActionResult
import chat.schildi.revenge.actions.KeyboardActionProvider
import chat.schildi.revenge.actions.execute
import chat.schildi.revenge.actions.orActionFailure
import chat.schildi.revenge.actions.orActionInapplicable
import chat.schildi.revenge.actions.orActionValidationError
import chat.schildi.revenge.compose.search.SearchProvider
import chat.schildi.revenge.compose.util.ComposableStringHolder
import chat.schildi.revenge.compose.util.StringResourceHolder
import chat.schildi.revenge.config.keybindings.Action
import chat.schildi.revenge.config.keybindings.KeyTrigger
import chat.schildi.revenge.flatMerge
import chat.schildi.revenge.flatMergeCombinedWith
import chat.schildi.revenge.model.spaces.RevengeSpaceListDataSource
import chat.schildi.revenge.model.spaces.SpaceAggregationDataSource
import chat.schildi.revenge.model.spaces.SpaceListDataSource
import chat.schildi.revenge.model.spaces.filterByVisible
import chat.schildi.revenge.model.spaces.filterHierarchical
import chat.schildi.revenge.model.spaces.findInHierarchy
import chat.schildi.revenge.model.spaces.resolveSelection
import co.touchlab.kermit.Logger
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.roomlist.RoomListService
import io.element.android.libraries.matrix.api.roomlist.RoomSummary
import io.element.android.libraries.matrix.api.sync.SyncState
import io.element.android.libraries.matrix.api.user.MatrixUser
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentHashMap
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.inbox

data class ScopedRoomSummary(
    val sessionId: SessionId,
    val summary: RoomSummary,
) {
    val key = ScopedRoomKey(sessionId, summary.roomId)
}

data class ScopedRoomKey(
    val sessionId: SessionId,
    val roomId: RoomId,
)

data class InboxAccount(
    val user: MatrixUser,
    val roomListState: RoomListService.State,
    val syncState: SyncState,
    val isHidden: Boolean,
    val isSelected: Boolean,
    val isCurrentlyVisible: Boolean,
)

private data class InboxSettings(
    val hideEmptyUnreadPseudoSpaces: Boolean,
    val showAllRoomsSpace: Boolean,
    val hiddenAccounts: Set<SessionId>,
    val selectedAccounts: Set<SessionId>,
)

class InboxViewModel(
    private val combinedSessions: CombinedSessions = UiState.combinedSessions,
    private val roomListDataSource: RoomListDataSource = RevengeRoomListDataSource,
    private val spaceListDataSource: SpaceListDataSource = RevengeSpaceListDataSource,
    private val scPreferencesStore: ScPreferencesStore = RevengePrefs,
    private val sessionIdComparatorFlow: Flow<Comparator<SessionId>> = UiState.sessionIdComparator,
) : ViewModel(), SearchProvider, KeyboardActionProvider, TitleProvider {
    private val log = Logger.withTag("Inbox")

    init {
        log.d { "Init" }
    }

    private val searchTerm = MutableStateFlow<String?>(null)

    // If an account is selected, automatically all non-selected accounts are treated as hidden,
    // and selected accounts are even shown even if they're otherwise muted.
    // Think of this as a selected=single, hidden=mute from a mixing control table.
    val selectedAccounts = MutableStateFlow(setOf<SessionId>())
    val hiddenAccounts = MutableStateFlow(setOf<SessionId>())

    private val settings = combine(
        scPreferencesStore.combinedSettingFlow { lookup ->
            Pair(
                ScPrefs.PSEUDO_SPACE_HIDE_EMPTY_UNREAD.safeLookup(lookup),
                ScPrefs.PSEUDO_SPACE_ALL_ROOMS.safeLookup(lookup),
            )
        },
        hiddenAccounts,
        selectedAccounts,
    ) { (hideEmptyUnreadPseudoSpaces, showAllRoomsSpace), hiddenAccounts, selectedAccounts ->
        InboxSettings(
            hideEmptyUnreadPseudoSpaces = hideEmptyUnreadPseudoSpaces,
            showAllRoomsSpace = showAllRoomsSpace,
            hiddenAccounts = hiddenAccounts,
            selectedAccounts = selectedAccounts,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly,
        InboxSettings(
            scPreferencesStore.getCachedOrDefaultValue(ScPrefs.PSEUDO_SPACE_HIDE_EMPTY_UNREAD),
            scPreferencesStore.getCachedOrDefaultValue(ScPrefs.PSEUDO_SPACE_ALL_ROOMS),
            emptySet(),
            emptySet(),
        )
    )

    /**
     * All rooms for the current account selection, merged together with appropriate sort order.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val allRooms = combine(
        roomListDataSource.allRooms,
        settings,
    ) { rooms, settings ->
        rooms.filter { room ->
            when {
                settings.selectedAccounts.isNotEmpty() -> room.sessionId in settings.selectedAccounts
                room.sessionId in settings.hiddenAccounts -> false
                else -> true
            }
        }
    }.flowOn(Dispatchers.IO)

    private val spacesFilteredByAccount = combine(
        spaceListDataSource.allSpacesHierarchical,
        hiddenAccounts,
        selectedAccounts,
    ) { spaces, hiddenAccounts, selectedAccounts ->
        spaces.filterHierarchical {
            val sessionIds = it.sessionIds
            sessionIds == null ||
                    selectedAccounts.isEmpty() && !hiddenAccounts.containsAll(sessionIds) ||
                    sessionIds.any { it in selectedAccounts }
        }.toImmutableList()
    }.flowOn(Dispatchers.IO)

    private val spaceAggregationDataSource = SpaceAggregationDataSource(
        spacesFilteredByAccount,
        allRooms,
    )

    val spaces = combine(
        spaceAggregationDataSource.state,
        hiddenAccounts,
        selectedAccounts,
    ) { spaces, hiddenAccounts, selectedAccounts ->
        spaces.enrichedSpaces?.filterHierarchical {
            val sessionIds = it.sessionIds
            sessionIds == null ||
                    selectedAccounts.isEmpty() && !hiddenAccounts.containsAll(sessionIds) ||
                    sessionIds.any { it in selectedAccounts }
        }?.toImmutableList()
    }.flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _spaceSelection = MutableStateFlow<ImmutableList<String>>(persistentListOf())
    val spaceSelection = _spaceSelection.asStateFlow()

    val selectedSpace = combine(
        spaces,
        spaceSelection
    ) { spaces, spaceSelection ->
        spaces?.resolveSelection(spaceSelection)
    }

    val showSpaceUi = searchTerm.map {
        it.isNullOrBlank()
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    /**
     * Rooms filtered by search and space selection.
     */
    val rooms = combine(
        allRooms,
        searchTerm,
        selectedSpace,
    ) { rooms, searchTerm, selectedSpace ->
        // Only filter by spaces if search term is empty
        if (searchTerm.isNullOrBlank()) {
            selectedSpace?.applyFilter(rooms) ?: rooms
        } else {
            val lowercaseSearch = searchTerm.lowercase()
            rooms.filter {
                it.summary.info.name?.lowercase()?.contains(lowercaseSearch) == true
            }
        }
    }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val accounts = combinedSessions.flatMerge(
        map = {
            combine(
                it.client.userProfile,
                it.client.roomListService.state,
                it.client.syncService.syncState,
                hiddenAccounts,
                selectedAccounts,
            ) { user, roomListState, syncState, hiddenAccounts, selectedAccounts ->
                val isHidden = user.userId in hiddenAccounts
                val isSelected = user.userId in selectedAccounts
                InboxAccount(
                    user = user,
                    roomListState = roomListState,
                    syncState = syncState,
                    isHidden = isHidden,
                    isSelected = isSelected,
                    isCurrentlyVisible = if (selectedAccounts.isEmpty()) !isHidden else isSelected,
                )
            }
        },
        merge = {
            it.associateBy { it.user.userId }.toPersistentHashMap()
        }
    )
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val accountsSorted = combine(
        accounts,
        sessionIdComparatorFlow
    ) { it, comparator ->
        it?.values?.sortedWith { l, r ->
            comparator.compare(l.user.userId, r.user.userId)
        }?.toPersistentList()
    }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val accountUnreadCounts = spaceAggregationDataSource.state.map {
        it.enrichedSpaces?.mapNotNull {
            it as? SpaceListDataSource.SessionIdPseudoSpaceItem
        }?.associate {
            it.sessionId to (it.unreadCounts ?: SpaceAggregationDataSource.SpaceUnreadCounts())
        }.orEmpty().toPersistentHashMap()
    }.flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.Lazily, persistentHashMapOf())

    val roomsByRoomId = allRooms.map {
        it.groupBy { it.summary.roomId }.toPersistentHashMap()
    }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.Lazily, persistentHashMapOf())

    val dmsByHeroes = allRooms.map {
        it.filter { it.summary.isOneToOne }.groupBy { it.summary.info.heroes }.toPersistentHashMap()
    }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.Lazily, persistentHashMapOf())

    fun onVisibleRoomsChanged(visibleRooms: List<ScopedRoomSummary>) {
        val roomsBySession = visibleRooms.groupBy { it.sessionId }
        viewModelScope.launch(Dispatchers.IO) {
            combinedSessions.value.forEach { session ->
                roomsBySession[session.client.sessionId]?.takeIf { it.isNotEmpty() }?.let {
                    log.v { "Subscribe to ${it.size} visible rooms for ${session.client.sessionId}" }
                    session.client.roomListService.subscribeToVisibleRooms(it.map { it.summary.roomId })
                }
            }
        }
    }

    override fun onSearchType(query: String) {
        searchTerm.value = query
    }

    override fun onSearchEnter(query: String) = onSearchType(query)

    override fun onSearchCleared() {
        searchTerm.value = null
    }

    override fun handleNavigationModeEvent(context: ActionContext, key: KeyTrigger): ActionResult {
        val keyConfig = UiState.keybindingsConfig.value ?: return ActionResult.NoMatch
        return keyConfig.inbox.execute(context, key) { inboxAction ->
            when (inboxAction.action) {
                Action.Inbox.SetAccountHidden -> {
                    val sessionId = findSessionIdForAccountAction(inboxAction.args[0])
                        ?: return@execute ActionResult.Failure("Failed to find user session")
                    val hidden = inboxAction.args.getOrNull(1)?.toBoolean() ?: true
                    setAccountHidden(sessionId, hidden)
                    ActionResult.Success()
                }

                Action.Inbox.SetAccountSelected -> {
                    val sessionId = findSessionIdForAccountAction(inboxAction.args[0])
                        ?: return@execute ActionResult.Failure("Failed to find user session")
                    val selected = inboxAction.args.getOrNull(1)?.toBoolean() ?: true
                    setAccountSelected(sessionId, selected)
                    ActionResult.Success()
                }

                Action.Inbox.SetAccountExclusivelySelected -> {
                    val sessionId = findSessionIdForAccountAction(inboxAction.args[0])
                        ?: return@execute ActionResult.Failure("Failed to find user session")
                    val selected = inboxAction.args.getOrNull(1)?.toBoolean() ?: true
                    setAccountExclusivelySelected(sessionId, selected)
                    ActionResult.Success()
                }

                Action.Inbox.ToggleAccountHidden -> {
                    val sessionId = findSessionIdForAccountAction(inboxAction.args[0])
                        ?: return@execute ActionResult.Failure("Failed to find user session")
                    toggleAccountHidden(sessionId)
                    ActionResult.Success()
                }

                Action.Inbox.ToggleAccountSelected -> {
                    val sessionId = findSessionIdForAccountAction(inboxAction.args[0])
                        ?: return@execute ActionResult.Failure("Failed to find user session")
                    toggleAccountSelected(sessionId)
                    ActionResult.Success()
                }

                Action.Inbox.ToggleAccountExclusivelySelected -> {
                    val sessionId = findSessionIdForAccountAction(inboxAction.args[0])
                        ?: return@execute ActionResult.Failure("Failed to find user session")
                    toggleAccountExclusivelySelected(sessionId)
                    ActionResult.Success()
                }

                Action.Inbox.NavigateSpaceRelative -> {
                    val diff = inboxAction.args[0].toIntOrNull().orActionValidationError()
                    navigateSpaceRelative(diff).orActionInapplicable()
                }

                Action.Inbox.SelectSpace -> {
                    val spaceSelection = inboxAction.args[0]
                    val asIndex = spaceSelection.toIntOrNull()
                    if (asIndex != null) {
                        navigateToSpaceIndex(asIndex).orActionInapplicable()
                    } else {
                        navigateToSpaceById(spaceSelection).orActionFailure("Space with ID $spaceSelection not found")
                    }
                }
            }
        }
    }

    private fun navigateSpaceInCurrentHierarchyLevel(
        select: (List<SpaceListDataSource.AbstractSpaceHierarchyItem?>, List<String>, List<String>) -> Boolean
    ): Boolean {
        val currentSpaces = spaces.value ?: return false
        val currentSelection = spaceSelection.value
        val currentParentSelection = if (currentSelection.isEmpty()) {
            emptyList()
        } else {
            currentSelection.subList(0, currentSelection.size - 1)
        }
        val currentSettings = settings.value
        val currentSpaceLevel = if (currentParentSelection.isEmpty()) {
            currentSpaces.filterByVisible(currentSelection, currentSettings.hideEmptyUnreadPseudoSpaces).let {
                if (currentSettings.showAllRoomsSpace) {
                    listOf(null) + it
                } else {
                    it
                }
            }
        } else {
            currentSpaces.resolveSelection(currentParentSelection)?.spaces
                ?: currentSpaces
        }
        if (currentSpaceLevel.size <= 1) {
            return false
        }
        return select(currentSpaceLevel, currentSelection, currentParentSelection)
    }

    private fun navigateSpaceRelative(diff: Int): Boolean = navigateSpaceInCurrentHierarchyLevel { currentSpaceLevel, currentSelection, currentParentSelection ->
        val currentIndex = if (currentSelection.isEmpty()) {
            0
        } else {
            val currentSpaceSelectionId = currentSelection.last()
            currentSpaceLevel.indexOfFirst { it?.selectionId == currentSpaceSelectionId }.takeIf { it >= 0 } ?: 0
        }
        val navigatedIndex = (currentIndex + diff).coerceIn(0, currentSpaceLevel.size - 1)
        if (navigatedIndex == currentIndex) {
            return@navigateSpaceInCurrentHierarchyLevel false
        }
        setSpaceSelection(currentParentSelection + listOfNotNull(currentSpaceLevel[navigatedIndex]?.selectionId))
        return@navigateSpaceInCurrentHierarchyLevel true
    }

    private fun navigateToSpaceIndex(index: Int): Boolean = navigateSpaceInCurrentHierarchyLevel { currentSpaceLevel, currentSelection, currentParentSelection ->
        val navigatedIndex = index.coerceIn(0, currentSpaceLevel.size - 1)
        val newSelectionId = currentSpaceLevel[navigatedIndex]?.selectionId
        if (newSelectionId == currentSelection.lastOrNull()) {
            return@navigateSpaceInCurrentHierarchyLevel false
        }
        setSpaceSelection(currentParentSelection + listOfNotNull(newSelectionId))
        return@navigateSpaceInCurrentHierarchyLevel true
    }

    private fun navigateToSpaceById(spaceId: String): Boolean {
        val currentSpaces = spaces.value ?: return false
        val condition: (SpaceListDataSource.AbstractSpaceHierarchyItem) -> Boolean = when {
            spaceId.startsWith("!") -> {{
                (it as? SpaceListDataSource.SpaceHierarchyItem)?.room?.summary?.roomId?.value == spaceId
            }}
            spaceId.startsWith("@") -> {{
                (it as? SpaceListDataSource.SessionIdPseudoSpaceItem)?.sessionId?.value == spaceId
            }}
            else -> {{
                it.selectionId == spaceId
            }}
        }
        return currentSpaces.findInHierarchy(condition)?.let {
            setSpaceSelection(it)
            true
        } ?: false
    }

    private fun findSessionIdForAccountAction(parameter: String): SessionId? {
        val index = parameter.toIntOrNull()
        val currentAccounts = accountsSorted.value ?: return null
        return if (index != null) {
            if (index > 0 && index <= currentAccounts.size) {
                currentAccounts[index-1].user.userId
            } else {
                log.e("Invalid index for account action: $index")
                null
            }
        } else {
            val found = currentAccounts.find { it.user.userId.value == parameter }
            if (found == null) {
                log.e("Cannot find account by ID: $parameter")
                null
            } else {
                found.user.userId
            }
        }
    }

    fun setAccountHidden(sessionId: SessionId, hidden: Boolean) {
        hiddenAccounts.update {
            if (hidden) {
                it + sessionId
            } else {
                it - sessionId
            }
        }
    }

    fun setAccountSelected(sessionId: SessionId, selected: Boolean) {
        selectedAccounts.update {
            if (selected) {
                it + sessionId
            } else {
                it - sessionId
            }
        }
    }

    fun setAccountExclusivelySelected(sessionId: SessionId, selected: Boolean) {
        if (selected) {
            selectedAccounts.value = setOf(sessionId)
        } else {
            selectedAccounts.value = setOf()
        }
    }

    fun toggleAccountHidden(sessionId: SessionId) {
        hiddenAccounts.update {
            if (sessionId in it) {
                it - sessionId
            } else {
                it + sessionId
            }
        }
    }

    fun toggleAccountSelected(sessionId: SessionId) {
        selectedAccounts.update {
            if (sessionId in it) {
                it - sessionId
            } else {
                it + sessionId
            }
        }
    }

    fun toggleAccountExclusivelySelected(sessionId: SessionId) {
        selectedAccounts.update {
            if (it.size == 1 && sessionId in it) {
                setOf()
            } else {
                setOf(sessionId)
            }
        }
    }

    fun setSpaceSelection(selection: List<String>) {
        _spaceSelection.value = selection.toImmutableList()
    }

    override val windowTitle: Flow<ComposableStringHolder?> = selectedSpace.map {
        it?.name ?: StringResourceHolder(Res.string.inbox)
    }

    override fun verifyDestination(destination: Destination) = destination is Destination.Inbox
}
