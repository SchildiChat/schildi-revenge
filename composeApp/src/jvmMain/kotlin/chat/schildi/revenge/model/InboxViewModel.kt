package chat.schildi.revenge.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chat.schildi.preferences.RevengePrefs
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
import kotlinx.collections.immutable.toImmutableList
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
)

private data class InboxSettings(
    val sdkSettings: ScSdkInboxSettings,
    val hiddenAccounts: Set<SessionId>,
)

class InboxViewModel(
    combinedSessions: CombinedSessions = UiState.combinedSessions,
) : ViewModel(), SearchProvider, KeyboardActionProvider {
    private val log = Logger.withTag("Inbox")

    init {
        log.d { "Init" }
    }

    private val searchTerm = MutableStateFlow<String?>(null)

    private val sdkSettings = RevengePrefs.combinedSettingFlow { lookup ->
        ScSdkInboxSettings(
            sortOrder = ScSdkRoomSortOrder(
                byUnread = ScPrefs.SORT_BY_UNREAD.safeLookup(lookup),
                pinFavourites = ScPrefs.PIN_FAVORITES.safeLookup(lookup),
                buryLowPriority = ScPrefs.BURY_LOW_PRIORITY.safeLookup(lookup),
                clientSideUnreadCounts = ScPrefs.CLIENT_GENERATED_UNREAD_COUNTS.safeLookup(lookup),
                withSilentUnread = ScPrefs.SORT_WITH_SILENT_UNREAD.safeLookup(lookup),
            )
        )
    }

    val hiddenAccounts = MutableStateFlow(setOf<SessionId>())

    private val settings = combine(
        sdkSettings,
        hiddenAccounts
    ) { sdkSettings, hiddenAccounts ->
        InboxSettings(sdkSettings, hiddenAccounts)
    }.stateIn(viewModelScope, SharingStarted.Eagerly,
        InboxSettings(
            ScSdkInboxSettings(
                sortOrder = ScSdkRoomSortOrder(
                    byUnread = RevengePrefs.getCachedOrDefaultValue(ScPrefs.SORT_BY_UNREAD),
                    pinFavourites = RevengePrefs.getCachedOrDefaultValue(ScPrefs.PIN_FAVORITES),
                    buryLowPriority = RevengePrefs.getCachedOrDefaultValue(ScPrefs.BURY_LOW_PRIORITY),
                    clientSideUnreadCounts = RevengePrefs.getCachedOrDefaultValue(ScPrefs.CLIENT_GENERATED_UNREAD_COUNTS),
                    withSilentUnread = RevengePrefs.getCachedOrDefaultValue(ScPrefs.SORT_WITH_SILENT_UNREAD),
                )
            ),
            emptySet(),
        )
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val allRooms = combinedSessions.flatMergeCombinedWith(
        map = { input, settings ->
            if (input.client.sessionId in settings.hiddenAccounts) {
                flowOf(emptyList())
            } else {
                input.client.roomListService.allRooms.summaries.map {
                    it.map {
                        ScopedRoomSummary(input.client.sessionId, it)
                    }
                }
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
            ) { user, roomListState, syncState, hiddenAccounts ->
                InboxAccount(user, roomListState, syncState, user.userId in hiddenAccounts)
            }
        },
        merge = {
            it.toImmutableList()
        }
    ).stateIn(viewModelScope, SharingStarted.Lazily, null)

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
        }
    }

    fun setAccountVisible(sessionId: SessionId, visible: Boolean) {
        hiddenAccounts.update {
            if (visible) {
                it - sessionId
            } else {
                it + sessionId
            }
        }
    }
}
