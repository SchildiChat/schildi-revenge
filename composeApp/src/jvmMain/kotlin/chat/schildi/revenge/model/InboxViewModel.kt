package chat.schildi.revenge.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chat.schildi.preferences.RevengePrefs
import chat.schildi.preferences.ScPref
import chat.schildi.preferences.ScPrefs
import chat.schildi.preferences.safeLookup
import chat.schildi.revenge.CombinedSessions
import chat.schildi.revenge.UiState
import chat.schildi.revenge.actions.KeyboardActionProvider
import chat.schildi.revenge.compose.search.SearchProvider
import chat.schildi.revenge.config.keybindings.Action
import chat.schildi.revenge.config.keybindings.KeyTrigger
import chat.schildi.revenge.flatMerge
import chat.schildi.revenge.flatMergeCombinedWith
import chat.schildi.revenge.util.mergeLists
import co.touchlab.kermit.Logger
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.roomlist.RoomListFilter
import io.element.android.libraries.matrix.api.roomlist.RoomListService
import io.element.android.libraries.matrix.api.roomlist.RoomSummary
import io.element.android.libraries.matrix.api.roomlist.ScSdkInboxSettings
import io.element.android.libraries.matrix.api.roomlist.ScSdkRoomSortOrder
import io.element.android.libraries.matrix.api.sync.SyncState
import io.element.android.libraries.matrix.api.user.MatrixUser
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.toPersistentHashMap
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScopedRoomSummary(
    val sessionId: SessionId,
    val summary: RoomSummary,
) {
    val draftKey: DraftKey
        get() = DraftKey(sessionId, summary.roomId)
}

data class InboxAccount(
    val user: MatrixUser,
    val roomListState: RoomListService.State,
    val syncState: SyncState,
    val isHidden: Boolean,
    val isSelected: Boolean,
    val isCurrentlyVisible: Boolean,
)

private data class InboxSettings(
    val sdkSettings: ScSdkInboxSettings,
    val hiddenAccounts: Set<SessionId>,
    val selectedAccounts: Set<SessionId>,
)

private fun buildScSdkInboxSettings(lookup: (ScPref<*>) -> Any?) = ScSdkInboxSettings(
    sortOrder = ScSdkRoomSortOrder(
        byUnread = ScPrefs.SORT_BY_UNREAD.safeLookup(lookup),
        pinFavourites = ScPrefs.PIN_FAVORITES.safeLookup(lookup),
        buryLowPriority = ScPrefs.BURY_LOW_PRIORITY.safeLookup(lookup),
        clientSideUnreadCounts = ScPrefs.CLIENT_GENERATED_UNREAD_COUNTS.safeLookup(lookup),
        withSilentUnread = ScPrefs.SORT_WITH_SILENT_UNREAD.safeLookup(lookup),
    )
)

