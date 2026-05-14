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
import chat.schildi.revenge.actions.LocalKeyboardActionProvider
import chat.schildi.revenge.actions.LocalListActionProvider
import chat.schildi.revenge.actions.LocalRoomContextSuggestionsProvider
import chat.schildi.revenge.actions.hierarchicalKeyboardActionProvider
import chat.schildi.revenge.compose.components.TopNavigation
import chat.schildi.revenge.compose.components.TopNavigationCloseOrNavigateToInboxIcon
import chat.schildi.revenge.compose.components.TopNavigationSearchOrTitle
import chat.schildi.revenge.compose.destination.SplashScreenContent
import chat.schildi.revenge.compose.focus.FocusContainer
import chat.schildi.revenge.compose.search.LocalSearchProvider
import chat.schildi.revenge.model.devtools.RoomDevToolsViewModel
import chat.schildi.revenge.publishTitle
import chat.schildi.revenge.viewModelKey
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.room_dev_tools_title

@Composable
fun RoomDevToolsScreen(
    destination: Destination.RoomDevTools,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
) {
    val viewModel: RoomDevToolsViewModel = viewModel(
        key = viewModelKey(destination),
        factory = RoomDevToolsViewModel.factory(destination.sessionId, destination.roomId),
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
        LocalKeyboardActionProvider provides viewModel.roomActionProvider.hierarchicalKeyboardActionProvider(),
        LocalRoomContextSuggestionsProvider provides viewModel.roomContextSuggestionsProvider,
        modifier = modifier,
        role = FocusRole.DESTINATION_ROOT_CONTAINER,
    ) {
        Column {
            TopNavigation {
                TopNavigationSearchOrTitle(viewModel.windowTitle.collectAsState(null).value?.render() ?: stringResource(Res.string.room_dev_tools_title))
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
