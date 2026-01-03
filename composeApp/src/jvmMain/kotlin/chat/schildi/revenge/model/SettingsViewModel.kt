package chat.schildi.revenge.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chat.schildi.preferences.AbstractScPref
import chat.schildi.preferences.ScPref
import chat.schildi.preferences.ScPrefContainer
import chat.schildi.preferences.ScPrefFilter
import chat.schildi.preferences.ScPrefScreen
import chat.schildi.preferences.ScPrefs
import chat.schildi.preferences.filteredBy
import chat.schildi.preferences.forEachPreference
import chat.schildi.preferences.forEachPreferenceOrContainer
import chat.schildi.revenge.Destination
import chat.schildi.revenge.TitleProvider
import chat.schildi.revenge.compose.components.ComposableStringLookupRequest
import chat.schildi.revenge.compose.components.ComposableStringLookupTable
import chat.schildi.revenge.compose.search.SearchProvider
import chat.schildi.revenge.compose.util.toStringHolder
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class SettingsViewModel(
    private val rootPrefs: ScPrefScreen = ScPrefs.rootPrefs,
) : ViewModel(), SearchProvider, TitleProvider {
    override val windowTitle = flowOf(rootPrefs.titleRes.toStringHolder())
    override fun verifyDestination(destination: Destination) = destination == Destination.Settings

    val stringLookupRequest = ComposableStringLookupRequest(
        buildList {
            rootPrefs.forEachPreferenceOrContainer {
                add(it.titleRes)
                it.summaryRes?.let(::add)
            }
        }.toImmutableList()
    )
    var stringLookupTable: ComposableStringLookupTable? = null

    private val searchQuery = MutableStateFlow<String?>(null)
    val prefScreen = searchQuery.map { query ->
        val lookup = stringLookupTable?.stringLookup
        if (query.isNullOrBlank() || lookup == null) {
            rootPrefs
        } else {
            val lowerQuery = query.lowercase()
            fun prefLookupMatches(pref: AbstractScPref): Boolean {
                val title = lookup[pref.titleRes]?.lowercase() ?: ""
                val summary = lookup[pref.summaryRes]?.lowercase() ?: ""
                return title.contains(lowerQuery) || summary.contains(lowerQuery)
            }
            val filter = ScPrefFilter(
                // Include SC prefs that match
                predicate = ::prefLookupMatches,
                // Include pref categories in full that match
                prePredicate = ::prefLookupMatches,
            )
            rootPrefs.filteredBy(filter)
        }
    }.flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.Lazily, rootPrefs)

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