class InboxViewModel(
    private val combinedSessions: CombinedSessions = UiState.combinedSessions,
) : ViewModel(), SearchProvider, KeyboardActionProvider {
    private val log = Logger.withTag("Inbox")

    init {
        log.d { "Init" }
    }

    private val searchTerm = MutableStateFlow<String?>(null)

    private val sdkSettings = RevengePrefs.combinedSettingFlow { lookup ->
        buildScSdkInboxSettings(lookup)
    }

    // If an account is selected, automatically all non-selected accounts are treated as hidden,
    // and selected accounts are even shown even if they're otherwise muted.
    // Think of this as a selected=single, hidden=mute from a mixing control table.
    val selectedAccounts = MutableStateFlow(setOf<SessionId>())
    val hiddenAccounts = MutableStateFlow(setOf<SessionId>())

    private val settings = combine(
        sdkSettings,
        hiddenAccounts,
        selectedAccounts,
    ) { sdkSettings, hiddenAccounts, selectedAccounts ->
        InboxSettings(
            sdkSettings = sdkSettings,
            hiddenAccounts = hiddenAccounts,
            selectedAccounts = selectedAccounts,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly,
        InboxSettings(
            buildScSdkInboxSettings { RevengePrefs.getCachedOrDefaultValue(it) },
            emptySet(),
            emptySet(),
        )
    )

    /**
     * All rooms for the current account selection, merged together with appropriate sort order.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val allRooms = combinedSessions.flatMergeCombinedWith(
        map = { input, settings ->
            val sessionId = input.client.sessionId
            val isAccountEnabled = when {
                settings.selectedAccounts.isNotEmpty() -> sessionId in settings.selectedAccounts
                sessionId in settings.hiddenAccounts -> false
                else -> true
            }
            if (isAccountEnabled) {
                input.client.roomListService.allRooms.summaries.map {
                    it.map {
                        ScopedRoomSummary(input.client.sessionId, it)
                    }
                }
            } else {
                flowOf(emptyList())
            }
        },
        onUpdatedInput = { it, settings ->
            it.forEach {
                log.v("Init for ${it.client.sessionId}")
                it.client.roomListService.allRooms.updateSettings(settings.sdkSettings)
                it.client.roomListService.allRooms.updateFilter(RoomListFilter.All(emptyList()))
                it.client.roomListService.allRooms.loadMore()
            }
        },
        merge = { it, settings ->
            log.v("Merging room lists [${it.joinToString { it.size.toString() }}]")
            mergeLists(
                *it,
                key = { it },
                comparator = settings.sdkSettings.sortOrder.toComparator { it.summary },
            )
        },
        other = settings,
    )

    /**
     * Rooms filtered by search and TODO space selection.
     */
    val rooms = combine(
        allRooms,
        searchTerm,
    ) { rooms, searchTerm ->
        if (searchTerm.isNullOrBlank()) {
            rooms
        } else {
            val lowercaseSearch = searchTerm.lowercase()
            rooms.filter {
                it.summary.info.name?.lowercase()?.contains(lowercaseSearch) == true
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

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
    ).stateIn(viewModelScope, SharingStarted.Lazily, null)

    // TODO user-defined sort order
    val accountsSorted = accounts.map { it?.values?.sortedBy { it.user.userId.value }?.toPersistentList() }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val roomsByRoomId = allRooms.map {
        it.groupBy { it.summary.roomId }.toPersistentHashMap()
    }.stateIn(viewModelScope, SharingStarted.Lazily, persistentHashMapOf())

    val dmsByHeroes = allRooms.map {
        it.filter { it.summary.isOneToOne }.groupBy { it.summary.info.heroes }.toPersistentHashMap()
    }.stateIn(viewModelScope, SharingStarted.Lazily, persistentHashMapOf())

    fun onVisibleRoomsChanged(visibleRooms: List<ScopedRoomSummary>) {
        val roomsBySession = visibleRooms.groupBy { it.sessionId }
        viewModelScope.launch {
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

    override fun handleNavigationModeEvent(key: KeyTrigger): Boolean {
        val keyConfig = UiState.keybindingsConfig.value
        val inboxAction = keyConfig.inbox.find { it.trigger == key } ?: return false
        return when (inboxAction.action) {
            Action.Inbox.SetSetting -> {
                viewModelScope.launch {
                    RevengePrefs.handleSetAction(inboxAction.args)
                }
                true
            }
            Action.Inbox.ToggleSetting -> {
                viewModelScope.launch {
                    RevengePrefs.handleToggleAction(inboxAction.args)
                }
                true
            }
            Action.Inbox.SetAccountHidden -> {
                if (inboxAction.args.size != 2) {
                    log.e("Invalid parameter size for SetAccountHidden action, expected 2 got ${inboxAction.args.size}")
                    return false
                }
                val sessionId = findSessionIdForAccountAction(inboxAction.args[0]) ?: return false
                val hidden = inboxAction.args[1].toBoolean()
                setAccountHidden(sessionId, hidden)
                true
            }
            Action.Inbox.SetAccountSelected -> {
                if (inboxAction.args.size != 2) {
                    log.e("Invalid parameter size for SetAccountSelected action, expected 2 got ${inboxAction.args.size}")
                    return false
                }
                val sessionId = findSessionIdForAccountAction(inboxAction.args[0]) ?: return false
                val selected = inboxAction.args[1].toBoolean()
                setAccountSelected(sessionId, selected)
                true
            }
            Action.Inbox.SetAccountExclusivelySelected -> {
                if (inboxAction.args.size != 2) {
                    log.e("Invalid parameter size for SetAccountExclusivelySelected action, expected 2 got ${inboxAction.args.size}")
                    return false
                }
                val sessionId = findSessionIdForAccountAction(inboxAction.args[0]) ?: return false
                val selected = inboxAction.args[1].toBoolean()
                setAccountExclusivelySelected(sessionId, selected)
                true
            }
            Action.Inbox.ToggleAccountHidden -> {
                if (inboxAction.args.size != 1) {
                    log.e("Invalid parameter size for ToggleAccountHidden action, expected 1 got ${inboxAction.args.size}")
                    return false
                }
                val sessionId = findSessionIdForAccountAction(inboxAction.args[0]) ?: return false
                toggleAccountHidden(sessionId)
                true
            }
            Action.Inbox.ToggleAccountSelected -> {
                if (inboxAction.args.size != 1) {
                    log.e("Invalid parameter size for ToggleAccountSelected action, expected 1 got ${inboxAction.args.size}")
                    return false
                }
                val sessionId = findSessionIdForAccountAction(inboxAction.args[0]) ?: return false
                toggleAccountSelected(sessionId)
                true
            }
            Action.Inbox.ToggleAccountExclusivelySelected -> {
                if (inboxAction.args.size != 1) {
                    log.e("Invalid parameter size for ToggleAccountExclusivelySelected action, expected 1 got ${inboxAction.args.size}")
                    return false
                }
                val sessionId = findSessionIdForAccountAction(inboxAction.args[0]) ?: return false
                toggleAccountExclusivelySelected(sessionId)
                true
            }
        }
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
}
