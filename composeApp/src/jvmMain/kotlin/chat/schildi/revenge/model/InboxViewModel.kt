package chat.schildi.revenge.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chat.schildi.revenge.preferences.RevengePrefs
import chat.schildi.lib.preferences.ScPreferencesStore
import chat.schildi.lib.preferences.ScPrefs
import chat.schildi.lib.preferences.safeLookup
import chat.schildi.revenge.CombinedSessions
import chat.schildi.revenge.Destination
import chat.schildi.revenge.GlobalActionsScope
import chat.schildi.revenge.TitleProvider
import chat.schildi.revenge.UiState
import chat.schildi.revenge.actions.ActionContext
import chat.schildi.revenge.actions.ActionResult
import chat.schildi.revenge.actions.FlatMergedKeyboardActionProvider
import chat.schildi.revenge.actions.KeyboardActionProvider
import chat.schildi.revenge.actions.execute
import chat.schildi.revenge.actions.launchActionAsync
import chat.schildi.revenge.actions.orActionValidationError
import chat.schildi.revenge.actions.toActionResult
import chat.schildi.revenge.compose.destination.inbox.isInvite
import chat.schildi.revenge.compose.search.SearchProvider
import chat.schildi.resources.ComposableStringHolder
import chat.schildi.resources.StringResourceHolder
import chat.schildi.revenge.config.keybindings.Action
import chat.schildi.revenge.config.keybindings.KeyTrigger
import chat.schildi.revenge.flatMerge
import chat.schildi.revenge.model.invites.SeenInvitesStore
import chat.schildi.revenge.model.conversation.MessageMetadata
import chat.schildi.revenge.model.spaces.PSEUDO_SPACE_ID_NO_FILTER
import chat.schildi.revenge.model.spaces.RevengeSpaceListDataSource
import chat.schildi.revenge.model.spaces.SpaceAggregationDataSource
import chat.schildi.revenge.model.spaces.SpaceListDataSource
import chat.schildi.revenge.model.spaces.filterByVisible
import chat.schildi.revenge.model.spaces.filterHierarchical
import chat.schildi.revenge.model.spaces.findInHierarchy
import chat.schildi.revenge.model.spaces.resolveSelection
import chat.schildi.revenge.store.AppStateStore
import chat.schildi.revenge.store.PersistentInboxState
import chat.schildi.revenge.util.combine
import chat.schildi.revenge.util.throttleLatest
import co.touchlab.kermit.Logger
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.room.RoomNotificationSettings
import io.element.android.libraries.matrix.api.roomlist.RoomListService
import io.element.android.libraries.matrix.api.roomlist.RoomSummary
import io.element.android.libraries.matrix.api.sync.SyncState
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.matrix.api.verification.SessionVerifiedStatus
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import shire.res.generated.resources.Res
import shire.res.generated.resources.inbox
import shire.res.generated.resources.inbox_search
import kotlin.collections.map

data class ScopedRoomSummary(
    val sessionId: SessionId,
    val summary: RoomSummary,
    val latestEventMessageMetadata: MessageMetadata?,
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
    val sessionVerifiedStatus: SessionVerifiedStatus?,
    val isHidden: Boolean,
    val isSelected: Boolean,
    val isCurrentlyVisible: Boolean,
    val isMuted: Boolean,
) {
    val shouldHideErrors: Boolean
        get() = isMuted && !isCurrentlyVisible
}

private data class InboxSettings(
    val hideEmptyUnreadPseudoSpaces: Boolean,
    val showAllRoomsSpace: Boolean,
    val hiddenAccounts: Set<SessionId>,
    val selectedAccounts: Set<SessionId>,
)

data class RoomListState(
    val rooms: ImmutableList<ScopedRoomSummary>,
    val searchTerm: String?,
)

