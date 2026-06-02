package chat.schildi.revenge.model.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chat.schildi.revenge.compose.search.SearchProvider
import co.touchlab.kermit.Logger
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.Json

data class DependencyInfo(
    val name: String,
    val url: String? = null,
    val license: String? = null,
    val licenseUrl: String? = null,
) {
    fun matches(search: String): Boolean {
        return name.contains(search, ignoreCase = true) ||
                license?.contains(search, ignoreCase = true) == true
    }
}

data class AboutScreenState(
    val acknowledgements: ImmutableList<ThirdPartyAcknowledgement>,
    val openSourceLicenses: ImmutableList<DependencyInfo>,
    val searchQuery: String? = null,
) {
    val isSearching = !searchQuery.isNullOrEmpty()
    val isEmpty = acknowledgements.isEmpty() && openSourceLicenses.isEmpty()
}

class AboutViewModel : ViewModel(), SearchProvider {
    private val log = Logger.withTag("About")

    private val json = Json { ignoreUnknownKeys = true }
    private val searchQuery = MutableStateFlow<String?>(null)

    private val openSourceLicenses = run {
        val stream = Thread.currentThread().contextClassLoader
            .getResourceAsStream("third-party-libs.json")
        if (stream == null) {
            log.e("Failed to read dependency report")
            persistentListOf()
        } else {
            try {
                val rawJson = stream.use { it.readBytes().decodeToString() }
                val report = json.decodeFromString<ThirdPartyLibsReport>(rawJson)
                report.dependencies
                    .map {
                        DependencyInfo(
                            name = it.moduleName,
                            url = it.moduleUrl,
                            license = it.moduleLicense,
                            licenseUrl = it.moduleLicenseUrl,
                        )
                    }
                    .distinctBy { it.name }
                    .sortedBy { it.name }
                    .toPersistentList()
            } catch (e: Exception) {
                log.e("Failed to parse dependency report", e)
                persistentListOf()
            }
        }
    }

    val state = searchQuery.map { query ->
        if (query.isNullOrEmpty()) {
            AboutScreenState(
                searchQuery = query,
                acknowledgements = ThirdPartyAcknowledgements,
                openSourceLicenses = openSourceLicenses,
            )
        } else {
            AboutScreenState(
                searchQuery = query,
                acknowledgements = ThirdPartyAcknowledgements.filter { it.matches(query) }.toPersistentList(),
                openSourceLicenses = openSourceLicenses.filter { it.matches(query) }.toPersistentList(),
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        AboutScreenState(ThirdPartyAcknowledgements, openSourceLicenses),
    )

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
