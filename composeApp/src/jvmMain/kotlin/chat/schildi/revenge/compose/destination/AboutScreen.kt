package chat.schildi.revenge.compose.destination

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import chat.schildi.revenge.BuildInfo
import chat.schildi.revenge.Destination
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.MatrixSdkMetadata
import chat.schildi.revenge.actions.ActionResult
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.InteractionAction
import chat.schildi.revenge.actions.ListActions
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.actions.LocalListActionProvider
import chat.schildi.revenge.actions.actionProvider
import chat.schildi.revenge.actions.plainTextCopyAction
import chat.schildi.revenge.compose.components.EmptyListScreen
import chat.schildi.revenge.compose.components.ExpandButton
import chat.schildi.revenge.compose.components.TopNavigation
import chat.schildi.revenge.compose.components.TopNavigationCloseOrNavigateToInboxIcon
import chat.schildi.revenge.compose.components.TopNavigationSearchOrTitle
import chat.schildi.revenge.compose.focus.FocusContainer
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.revenge.compose.search.LocalSearchProvider
import chat.schildi.revenge.compose.util.appendUrlText
import chat.schildi.revenge.glue.platformVersionCode
import chat.schildi.revenge.glue.platformVersionName
import chat.schildi.resources.toStringHolder
import chat.schildi.revenge.model.about.AboutViewModel
import chat.schildi.revenge.model.about.AppLink
import chat.schildi.revenge.model.about.AppLinks
import chat.schildi.revenge.model.about.DependencyInfo
import chat.schildi.revenge.model.about.REVENGE_SDK_SOURCE_URL
import chat.schildi.revenge.model.about.REVENGE_SOURCE_URL
import chat.schildi.revenge.model.about.SCHILDI_NEXT_SOURCE_URL
import chat.schildi.revenge.model.about.ThirdPartyAcknowledgement
import chat.schildi.revenge.util.SystemInfo
import chat.schildi.revenge.viewModelKey
import chat.schildi.theme.scExposures
import chat.schildi.theme.scLinkStyle
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import shire.res.generated.resources.Res
import shire.res.generated.resources.about
import shire.res.generated.resources.about_build_date
import shire.res.generated.resources.about_build_info
import shire.res.generated.resources.about_java_runtime
import shire.res.generated.resources.about_java_vm
import shire.res.generated.resources.about_kotlin_base_revision
import shire.res.generated.resources.about_open_source_licenses
import shire.res.generated.resources.about_os_name
import shire.res.generated.resources.about_release_variant
import shire.res.generated.resources.about_revision
import shire.res.generated.resources.about_rust_release_variant
import shire.res.generated.resources.about_rust_revision
import shire.res.generated.resources.about_system_info
import shire.res.generated.resources.about_version_code
import shire.res.generated.resources.about_version_name
import shire.res.generated.resources.action_show_less
import shire.res.generated.resources.action_show_more
import shire.res.generated.resources.app_title_full
import shire.res.generated.resources.empty_screen_placeholder_unexpected
import shire.res.generated.resources.hint_app_icon
import shire.res.generated.resources.ic_launcher

