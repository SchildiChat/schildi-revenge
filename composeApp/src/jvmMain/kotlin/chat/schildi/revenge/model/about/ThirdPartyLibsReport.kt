package chat.schildi.revenge.model.about

import kotlinx.serialization.Serializable
import kotlin.String

@Serializable
data class ThirdPartyLibsReport(
    val dependencies: List<ThirdPartyDependency>
)

@Serializable
data class ThirdPartyDependency(
    val moduleName: String,
    val moduleUrl: String? = null,
    val moduleVersion: String,
    val moduleLicense: String? = null,
    val moduleLicenseUrl: String? = null,
)
