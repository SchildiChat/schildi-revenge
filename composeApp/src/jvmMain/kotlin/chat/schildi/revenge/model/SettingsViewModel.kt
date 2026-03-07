package chat.schildi.revenge.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import chat.schildi.preferences.AbstractScPref
import chat.schildi.preferences.ScPrefContainer
import chat.schildi.preferences.ScPrefFilter
import chat.schildi.preferences.ScPrefScreen
import chat.schildi.preferences.ScPrefs
import chat.schildi.preferences.filteredBy
import chat.schildi.preferences.findPreferenceContainer
import chat.schildi.preferences.forEachPreferenceOrContainer
import chat.schildi.preferences.hasDirectChild
import chat.schildi.revenge.Destination
import chat.schildi.revenge.TitleProvider
import chat.schildi.revenge.compose.components.ComposableStringLookupRequest
import chat.schildi.revenge.compose.components.ComposableStringLookupTable
import chat.schildi.revenge.compose.search.SearchProvider
import chat.schildi.revenge.compose.util.toStringHolder
import co.touchlab.kermit.Logger
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class PrefScreenState(
    val prefScreen: ScPrefContainer,
    val searchQuery: String? = null,
)

class SettingsViewModel(
    rootPreferenceCategory: String? = null
) : ViewModel(), SearchProvider, TitleProvider {
    private val log = Logger.withTag("SettingsViewModel")

    private val rootPrefs: ScPrefContainer = if (rootPreferenceCategory != null) {
        ScPrefs.rootPrefs.findPreferenceContainer { it.sKey == rootPreferenceCategory }
            ?: run {
                log.e("Did not find initial preference category $rootPreferenceCategory")
                ScPrefs.rootPrefs
            }
    } else {
        ScPrefs.rootPrefs
    }

    private val rootPreferenceKey = rootPrefs.sKey
    val isRootPreferences = rootPrefs == ScPrefs.rootPrefs

    val parentPreferenceKey = if (isRootPreferences) {
        null
    } else {
        ScPrefs.rootPrefs.findPreferenceContainer { pref ->
            pref.hasDirectChild(
                allowedIntermediate = { it !is ScPrefScreen },
            ) {
                (it as? ScPrefContainer)?.sKey == rootPreferenceKey
            }
        }?.sKey
    }

    override val windowTitle = flowOf(rootPrefs.titleRes.toStringHolder())
    override fun verifyDestination(destination: Destination) =
        (destination is Destination.Settings) ||
                (destination as? Destination.SettingsPane)?.rootPreferenceCategory == rootPrefs.sKey

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
        val pref = if (query.isNullOrBlank() || lookup == null) {
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
        PrefScreenState(
            prefScreen = pref,
            searchQuery = query,
        )
    }.flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.Lazily, PrefScreenState(rootPrefs))

    override fun onSearchType(query: String) {
        searchQuery.value = query
    }

    override fun onSearchEnter(query: String) {
        searchQuery.value = query
    }

    override fun onSearchCleared() {
        searchQuery.value = null
    }

    companion object {
        fun factory(
            rootPreferenceCategory: String? = null,
        ) = viewModelFactory {
            initializer {
                SettingsViewModel(rootPreferenceCategory)
            }
        }
    }
}
