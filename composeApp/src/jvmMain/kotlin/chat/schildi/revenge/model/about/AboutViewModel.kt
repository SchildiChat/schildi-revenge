package chat.schildi.revenge.model.about

import androidx.lifecycle.ViewModel
import co.touchlab.kermit.Logger
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.serialization.json.Json

data class DependencyInfo(
    val name: String,
    val url: String? = null,
    val license: String? = null,
    val licenseUrl: String? = null,
)

class AboutViewModel : ViewModel() {
    private val log = Logger.withTag("About")

    private val json = Json { ignoreUnknownKeys = true }

    val openSourceLicenses = run {
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
}
