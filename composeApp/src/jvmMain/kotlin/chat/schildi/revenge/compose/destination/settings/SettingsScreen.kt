package chat.schildi.revenge.compose.destination.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import chat.schildi.preferences.ScPrefs
import chat.schildi.preferences.value
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.ListAction
import chat.schildi.revenge.actions.LocalListActionProvider
import chat.schildi.revenge.compose.components.TopNavigation
import chat.schildi.revenge.compose.components.TopNavigationCloseOrNavigateToInboxIcon
import chat.schildi.revenge.compose.components.TopNavigationSearchOrTitle
import chat.schildi.revenge.compose.components.lookup
import chat.schildi.revenge.compose.focus.FocusContainer
import chat.schildi.revenge.compose.search.LocalSearchProvider
import chat.schildi.revenge.model.SettingsViewModel
import chat.schildi.revenge.publishTitle
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.hint_settings

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val viewModel: SettingsViewModel = viewModel()
    val stringLookup = viewModel.stringLookupRequest.lookup()
    LaunchedEffect(stringLookup) {
        viewModel.stringLookupTable = stringLookup
    }
    publishTitle(viewModel)
    val prefScreen = viewModel.prefScreen.collectAsState().value
    val listState = rememberLazyListState()
    val listAction = remember(listState) { ListAction(listState) }
    FocusContainer(
        LocalSearchProvider provides viewModel,
        LocalListActionProvider provides listAction,
        modifier = modifier,
        role = FocusRole.DESTINATION_ROOT_CONTAINER,
    ) {
        Column(Modifier.widthIn(max = ScPrefs.MAX_WIDTH_SETTINGS.value().dp)) {
            TopNavigation {
                TopNavigationSearchOrTitle(stringResource(Res.string.hint_settings))
                TopNavigationCloseOrNavigateToInboxIcon()
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                // TODO empty search results content?
                LazyColumn(
                    modifier = Modifier.padding(horizontal = Dimens.windowPadding),
                    verticalArrangement = Dimens.verticalArrangement,
                    state = listState,
                ) {
                    renderPref(prefScreen)
                }
            }
        }
    }
}
