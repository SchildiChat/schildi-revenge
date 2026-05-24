package chat.schildi.revenge.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chat.schildi.revenge.Destination
import chat.schildi.revenge.TitleProvider
import chat.schildi.revenge.UiState
import chat.schildi.revenge.compose.search.SearchProvider
import chat.schildi.revenge.compose.util.ComposableStringHolder
import chat.schildi.revenge.compose.util.toStringHolder
import chat.schildi.revenge.flatMergeCombinedWith
import chat.schildi.revenge.model.account.AccountComparator
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.user.MatrixUser
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.select_account

data class SessionSelectorAccount(
    val user: MatrixUser,
) {
    val sessionId: SessionId
        get() = user.userId
}

data class SessionSelectorAccountList(
    val accounts: ImmutableList<SessionSelectorAccount>,
    val searchTerm: String? = null,
)

class SessionSelectorViewModel : ViewModel(), SearchProvider, TitleProvider {
    private val searchQuery = MutableStateFlow<String?>(null)

    val accounts = UiState.combinedSessions.flatMergeCombinedWith(
        map = { session, _ ->
            session.client.userProfile
        },
        merge = { users, comparator ->
            users
                .sortedWith(AccountComparator(comparator) { it.userId })
                .map(::SessionSelectorAccount)
                .toPersistentList()
        },
        onEmpty = { persistentListOf() },
        other = UiState.sessionIdComparator,
    )
        .flowOn(Dispatchers.IO)
        .stateIn<ImmutableList<SessionSelectorAccount>?>(viewModelScope, SharingStarted.Eagerly, null)

    val filteredAccounts = combine(accounts, searchQuery) { accounts, query ->
        if (accounts == null) {
            null
        } else if (query.isNullOrBlank()) {
            SessionSelectorAccountList(accounts)
        } else {
            val lowerQuery = query.lowercase()
            SessionSelectorAccountList(
                accounts = accounts.filter { account ->
                    account.sessionId.value.lowercase().contains(lowerQuery) ||
                            account.user.displayName?.lowercase()?.contains(lowerQuery) == true
                }.toPersistentList(),
                searchTerm = query,
            )
        }
    }.flowOn(Dispatchers.IO)
        .stateIn<SessionSelectorAccountList?>(viewModelScope, SharingStarted.Lazily, null)

    override val windowTitle: Flow<ComposableStringHolder?> = flowOf(Res.string.select_account.toStringHolder())
    override fun verifyDestination(destination: Destination) = destination is Destination.SessionSelector

    override fun onSearchType(query: String) {
        searchQuery.value = query
    }

    override fun onSearchEnter(query: String) {
        searchQuery.value = query
    }

    override fun onSearchCleared() {
        searchQuery.value = null
    }
}