@Composable
fun AboutScreen(
    destination: Destination.About,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
) {
    val viewModel: AboutViewModel = viewModel(
        key = viewModelKey(destination),
        factory = viewModelFactory { initializer { AboutViewModel() } }
    )
    val state = viewModel.state.collectAsState().value
    val isSearching = state.isSearching
    val listState = rememberLazyListState()
    val listAction = remember(listState) { ListActions(listState) }
    FocusContainer(
        LocalSearchProvider provides viewModel,
        LocalListActionProvider provides listAction,
        modifier = modifier.windowInsetsPadding(
            WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        ),
        role = FocusRole.DESTINATION_ROOT_CONTAINER,
    ) {
        var expandOpenSourceLicenses by remember { mutableStateOf(false) }
        Column {
            TopNavigation {
                TopNavigationSearchOrTitle(stringResource(Res.string.about))
                TopNavigationCloseOrNavigateToInboxIcon()
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (state.isEmpty) {
                    EmptyListScreen(
                        title = Res.string.empty_screen_placeholder_unexpected.toStringHolder(),
                        icon = rememberVectorPainter(Icons.Default.Info),
                        renderedSearchTerm = state.searchQuery,
                        modifier = contentModifier.fillMaxSize(),
                    )
                } else {
                    LazyColumn(
                        modifier = contentModifier.padding(horizontal = Dimens.windowPadding),
                        verticalArrangement = Dimens.verticalArrangement,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        state = listState,
                        contentPadding = PaddingValues(vertical = Dimens.windowPadding),
                    ) {
                        if (!isSearching) {
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
                                        )
                                    }
                                }
                            }
                        }

                        item(key = "section_open_source_licenses") {
                            AboutSectionHeader(stringResource(Res.string.about_open_source_licenses))
                        }
                        items(state.acknowledgements, key = { it.name }) { item ->
                            AcknowledgementItem(
                                item,
                            )
                        }
                        if (!isSearching) {
                            item(key = "expand_licenses") {
                                ExpandButton(
                                    text = if (expandOpenSourceLicenses) {
                                        stringResource(Res.string.action_show_less)
                                    } else {
                                        stringResource(Res.string.action_show_more)
                                    },
                                    expanded = expandOpenSourceLicenses,
                                    enabledColor = MaterialTheme.scExposures.linkColor,
                                ) {
                                    expandOpenSourceLicenses = !expandOpenSourceLicenses
                                }
                            }
                        }
                        if (expandOpenSourceLicenses || isSearching) {
                            items(state.openSourceLicenses, key = { it.name }) { item ->
                                DependencyItem(
                                    item,
                                )
                            }
                        }

                        if (!isSearching) {

                            item(key = "build_info") {
                                AboutSectionHeader(stringResource(Res.string.about_build_info))
                            }
                            buildInfoItem("version_name") {
                                stringResource(Res.string.about_version_name, platformVersionName)
                            }
                            platformVersionCode?.let { versionCode ->
                                buildInfoItem("version_code") {
                                    stringResource(Res.string.about_version_code, versionCode)
                                }
                            }
                            buildInfoItem("release_variant") {
                                stringResource(Res.string.about_release_variant, BuildInfo.BUILD_TYPE)
                            }
                            if (BuildInfo.RUST_PROFILE != BuildInfo.BUILD_TYPE) {
                                buildInfoItem("rust_release_variant") {
                                    stringResource(Res.string.about_rust_release_variant, BuildInfo.RUST_PROFILE)
                                }
                            }
                            buildInfoItem(
                                "revision",
                                action = InteractionAction.OpenInBrowser("$REVENGE_SOURCE_URL/commits/${BuildInfo.SOURCE_REVISION}")
                            ) {
                                stringResource(Res.string.about_revision, BuildInfo.SOURCE_REVISION.formatCommitHash())
                            }
                            buildInfoItem(
                                "kotlin_sdk_revision",
                                action = InteractionAction.OpenInBrowser("$SCHILDI_NEXT_SOURCE_URL/commits/${MatrixSdkMetadata.SCHILDI_NEXT_REVISION}")
                            ) {
                                stringResource(Res.string.about_kotlin_base_revision, MatrixSdkMetadata.ELEMENT_VERSION)
                            }
                            buildInfoItem(
                                "rust_sdk_revision",
                                action = InteractionAction.OpenInBrowser("$REVENGE_SDK_SOURCE_URL/commits/${BuildInfo.SDK_REVISION}")
                            ) {
                                stringResource(
                                    Res.string.about_rust_revision,
                                    BuildInfo.SDK_REVISION.formatCommitHash()
                                )
                            }
                            buildInfoItem("build_timestamp") {
                                stringResource(Res.string.about_build_date, BuildInfo.BUILD_TIMESTAMP)
                            }

                            item(key = "system_info") {
                                AboutSectionHeader(stringResource(Res.string.about_system_info))
                            }
                            buildInfoItem("os_name") {
                                stringResource(
                                    Res.string.about_os_name,
                                    remember { SystemInfo.getOsName() },
                                )
                            }
                            buildInfoItem("java_runtime") {
                                stringResource(
                                    Res.string.about_java_runtime,
                                    remember { SystemInfo.javaRuntime() },
                                )
                            }
                            buildInfoItem("java_vm") {
                                stringResource(
                                    Res.string.about_java_vm,
                                    remember { SystemInfo.javaVm() },
                                )
                            }
                        }
                        item(key = "navigation_bar_spacer") {
                            Spacer(
                                Modifier.windowInsetsBottomHeight(
                                    WindowInsets.navigationBars.only(WindowInsetsSides.Bottom),
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun String.formatCommitHash() = take(12)

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
                stringResource(Res.string.app_title_full),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.alignByBaseline(),
            )

            Text(
                platformVersionName,
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
    val keyHandler = LocalKeyboardActionHandler.current
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
    AboutCard(
        modifier.keyFocusable(
            role = FocusRole.LIST_ITEM,
            actionProvider = actionProvider(
                primaryAction = InteractionAction.Invoke {
                    keyHandler.openLink(item.url) is ActionResult.Success
                },
                copyActions = plainTextCopyAction { text.toString() },
            ),
        ).fillMaxWidth()
    ) {
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
private fun DependencyItem(item: DependencyInfo, modifier: Modifier = Modifier) {
    val keyHandler = LocalKeyboardActionHandler.current
    val linkStyle = scLinkStyle()
    val text = remember(item) {
        buildAnnotatedString {
            appendUrlText(item.url, item.name, linkStyle)
            if (item.license != null) {
                append(" under the terms of ")
                appendUrlText(item.licenseUrl, item.license, linkStyle)
            }
        }
    }
    val primaryUrl = item.url ?: item.licenseUrl
    AboutCard(
        modifier.keyFocusable(
            role = FocusRole.LIST_ITEM,
            actionProvider = actionProvider(
                primaryAction = if (primaryUrl == null) null else InteractionAction.Invoke {
                    keyHandler.openLink(primaryUrl) is ActionResult.Success
                },
                copyActions = plainTextCopyAction { text.toString() },
            ),
        ).fillMaxWidth()
    ) {
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
    val keyHandler = LocalKeyboardActionHandler.current
    AboutCard(
        modifier.keyFocusable(
            actionProvider = actionProvider(
                primaryAction = InteractionAction.Invoke {
                    keyHandler.openLink(item.url) is ActionResult.Success
                },
                copyActions = plainTextCopyAction { text.toString() },
            )
        )
    ) {
        SelectionContainer {
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(16.dp)
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
                        role = FocusRole.LIST_ITEM,
                        actionProvider = actionProvider(
                            primaryAction = action ?: copyAction,
                            secondaryAction = copyAction,
                            copyActions = plainTextCopyAction { text },
                        ),
                    ),
                )
            }
        }
    }
}
