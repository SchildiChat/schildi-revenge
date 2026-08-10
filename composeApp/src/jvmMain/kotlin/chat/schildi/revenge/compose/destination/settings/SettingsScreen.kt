package chat.schildi.revenge.compose.destination.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import chat.schildi.lib.preferences.ScPrefs
import chat.schildi.revenge.preferences.value
import chat.schildi.revenge.Destination
import chat.schildi.revenge.DestinationStateHolder
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.ListActions
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.actions.LocalListActionProvider
import chat.schildi.revenge.compose.components.EmptyListScreen
import chat.schildi.revenge.compose.components.TopNavigation
import chat.schildi.revenge.compose.components.TopNavigationCloseOrNavigateToInboxIcon
import chat.schildi.revenge.compose.components.TopNavigationIcon
import chat.schildi.revenge.compose.components.TopNavigationSearchOrTitle
import chat.schildi.revenge.compose.components.TopNavigationTitle
import chat.schildi.revenge.compose.destination.split.MultiPaneLayout
import chat.schildi.revenge.compose.destination.split.buildMultiPaneDestinationStateHolderWrapper
import chat.schildi.revenge.compose.focus.FocusContainer
import chat.schildi.revenge.compose.search.LocalSearchProvider
import chat.schildi.revenge.compose.search.SearchProvider
import chat.schildi.resources.toStringHolder
import chat.schildi.revenge.compose.components.PlatformBackHandler
import chat.schildi.revenge.config.keybindings.DestinationEnum
import chat.schildi.revenge.model.SettingsViewModel
import chat.schildi.revenge.publishTitle
import chat.schildi.revenge.viewModelKey
import org.jetbrains.compose.resources.stringResource
import shire.res.generated.resources.Res
import shire.res.generated.resources.about
import shire.res.generated.resources.action_close
import shire.res.generated.resources.diagnostics
import shire.res.generated.resources.empty_screen_placeholder_unexpected
import shire.res.generated.resources.manage_accounts

private val LocalRootPreferenceViewModel = compositionLocalOf<MutableState<SettingsViewModel?>?> { null }

/**
 * Multi-pane settings.
 */
@Composable
fun SettingsScreen(
    destination: Destination.Settings,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
) {
    FocusContainer(
        LocalRootPreferenceViewModel provides remember { mutableStateOf(null) },
        role = FocusRole.NESTING_DESTINATION_ROOT_CONTAINER,
        modifier = modifier,
    ) {
        BoxWithConstraints {
            val minSplitWidth = ScPrefs.INBOX_CONVERSATION_SPLIT_MIN_WIDTH.value().dp
            val collapseSinglePane = maxWidth < minSplitWidth
            val hasDetails =
                destination.details.state.collectAsState().value.destination !is Destination.MultiPaneSettingsPlaceholder
            val rootViewModel = LocalRootPreferenceViewModel.current?.value
            val isSearching = if (rootViewModel == null) {
                false
            } else {
                LocalKeyboardActionHandler.current.searchQueryForDestination(rootViewModel)
                    .collectAsState(null).value != null
            }
            PlatformBackHandler(enabled = hasDetails) {
                destination.details.navigate(Destination.MultiPaneSettingsPlaceholder)
            }
            MultiPaneLayout(
                outerDestination = destination.destinationId,
                innerDestinations = listOfNotNull(
                    if (collapseSinglePane && hasDetails && !isSearching)
                        null
                    else
                        destination.root.wrapped(destination, false),
                    if (collapseSinglePane && !hasDetails || isSearching)
                        null
                    else
                        destination.details.wrapped(destination, true),
                ),
                contentModifier = contentModifier,
            )
        }
    }
}

/**
 * Single-pane settings.
 */
