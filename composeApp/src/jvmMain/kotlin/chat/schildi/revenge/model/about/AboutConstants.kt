package chat.schildi.revenge.model.about

import chat.schildi.revenge.compose.util.ComposableStringHolder
import chat.schildi.revenge.compose.util.toStringHolder
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.about_privacy_policy
import shire.composeapp.generated.resources.about_source_code
import shire.composeapp.generated.resources.about_website

const val REVENGE_SOURCE_URL = "https://github.com/SchildiChat/schildi-revenge"
const val REVENGE_SDK_SOURCE_URL = "https://github.com/SchildiChat/matrix-rust-sdk"
const val SCHILDI_NEXT_SOURCE_URL = "https://github.com/SchildiChat/schildichat-android-next"

data class ThirdPartyAcknowledgement(
    val name: String,
    val nameAdd: String? = null,
    val url: String,
    val author: String,
    val authorUrl: String?,
    val license: String,
    val licenseUrl: String,
)

data class AppLink(
    val name: ComposableStringHolder,
    val url: String,
)

val ThirdPartyAcknowledgements = listOf(
    ThirdPartyAcknowledgement(
        name = "Matrix Rust SDK",
        url = "https://github.com/matrix-org/matrix-rust-sdk",
        author = "The Matrix.org Foundation C.I.C.",
        authorUrl = "https://matrix.org/",
        license = "Apache-2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0.txt",
    ),
    ThirdPartyAcknowledgement(
        name = "Element X Android",
        url = "https://github.com/element-hq/element-x-android/",
        author = "Element Creations Ltd.",
        authorUrl = "https://element.io",
        license = "AGPL-3.0",
        licenseUrl = "https://www.gnu.org/licenses/agpl-3.0.txt",
    ),
    ThirdPartyAcknowledgement(
        name = "matrix-messageformat-compose",
        url = "https://github.com/beeper/matrix-messageformat-compose",
        author = "Beeper (Automattic)",
        authorUrl = "https://www.beeper.com/",
        license = "MIT",
        licenseUrl = "https://mit-license.org/",
    ),
    ThirdPartyAcknowledgement(
        name = "tortoise",
        url = "https://pictogrammers.com/library/mdi/icon/tortoise/",
        author = "Nick",
        authorUrl = "https://github.com/Croutonix",
        license = "Apache-2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0.txt",
    ),
    ThirdPartyAcknowledgement(
        name = "Inter",
        nameAdd = "font",
        url = "https://fonts.google.com/specimen/Inter",
        author = "Rasmus Andersson",
        authorUrl = null,
        license = "OFL-1.1",
        licenseUrl = "https://fonts.google.com/specimen/Inter/license",
    ),
    ThirdPartyAcknowledgement(
        name = "Noto Color Emoji",
        nameAdd = "font",
        url = "https://fonts.google.com/noto/specimen/Noto+Color+Emoji",
        author = "Google Inc.",
        authorUrl = null,
        license = "OFL-1.1",
        licenseUrl = "https://fonts.google.com/noto/specimen/Noto+Color+Emoji/license",
    ),
)

val AppLinks = listOf(
    AppLink(
        name = Res.string.about_website.toStringHolder(),
        url = "https://schildi.chat/revenge",
    ),
    AppLink(
        name = Res.string.about_privacy_policy.toStringHolder(),
        url = "https://schildi.chat/revenge/privacy/",
    ),
    AppLink(
        name = Res.string.about_source_code.toStringHolder(),
        url = REVENGE_SOURCE_URL,
    ),
)
