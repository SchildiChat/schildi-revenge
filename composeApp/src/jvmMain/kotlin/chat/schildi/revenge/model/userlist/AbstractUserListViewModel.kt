package chat.schildi.revenge.model.userlist

import androidx.lifecycle.ViewModel
import chat.schildi.revenge.actions.KeyboardActionProvider
import chat.schildi.revenge.actions.UserIdSuggestion
import chat.schildi.revenge.actions.UserIdSuggestionsProvider
import chat.schildi.revenge.compose.search.SearchProvider
import chat.schildi.revenge.model.UserActionProvider
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.core.UserId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest

interface UserListItem {
    val userId: UserId
    val displayName: String?
    val avatarUrl: String?
}

abstract class AbstractUserListViewModel<T: UserListItem>: ViewModel(), SearchProvider, UserIdSuggestionsProvider {
    abstract val sessionId: SessionId
    abstract val roomId: RoomId?
    abstract val allEntries: Flow<ImmutableList<T>?>

    protected val searchTerm = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    protected fun filteredEntriesFlow() = combine(
        allEntries,
        searchTerm,
    ) { allEntries, searchTerm ->
        Pair(allEntries, searchTerm)
    }.mapLatest { (allEntries, searchTerm) ->
        if (searchTerm == null) {
            allEntries
        } else {
            val lowercaseSearchTerm = searchTerm.lowercase()
            allEntries?.filter {
                searchMatches(it, lowercaseSearchTerm)
            }
        }
    }.flowOn(Dispatchers.Default)

    open fun getItemActionHandler(userId: UserId): KeyboardActionProvider<*> =
        UserActionProvider(sessionId, userId, roomId)

    open fun searchMatches(item: T, lowercaseSearchTerm: String): Boolean {
        return item.displayName?.lowercase()?.contains(lowercaseSearchTerm) == true ||
                item.userId.value.lowercase().contains(lowercaseSearchTerm)
    }

    override fun onSearchType(query: String) {
        searchTerm.value = query
    }
    override fun onSearchEnter(query: String) = onSearchType(query)
    override fun onSearchCleared() {
        searchTerm.value = null
    }

    fun userIdInRoomSuggestionsFlow(): Flow<List<UserIdSuggestion>> = allEntries.map {
        it?.map {
            UserIdSuggestion(it.userId, it.displayName)
        }.orEmpty()
    }
}
