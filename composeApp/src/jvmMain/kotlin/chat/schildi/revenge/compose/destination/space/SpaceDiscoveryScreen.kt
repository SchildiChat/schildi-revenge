package chat.schildi.revenge.compose.destination.space

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.lifecycle.viewmodel.compose.viewModel
import chat.schildi.lib.preferences.ScPrefs
import chat.schildi.revenge.Destination
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.LocalListActionProvider
import chat.schildi.revenge.actions.rememberListActions
import chat.schildi.revenge.compose.components.EmptyListScreen
import chat.schildi.revenge.compose.destination.conversation.virtual.PagingIndicator
import chat.schildi.revenge.compose.destination.split.requireSinglePaneLayout
import chat.schildi.revenge.compose.focus.FocusContainer
import chat.schildi.revenge.compose.search.LocalSearchProvider
import chat.schildi.revenge.config.keybindings.DestinationEnum
import chat.schildi.revenge.model.SpaceDiscoveryViewModel
import chat.schildi.revenge.publishTitle
import chat.schildi.revenge.viewModelKey
import chat.schildi.resources.toStringHolder
import io.element.android.libraries.matrix.api.spaces.SpaceRoomList
import shire.res.generated.resources.Res
import shire.res.generated.resources.space_discovery_empty

@Composable
fun SpaceDiscoveryScreen(
    destination: Destination.SpaceDiscovery,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
) {
    if (requireSinglePaneLayout(DestinationEnum.SpaceDiscoveryDetailsSplit, ScPrefs.PREFER_CONVERSATION_DETAILS_SPLIT) { discovery ->
        Destination.SpaceDiscoveryDetailsMultiPane(discovery)
    }) {
        return
    }

    val viewModel: SpaceDiscoveryViewModel = viewModel(
        key = viewModelKey(destination),
        factory = SpaceDiscoveryViewModel.factory(destination.sessionId, destination.roomId),
    )
    publishTitle(viewModel)

    val roomsState = viewModel.filteredSpaceRooms.collectAsState(null).value
    val spaceRooms = roomsState?.rooms
    val isLoading = when (val paginationStatus = viewModel.paginationStatus.collectAsState(null).value) {
        is SpaceRoomList.PaginationStatus.Idle -> paginationStatus.hasMoreToLoad
        SpaceRoomList.PaginationStatus.Loading -> true
        null -> true
    }

    val listState = key(roomsState?.searchTerm) {
        rememberLazyListState()
    }

    FocusContainer(
        LocalSearchProvider provides viewModel,
        LocalListActionProvider provides rememberListActions(listState),
        modifier = modifier.windowInsetsPadding(
            WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        ),
        role = FocusRole.DESTINATION_ROOT_CONTAINER,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val compact = maxWidth < Dimens.compactActionBarThreshold
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                SpaceDiscoveryTopNavigation(viewModel, compact)

                when {
                    spaceRooms.isNullOrEmpty() -> EmptyListScreen(
                        title = Res.string.space_discovery_empty.toStringHolder(),
                        icon = rememberVectorPainter(Icons.Default.TravelExplore),
                        renderedSearchTerm = roomsState?.searchTerm,
                        isLoading = roomsState == null || isLoading,
                        loadState = viewModel.loadState,
                        modifier = contentModifier.fillMaxSize(),
                    )
                    else -> LazyColumn(
                        modifier = contentModifier.fillMaxSize(),
                        state = listState,
                        contentPadding = WindowInsets.navigationBars
                            .only(WindowInsetsSides.Bottom)
                            .asPaddingValues(),
                    ) {
                        items(spaceRooms, key = { it.roomId }) { room ->
                            SpaceDiscoveryRow(viewModel, room)
                        }
                        if (isLoading) {
                            item {
                                PagingIndicator(
                                    Modifier.padding(vertical = Dimens.windowPadding),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
