package chat.schildi.revenge.compose.destination

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import chat.schildi.revenge.BuildInfo
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.ActionResult
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.InteractionAction
import chat.schildi.revenge.actions.ListAction
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.actions.LocalListActionProvider
import chat.schildi.revenge.actions.actionProvider
import chat.schildi.revenge.compose.components.TopNavigation
import chat.schildi.revenge.compose.components.TopNavigationCloseOrNavigateToInboxIcon
import chat.schildi.revenge.compose.components.TopNavigationTitle
import chat.schildi.revenge.compose.focus.FocusContainer
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.revenge.compose.util.ComposableStringHolder
import chat.schildi.revenge.compose.util.appendUrlText
import chat.schildi.revenge.compose.util.toStringHolder
import chat.schildi.theme.scLinkStyle
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.about
import shire.composeapp.generated.resources.about_build_date
import shire.composeapp.generated.resources.about_build_info
import shire.composeapp.generated.resources.about_credits
import shire.composeapp.generated.resources.about_privacy_policy
import shire.composeapp.generated.resources.about_release_variant
import shire.composeapp.generated.resources.about_revision
import shire.composeapp.generated.resources.about_rust_release_variant
import shire.composeapp.generated.resources.about_rust_revision
import shire.composeapp.generated.resources.about_source_code
import shire.composeapp.generated.resources.about_website
import shire.composeapp.generated.resources.app_title
import shire.composeapp.generated.resources.hint_app_icon
import shire.composeapp.generated.resources.ic_launcher

private const val REVENGE_SOURCE_URL = "https://github.com/SchildiChat/schildi-revenge"
private const val REVENGE_SDK_SOURCE_URL = "https://github.com/SchildiChat/matrix-rust-sdk"

private data class ThirdPartyAcknowledgement(
    val name: String,
    val nameAdd: String? = null,
    val url: String,
    val author: String,
    val authorUrl: String?,
    val license: String,
    val licenseUrl: String,
)

private data class AppLink(
    val name: ComposableStringHolder,
    val url: String,
)

