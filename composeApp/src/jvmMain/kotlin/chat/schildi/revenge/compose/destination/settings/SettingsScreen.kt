package chat.schildi.revenge.compose.destination.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import chat.schildi.revenge.Destination
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.ListAction
import chat.schildi.revenge.actions.LocalListActionProvider
import chat.schildi.revenge.compose.components.EmptyListScreen
import chat.schildi.revenge.compose.components.TopNavigation
import chat.schildi.revenge.compose.components.TopNavigationCloseOrNavigateToInboxIcon
import chat.schildi.revenge.compose.components.TopNavigationSearchOrTitle
import chat.schildi.revenge.compose.components.lookup
import chat.schildi.revenge.compose.focus.FocusContainer
import chat.schildi.revenge.compose.search.LocalSearchProvider
import chat.schildi.revenge.compose.util.toStringHolder
import chat.schildi.revenge.model.SettingsViewModel
import chat.schildi.revenge.publishTitle
import chat.schildi.revenge.viewModelKey
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.empty_screen_placeholder_unexpected
import shire.composeapp.generated.resources.hint_settings

@Composable
fun SettingsScreen(
    destination: Destination.Settings,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
) {
    val viewModel: SettingsViewModel = viewModel(
        key = viewModelKey(destination),
        factory = viewModelFactory { initializer { SettingsViewModel() } }
    )
    val stringLookup = viewModel.stringLookupRequest.lookup()
    LaunchedEffect(stringLookup) {
        viewModel.stringLookupTable = stringLookup
    }
    publishTitle(viewModel)
    val prefScreenState = viewModel.prefScreen.collectAsState().value
    val prefScreen = prefScreenState.prefScreen
    val listState = rememberLazyListState()
    val listAction = remember(listState) { ListAction(listState) }
    FocusContainer(
        LocalSearchProvider provides viewModel,
        LocalListActionProvider provides listAction,
        modifier = modifier,
        role = FocusRole.DESTINATION_ROOT_CONTAINER,
    ) {
        Column {
            TopNavigation {
                TopNavigationSearchOrTitle(stringResource(Res.string.hint_settings))
                TopNavigationCloseOrNavigateToInboxIcon()
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
                    ) {
                        renderPref(prefScreen)
                    }
                }
            }
        }
    }
}