@Composable
fun SettingsScreen(
    destination: Destination.SettingsPane,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
) {
    val viewModel: SettingsViewModel = viewModel(
        key = viewModelKey(destination),
        factory = viewModelFactory { initializer { SettingsViewModel(destination.rootPreferenceCategory) } }
    )
    val searchUpstream = LocalRootPreferenceViewModel.current
    LaunchedEffect(viewModel, searchUpstream) {
        if (searchUpstream != null && viewModel.isRootPreferences) {
            searchUpstream.value = viewModel
        }
    }
    publishTitle(viewModel)
    val prefScreenState = viewModel.prefScreen.collectAsState().value
    val prefScreen = prefScreenState.prefScreen
    val listState = rememberLazyListState()
    val listAction = remember(listState) { ListActions(listState) }
    FocusContainer(
        LocalSearchProvider provides (viewModel.takeIf { it.isRootPreferences } as? SearchProvider ?: LocalSearchProvider.current),
        LocalListActionProvider provides listAction,
        modifier = modifier.windowInsetsPadding(
            WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        ),
        role = FocusRole.DESTINATION_ROOT_CONTAINER,
    ) {
        Column {
            TopNavigation {
                val destinationState = LocalDestinationState.current
                if (viewModel.isRootPreferences) {
                    TopNavigationSearchOrTitle(stringResource(viewModel.prefScreen.collectAsState().value.prefScreen.titleRes))
                    TopNavigationIcon(
                        Icons.Default.Info,
                        stringResource(Res.string.about)
                    ) {
                        destinationState?.navigate(Destination.About)
                    }
                    TopNavigationIcon(
                        Icons.Default.AccountCircle,
                        stringResource(Res.string.manage_accounts)
                    ) {
                        destinationState?.navigate(Destination.AccountManagement())
                    }
                    TopNavigationCloseOrNavigateToInboxIcon()
                } else {
                    TopNavigationTitle(stringResource(viewModel.prefScreen.collectAsState().value.prefScreen.titleRes))
                    if (prefScreen.sKey == ScPrefs.devPrefs.sKey) {
                        TopNavigationIcon(
                            Icons.Default.BugReport,
                            stringResource(Res.string.diagnostics),
                        ) {
                            destinationState?.navigate(Destination.Diagnostics)
                        }
                    }
                    TopNavigationIcon(
                        Icons.Default.Close,
                        stringResource(Res.string.action_close),
                    ) {
                        destinationState?.navigate(Destination.SettingsPane(viewModel.parentPreferenceKey))
                    }
                }
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (prefScreen.prefs.isEmpty()) {
                    EmptyListScreen(
                        title = Res.string.empty_screen_placeholder_unexpected.toStringHolder(),
                        icon = rememberVectorPainter(Icons.Default.BugReport),
                        renderedSearchTerm = prefScreenState.searchQuery,
                    )
                } else {
                    LazyColumn(
                        modifier = contentModifier.padding(horizontal = Dimens.windowPadding),
                        verticalArrangement = Dimens.verticalArrangement,
                        state = listState,
                        contentPadding = WindowInsets.navigationBars
                            .only(WindowInsetsSides.Bottom)
                            .asPaddingValues(),
                    ) {
                        renderPref(
                            prefScreen,
                            renderPrefScreenInline = prefScreenState.searchQuery != null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DestinationStateHolder.wrapped(
    destination: Destination.Settings,
    isDetails: Boolean,
    parent: DestinationStateHolder? = LocalDestinationState.current,
) = remember(destination, isDetails, parent) {
    buildMultiPaneDestinationStateHolderWrapper(
        parent = parent,
        inner = this,
        isDetails = isDetails,
        accessMain = { destination.root },
        accessDetails = { destination.details },
        createPlaceholder = { Destination.MultiPaneSettingsPlaceholder },
        mainDestination = DestinationEnum.SettingsRoot,
        allowedDetailsDestinations = listOf(
            DestinationEnum.SettingsDetails,
            DestinationEnum.SplitSettingsDetailsPlaceholder,
            DestinationEnum.AccountManagement,
        ),
        allowedDetailsCategories = listOf(),
    )
}
