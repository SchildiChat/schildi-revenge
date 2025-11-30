package chat.schildi.revenge.compose.destination.conversation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import chat.schildi.revenge.model.ConversationViewModel
import chat.schildi.revenge.Destination
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.actions.LocalListActionProvider
import chat.schildi.revenge.compose.search.LocalSearchProvider
import chat.schildi.revenge.publishTitle
import kotlinx.collections.immutable.persistentListOf

@Composable
fun ChatScreen(destination: Destination.Conversation) {
    val viewModel: ConversationViewModel = viewModel(
        key = "${LocalDestinationState.current?.id}/${destination.sessionId}/${destination.roomId}",
        factory = ConversationViewModel.factory(destination.sessionId, destination.roomId)
    )
    val timelineItems = viewModel.timelineItems.collectAsState(persistentListOf()).value
    val forwardPaginationStatus = viewModel.forwardPaginationStatus.collectAsState(null).value
    val backwardPaginationStatus = viewModel.backwardPaginationStatus.collectAsState(null).value
    val listState = rememberLazyListState()

    publishTitle(viewModel)

    LaunchedEffect(listState, forwardPaginationStatus) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        }
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex != null && lastVisibleIndex >= timelineItems.size - 3 && forwardPaginationStatus?.canPaginate == true) {
                    viewModel.paginateForward()
                }
            }
    }
    LaunchedEffect(listState, backwardPaginationStatus) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index
        }
            .collect { firstVisibleIndex ->
                if (firstVisibleIndex != null && firstVisibleIndex <= 3 && backwardPaginationStatus?.canPaginate == true) {
                    viewModel.paginateBackward()
                }
            }
    }

    CompositionLocalProvider(
        //LocalSearchProvider provides viewModel, // TODO CV search
        LocalListActionProvider provides listState,
    ) {
        // Double reverse helps with stick-to-bottom while paging backwards or receiving messages
        LazyColumn(Modifier.fillMaxSize(), reverseLayout = true, state = listState) {
            items(timelineItems.reversed()) { item ->
                ConversationItemRow(item)
            }
        }
    }
}