private val ThirdPartyAcknowledgements = listOf(
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

private val AppLinks = listOf(
    AppLink(
        name = Res.string.about_website.toStringHolder(),
        url = "https://schildi.chat/",
    ),
    AppLink(
        name = Res.string.about_privacy_policy.toStringHolder(),
        url = "https://schildi.chat/desktop/privacy/",
    ),
    AppLink(
        name = Res.string.about_source_code.toStringHolder(),
        url = REVENGE_SOURCE_URL,
    ),
)

@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    val listAction = remember(listState) { ListAction(listState, isReverseList = true) }
    val keyHandler = LocalKeyboardActionHandler.current
    FocusContainer(
        LocalListActionProvider provides listAction,
        modifier = modifier,
        role = FocusRole.DESTINATION_ROOT_CONTAINER,
    ) {
        Column {
            TopNavigation {
                TopNavigationTitle(stringResource(Res.string.about))
                TopNavigationCloseOrNavigateToInboxIcon()
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = Dimens.windowPadding),
                    verticalArrangement = Dimens.verticalArrangement,
                    state = listState,
                ) {
                    item(key = "header") {
                        AboutHeader()
                    }
                    item(key = "links") {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(
                                Dimens.horizontalItemPadding,
                                Alignment.CenterHorizontally
                            ),
                            verticalArrangement = Dimens.verticalArrangement,
                        ) {
                            AppLinks.forEach { item ->
                                AppLinkItem(
                                    item,
                                    Modifier.keyFocusable(
                                        actionProvider = actionProvider(
                                            primaryAction = InteractionAction.Invoke {
                                                keyHandler.openLinkInExternalBrowser(item.url) is ActionResult.Success
                                            }
                                        )
                                    )
                                )
                            }
                        }
                    }
                    item(key = "section_credits") {
                        AboutSectionHeader(stringResource(Res.string.about_credits))
                    }
                    items(ThirdPartyAcknowledgements, key = { it.name }) { item ->
                        AcknowledgementItem(
                            item,
                            Modifier.keyFocusable(
                                actionProvider = actionProvider(
                                    primaryAction = InteractionAction.Invoke {
                                        keyHandler.openLinkInExternalBrowser(item.url) is ActionResult.Success
                                    }
                                )
                            )
                        )
                    }
                    item(key = "build_info") {
                        AboutSectionHeader(stringResource(Res.string.about_build_info))
                    }
                    buildInfoItem("release_variant") {
                        stringResource(Res.string.about_release_variant, BuildInfo.BUILD_TYPE)
                    }
                    buildInfoItem("rust_release_variant") {
                        stringResource(Res.string.about_rust_release_variant, BuildInfo.RUST_PROFILE)
                    }
                    buildInfoItem("revision", action = InteractionAction.OpenInBrowser("$REVENGE_SOURCE_URL/commits/${BuildInfo.SOURCE_REVISION}")) {
                        stringResource(Res.string.about_revision, BuildInfo.SOURCE_REVISION.take(12))
                    }
                    buildInfoItem("rust_revision", action = InteractionAction.OpenInBrowser("$REVENGE_SDK_SOURCE_URL/commits/${BuildInfo.SDK_REVISION}")) {
                        stringResource(Res.string.about_rust_revision, BuildInfo.SDK_REVISION.take(12))
                    }
                    buildInfoItem("build_timestamp") {
                        stringResource(Res.string.about_build_date, BuildInfo.BUILD_TIMESTAMP)
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutHeader(modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth(),
        verticalArrangement = Dimens.verticalArrangement,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painterResource(Res.drawable.ic_launcher),
            stringResource(Res.string.hint_app_icon)
        )

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Dimens.horizontalArrangement,
        ) {
            Text(
                stringResource(Res.string.app_title),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.alignByBaseline(),
            )

            val appVersion: String = System.getProperty("jpackage.app-version") ?: "0.0.0-dev"
            Text(
                appVersion,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.alignByBaseline(),
            )
        }
    }
}

@Composable
private fun AboutSectionHeader(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(top = Dimens.listPadding), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.headlineLarge,
        )
    }
}

@Composable
fun AboutCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Card {
            content()
        }
    }
}

@Composable
private fun AcknowledgementItem(item: ThirdPartyAcknowledgement, modifier: Modifier = Modifier) {
    val linkStyle = scLinkStyle()
    val text = remember(item) {
        buildAnnotatedString {
            appendUrlText(item.url, item.name, linkStyle)
            if (item.nameAdd != null) {
                append(" ")
                append(item.nameAdd)
            }
            append(" by ")
            appendUrlText(item.authorUrl, item.author, linkStyle)
            append(" under the terms of ")
            appendUrlText(item.licenseUrl, item.license, linkStyle)
        }
    }
    AboutCard(modifier.fillMaxWidth()) {
        SelectionContainer {
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
private fun AppLinkItem(item: AppLink, modifier: Modifier = Modifier) {
    val linkStyle = scLinkStyle()
    val itemText = item.name.render()
    val text = remember(item) {
        buildAnnotatedString {
            appendUrlText(item.url, itemText, linkStyle)
        }
    }
    AboutCard(modifier) {
        SelectionContainer {
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

private fun LazyListScope.buildInfoItem(
    key: String,
    modifier: Modifier = Modifier,
    action: InteractionAction? = null,
    text: @Composable () -> String,
) {
    item(key = key) {
        val text = text()
        Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val copyAction = InteractionAction.CopyToClipboard(text)
            SelectionContainer {
                Text(
                    text,
                    Modifier.keyFocusable(
                        actionProvider = actionProvider(
                            primaryAction = action ?: copyAction,
                            secondaryAction = copyAction,
                        )
                    ),
                )
            }
        }
    }
}
