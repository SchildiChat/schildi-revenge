package chat.schildi.revenge.compose.destination.devtools

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import chat.schildi.revenge.Destination
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.ListActions
import chat.schildi.revenge.actions.LocalListActionProvider
import chat.schildi.revenge.compose.components.TopNavigation
import chat.schildi.revenge.compose.components.TopNavigationCloseOrNavigateToInboxIcon
import chat.schildi.revenge.compose.components.TopNavigationSearchOrTitle
import chat.schildi.revenge.compose.destination.SplashScreenContent
import chat.schildi.revenge.compose.focus.FocusContainer
import chat.schildi.revenge.compose.search.LocalSearchProvider
import chat.schildi.revenge.model.devtools.AccountDevToolsViewModel
import chat.schildi.revenge.publishTitle
import chat.schildi.revenge.viewModelKey
import org.jetbrains.compose.resources.stringResource
import shire.res.generated.resources.Res
import shire.res.generated.resources.account_dev_tools_title

@Composable
fun AccountDevToolsScreen(
    destination: Destination.AccountDevTools,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
) {
    val viewModel: AccountDevToolsViewModel = viewModel(
        key = viewModelKey(destination),
        factory = AccountDevToolsViewModel.factory(destination.sessionId),
    )
    publishTitle(viewModel)

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    val listState = rememberLazyListState()
    val listAction = remember(listState) { ListActions(listState) }

    val sectionedList = viewModel.sectionedList.collectAsState().value

    FocusContainer(
        LocalSearchProvider provides viewModel,
        LocalListActionProvider provides listAction,
        modifier = modifier,
        role = FocusRole.DESTINATION_ROOT_CONTAINER,
    ) {
        Column {
            TopNavigation {
                TopNavigationSearchOrTitle(viewModel.windowTitle.collectAsState(null).value?.render() ?: stringResource(Res.string.account_dev_tools_title))
                TopNavigationCloseOrNavigateToInboxIcon()
            }

            BoxWithConstraints(contentModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (sectionedList == null) {
                    SplashScreenContent(Modifier.padding(Dimens.windowPadding), viewModel.loadState)
                    return@BoxWithConstraints
                }

                DevToolsEventList(
                    sections = sectionedList,
                    listState = listState,
                    persist = viewModel::persist,
                    modifier = modifier.padding(horizontal = Dimens.windowPadding),
                    maxEditItemHeight = maxHeight - Dimens.windowPadding * 4,
                )
            }
        }
    }
}
