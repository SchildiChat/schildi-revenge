package chat.schildi.revenge.compose.search

import androidx.compose.runtime.compositionLocalOf

val LocalSearchProvider = compositionLocalOf<SearchProvider?> { null }

interface SearchProvider {
    fun onSearchType(query: String)
    fun onSearchEnter(query: String)
    fun onSearchCleared()
}