class InboxViewModel(
    private val combinedSessions: CombinedSessions = UiState.combinedSessions,
    private val roomListDataSource: RoomListDataSource = RevengeRoomListDataSource,
    private val spaceListDataSource: SpaceListDataSource = RevengeSpaceListDataSource,
    private val scPreferencesStore: ScPreferencesStore = RevengePrefs,
    private val sessionIdComparatorFlow: Flow<Comparator<SessionId>> = UiState.sessionIdComparator,
    private val appStateStore: AppStateStore = UiState.appStateStore,
    private val seenInvitesStore: SeenInvitesStore = UiState.appStateStore,
) : ViewModel(), SearchProvider, KeyboardActionProvider<Action.Inbox>, TitleProvider {
    private val log = Logger.withTag("Inbox")

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
    }.flowOn(Dispatchers.Default)

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
    }.flowOn(Dispatchers.Default)

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
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _spaceSelection = MutableStateFlow<ImmutableList<String>>(persistentListOf())
    val spaceSelection = _spaceSelection.asStateFlow()

    val selectedSpace = combine(
        spaces,
        spaceSelection
    ) { spaces, spaceSelection ->
        spaces?.resolveSelection(spaceSelection, followWildcards = true)
    }

    val showSpaceUi = searchTerm.map {
        it.isNullOrBlank()
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    /**
     * Rooms filtered by search and space selection.
     */
    val filteredRooms = combine(
        allRooms,
        searchTerm,
        selectedSpace,
        UiState.sessionIdOrder,
        seenInvitesStore.seenInvites(),
    ) { rooms, searchTerm, selectedSpace, sessionIdOrder, seenRoomInvites ->
        // Only filter by spaces if search term is empty
        if (searchTerm.isNullOrBlank()) {
            RoomListState(
                rooms = selectedSpace?.applyFilter(rooms, seenRoomInvites) ?: rooms.toPersistentList(),
                searchTerm = null,
            )
        } else {
            val lowercaseSearch = searchTerm.lowercase()
            val searchedRooms = rooms.filter {
                it.summary.info.name?.lowercase()?.contains(lowercaseSearch) == true ||
                        it.summary.info.privateRoomName?.lowercase()?.contains(lowercaseSearch) == true
            }.sortedWith(compareBy(
                {
                    minOf(
                        it.summary.info.name?.lowercase()?.indexOf(lowercaseSearch)?.takeIf { it >= 0 } ?: Integer.MAX_VALUE,
                        it.summary.info.privateRoomName?.lowercase()?.indexOf(lowercaseSearch)?.takeIf { it >= 0 } ?: Integer.MAX_VALUE,
                    )
                },
                { it.summary.latestEventTimestamp == null },
                { it.summary.latestEventTimestamp?.let { -it } },
                { sessionIdOrder[it.sessionId.value] ?: Int.MAX_VALUE },
            ))
            RoomListState(
                rooms = searchedRooms.toPersistentList(),
                searchTerm = searchTerm,
            )
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val accounts = combinedSessions.flatMerge(
        map = {
            combine(
                it.client.userProfile,
                it.client.roomListService.state,
                it.client.syncService.syncState,
                it.client.sessionVerificationService.sessionVerifiedStatus,
                hiddenAccounts,
                selectedAccounts,
                UiState.mutedAccounts,
            ) { user, roomListState, syncState, verifiedStatus, hiddenAccounts, selectedAccounts, mutedAccounts ->
                val isHidden = user.userId in hiddenAccounts
                val isSelected = user.userId in selectedAccounts
                val isMuted = user.userId in mutedAccounts.orEmpty()
                InboxAccount(
                    user = user,
                    roomListState = roomListState,
                    syncState = syncState,
                    sessionVerifiedStatus = verifiedStatus,
                    isHidden = isHidden,
                    isSelected = isSelected,
                    isCurrentlyVisible = if (selectedAccounts.isEmpty()) !isHidden else isSelected,
                    isMuted = isMuted,
                )
            }
        },
        merge = {
            it.associateBy { it.user.userId }.toPersistentHashMap()
        },
        onEmpty = { persistentHashMapOf() },
    )
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val accountsSorted = combine(
        accounts,
        sessionIdComparatorFlow
    ) { it, comparator ->
        it?.values?.sortedWith { l, r ->
            comparator.compare(l.user.userId, r.user.userId)
        }?.toPersistentList()
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val accountUnreadCounts = spaceAggregationDataSource.state.map {
        it.enrichedSpaces?.mapNotNull {
            it as? SpaceListDataSource.SessionIdPseudoSpaceItem
        }?.associate {
            it.sessionId to (it.unreadCounts ?: SpaceAggregationDataSource.SpaceUnreadCounts())
        }.orEmpty().toPersistentHashMap()
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Lazily, persistentHashMapOf())

    val roomsByRoomId = allRooms.map {
        it.groupBy { it.summary.roomId }.toPersistentHashMap()
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Lazily, persistentHashMapOf())

    val dmsByHeroes = allRooms.map {
        it.filter { it.summary.isDm }.groupBy { it.summary.info.heroes }.toPersistentHashMap()
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Lazily, persistentHashMapOf())

    val spaceSummariesByKey = spaceListDataSource.spaceSummariesByKey
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Lazily, persistentHashMapOf())

    init {
        log.d { "Init" }
        // Restore inbox state
        viewModelScope.launch {
            val lastInboxState = appStateStore.config.filterNotNull().first().lastInboxState
            log.d { "Restoring last inbox state: ${lastInboxState != null}" }
            if (lastInboxState != null) {
                if (lastInboxState.hiddenAccounts.isNotEmpty()) {
                    hiddenAccounts.update {
                        if (it.isEmpty()) {
                            lastInboxState.hiddenAccounts.map { SessionId(it) }.toSet()
                        } else {
                            log.w { "Race condition restoring hidden accounts from last inbox state" }
                            it
                        }
                    }
                }
                if (lastInboxState.spaceSelection.isNotEmpty()) {
                    _spaceSelection.update {
                        if (it.isEmpty()) {
                            lastInboxState.spaceSelection.toPersistentList()
                        } else {
                            log.w { "Race condition restoring space selection from last inbox state" }
                            it
                        }
                    }
                }
            }
        }
        // Persist inbox state
        combine(
            spaceSelection,
            hiddenAccounts
        ) { spaceSelection, hiddenAccounts ->
            PersistentInboxState(
                spaceSelection = spaceSelection,
                hiddenAccounts = hiddenAccounts.map { it.value }.sorted(),
            )
        }
            .distinctUntilChanged()
            .throttleLatest(1000)
            .onEach {
                appStateStore.persistInboxState(it)
            }
            .flowOn(Dispatchers.Default)
            .launchIn(viewModelScope)
    }

    fun followNotificationSettings(
        room: ScopedRoomSummary,
    ): Flow<RoomNotificationSettings?> {
        // Not following client refreshes should be fine for calling sites
        val client = UiState.currentClientFor(room.sessionId) ?: return flowOf(null)
        return flow {
            emit(getRoomNotificationSettings(client, room))
            client
                .notificationSettingsService
                .notificationSettingsChangeFlow.collect {
                    emit(getRoomNotificationSettings(client, room))
                }
        }
    }

    private suspend fun getRoomNotificationSettings(client: MatrixClient, room: ScopedRoomSummary): RoomNotificationSettings? {
        val result = client.notificationSettingsService.getRoomNotificationSettings(
            room.summary.roomId,
            room.summary.info.isEncrypted == true,
            room.summary.isDm,
        )
        if (result.isFailure) {
            log.e("Failed to read notification settings for ${room.summary.roomId} via ${room.sessionId}")
        }
        return result.getOrNull()
    }

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

    override fun getPossibleActions(): Set<Action.Inbox> = Action.Inbox.entries.toSet()
    override fun ensureActionType(action: Action) = action as? Action.Inbox

    override fun handleNavigationModeEvent(context: ActionContext, key: KeyTrigger): ActionResult {
        val keyConfig = context.keybindingConfig ?: return ActionResult.NoMatch
        return keyConfig.inbox.execute(context, key, ::handleAction)
    }

    override fun handleAction(
        context: ActionContext,
        action: Action.Inbox,
        args: List<String>,
    ): ActionResult = context.run {
        when (action) {
            Action.Inbox.SetAccountHidden -> {
                val sessionId = findSessionIdForAccountAction(args[0])
                    ?: return@run ActionResult.Failure("Failed to find user session")
                val hidden = args.getOrNull(1)?.toBoolean() ?: true
                setAccountHidden(sessionId, hidden)
                ActionResult.Success()
            }

            Action.Inbox.SetAccountSelected -> {
                val sessionId = findSessionIdForAccountAction(args[0])
                    ?: return@run ActionResult.Failure("Failed to find user session")
                val selected = args.getOrNull(1)?.toBoolean() ?: true
                setAccountSelected(sessionId, selected)
                ActionResult.Success()
            }

            Action.Inbox.SetAccountExclusivelySelected -> {
                val sessionId = findSessionIdForAccountAction(args[0])
                    ?: return@run ActionResult.Failure("Failed to find user session")
                val selected = args.getOrNull(1)?.toBoolean() ?: true
                setAccountExclusivelySelected(sessionId, selected)
                ActionResult.Success()
            }

            Action.Inbox.SetAccountMuted -> {
                val sessionId = findSessionIdForAccountAction(args[0])
                    ?: return@run ActionResult.Failure("Failed to find user session")
                val selected = args.getOrNull(1)?.toBoolean() ?: true
                setAccountMuted(sessionId, selected)
                ActionResult.Success()
            }

            Action.Inbox.ToggleAccountHidden -> {
                val sessionId = findSessionIdForAccountAction(args[0])
                    ?: return@run ActionResult.Failure("Failed to find user session")
                toggleAccountHidden(sessionId)
                ActionResult.Success()
            }

            Action.Inbox.ToggleAccountSelected -> {
                val sessionId = findSessionIdForAccountAction(args[0])
                    ?: return@run ActionResult.Failure("Failed to find user session")
                toggleAccountSelected(sessionId)
                ActionResult.Success()
            }

            Action.Inbox.ToggleAccountExclusivelySelected -> {
                val sessionId = findSessionIdForAccountAction(args[0])
                    ?: return@run ActionResult.Failure("Failed to find user session")
                toggleAccountExclusivelySelected(sessionId)
                ActionResult.Success()
            }

            Action.Inbox.ToggleAccountMuted -> {
                val sessionId = findSessionIdForAccountAction(args[0])
                    ?: return@run ActionResult.Failure("Failed to find user session")
                toggleAccountMuted(sessionId)
                ActionResult.Success()
            }

            Action.Inbox.NavigateSpaceRelative -> {
                val diff = args[0].toIntOrNull().orActionValidationError()
                navigateSpaceRelative(diff)
            }

            Action.Inbox.SelectSpaceIfNotHidden -> {
                val spaceSelection = args[0]
                navigateToSpaceById(spaceSelection) { selection ->
                    spaces.value?.resolveSelection(selection)?.shouldShow(filterByUnread = true) == true
                }
            }

            Action.Inbox.SelectSpace -> {
                val spaceSelection = args[0]
                val asIndex = spaceSelection.toIntOrNull()
                if (asIndex != null) {
                    navigateToSpaceIndex(asIndex)
                } else {
                    navigateToSpaceById(spaceSelection)
                }
            }

            Action.Inbox.ToggleSpaceExpanded,
            Action.Inbox.SetSpaceExpanded -> {
                val currentSelection = spaceSelection.value
                val isAlreadyExpanded = currentSelection.lastOrNull() == PSEUDO_SPACE_ID_NO_FILTER
                val currentSpace = spaces.value?.resolveSelection(currentSelection)
                val shouldExpand = if (action == Action.Inbox.ToggleSpaceExpanded) {
                    !isAlreadyExpanded && !currentSpace?.spaces.isNullOrEmpty()
                } else {
                    args.firstOrNull()?.toBooleanStrictOrNull() ?: true
                }
                if (shouldExpand) {
                    if (currentSpace is SpaceListDataSource.SpaceHierarchyItem) {
                        setSpaceSelection(currentSelection + PSEUDO_SPACE_ID_NO_FILTER)
                        ActionResult.Success()
                    } else {
                        ActionResult.Inapplicable
                    }
                } else if (currentSelection.size > 1) {
                    setSpaceSelection(currentSelection.take(currentSelection.size - 1))
                    ActionResult.Success()
                } else {
                    ActionResult.Inapplicable
                }
            }
        }
    }

    private fun navigateSpaceInCurrentHierarchyLevel(
        select: (List<SpaceListDataSource.AbstractSpaceHierarchyItem?>, List<String>, List<String>) -> ActionResult
    ): ActionResult {
        val currentSpaces = spaces.value ?: return ActionResult.Failure("No spaces found")
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
            return ActionResult.Inapplicable
        }
        return select(currentSpaceLevel, currentSelection, currentParentSelection)
    }

    private fun navigateSpaceRelative(diff: Int): ActionResult = navigateSpaceInCurrentHierarchyLevel { currentSpaceLevel, currentSelection, currentParentSelection ->
        val currentIndex = if (currentSelection.isEmpty()) {
            0
        } else {
            val currentSpaceSelectionId = currentSelection.last()
            currentSpaceLevel.indexOfFirst {
                (it?.selectionId ?: PSEUDO_SPACE_ID_NO_FILTER) == currentSpaceSelectionId
            }
        }
        val navigatedIndex = (currentIndex + diff).coerceIn(-1, currentSpaceLevel.size - 1)
        if (navigatedIndex == currentIndex) {
            return@navigateSpaceInCurrentHierarchyLevel ActionResult.NoOp
        }
        setSpaceSelection(
            currentParentSelection + listOf(currentSpaceLevel.getOrNull(navigatedIndex)?.selectionId ?: PSEUDO_SPACE_ID_NO_FILTER)
        )
        return@navigateSpaceInCurrentHierarchyLevel ActionResult.Success()
    }

    private fun navigateToSpaceIndex(index: Int): ActionResult = navigateSpaceInCurrentHierarchyLevel { currentSpaceLevel, currentSelection, currentParentSelection ->
        val navigatedIndex = index.coerceIn(0, currentSpaceLevel.size - 1)
        val newSelectionId = currentSpaceLevel[navigatedIndex]?.selectionId
        if (newSelectionId == currentSelection.lastOrNull()) {
            return@navigateSpaceInCurrentHierarchyLevel ActionResult.NoOp
        }
        setSpaceSelection(currentParentSelection + listOfNotNull(newSelectionId))
        return@navigateSpaceInCurrentHierarchyLevel ActionResult.Success()
    }

    private fun navigateToSpaceById(spaceId: String, condition: (List<String>) -> Boolean = { true }): ActionResult {
        val currentSpaces = spaces.value ?: return ActionResult.Failure("No spaces found")
        val condition: (SpaceListDataSource.AbstractSpaceHierarchyItem) -> Boolean = when {
            spaceId.startsWith("!") -> {{
                (it as? SpaceListDataSource.SpaceHierarchyItem)?.room?.summary?.roomId?.value == spaceId
            }}
            spaceId.startsWith("@") -> {{
                (it as? SpaceListDataSource.SessionIdPseudoSpaceItem)?.sessionId?.value == spaceId
            }}
            RevengeSpaceListDataSource.isValidPseudoSpaceId(spaceId) -> {{
                (it as? SpaceListDataSource.PseudoSpaceItem)?.id == spaceId
            }}
            else -> {{
                it.selectionId == spaceId
            }}
        }
        return currentSpaces.findInHierarchy(condition)?.let {
            if (it == _spaceSelection.value) {
                ActionResult.NoOp
            } else {
                if (condition(it)) {
                    setSpaceSelection(it)
                    ActionResult.Success()
                } else {
                    ActionResult.NoOp
                }
            }
        } ?: ActionResult.Failure("Space with ID $spaceId not found")
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

    fun setAccountMuted(sessionId: SessionId, muted: Boolean) = UiState.setAccountMuted(sessionId, muted)

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

    fun toggleAccountMuted(sessionId: SessionId) = UiState.toggleAccountMuted(sessionId)

    fun setSpaceSelection(selection: List<String>) {
        _spaceSelection.value = selection.toImmutableList()
    }

    override val windowTitle: Flow<ComposableStringHolder?> = combine(
        selectedSpace,
        searchTerm
    ) { space, query ->
        if (query.isNullOrBlank()) {
            space?.name ?: StringResourceHolder(Res.string.inbox)
        } else {
            StringResourceHolder(Res.string.inbox_search)
        }
    }

    override fun verifyDestination(destination: Destination) = destination is Destination.Inbox

    fun joinRoom(context: ActionContext, sessionId: SessionId, roomId: RoomId): ActionResult {
        val client = UiState.currentClientFor(sessionId) ?: return ActionResult.Failure("Client not ready for $sessionId")
        return context.launchActionAsync(
            "joinRoom",
            GlobalActionsScope,
            Dispatchers.IO,
            "joinRoom",
            notifyProcessing = true
        ) {
            client.joinRoomTracked(roomId).toActionResult()
        }
    }

    fun getKeyboardActionProviderForRoom(
        sessionId: SessionId,
        roomId: RoomId,
        isInvite: Boolean
    ): KeyboardActionProvider<Action.Room> {
        return RoomActionProvider(
            sessionId = sessionId,
            roomId = roomId,
            isInvite = isInvite,
            peekClient = { UiState.currentClientFor(sessionId) },
            peekRoom = null,
        )
    }

    fun getKeyboardActionProviderForSpace(
        space: SpaceListDataSource.SpaceHierarchyItem,
    ) = FlatMergedKeyboardActionProvider(
        listOf(
            getKeyboardActionProviderForRoom(
                sessionId = space.room.sessionId,
                roomId = space.room.summary.roomId,
                isInvite = space.room.summary.isInvite(),
            ),
            SpaceActionProvider(space, UiState::currentClientFor)
        )
    )
}
