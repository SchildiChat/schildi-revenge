package chat.schildi.revenge.compose.destination.conversation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import chat.schildi.preferences.ScPrefs
import chat.schildi.preferences.value
import chat.schildi.revenge.Anim
import chat.schildi.revenge.model.ConversationViewModel
import chat.schildi.revenge.Destination
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.ListAction
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.actions.LocalKeyboardActionProvider
import chat.schildi.revenge.actions.LocalListActionProvider
import chat.schildi.revenge.actions.hierarchicalKeyboardActionProvider
import chat.schildi.revenge.compose.composer.ComposerRow
import chat.schildi.revenge.compose.focus.FocusContainer
import chat.schildi.revenge.publishTitle
import kotlinx.collections.immutable.persistentListOf

@Composable
fun ConversationScreen(destination: Destination.Conversation, modifier: Modifier = Modifier) {
    val keyHandler = LocalKeyboardActionHandler.current
    val viewModel: ConversationViewModel = viewModel(
        key = "${LocalDestinationState.current?.id}/${destination.sessionId}/${destination.roomId}",
        factory = ConversationViewModel.factory(destination.sessionId, destination.roomId, keyHandler)
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

    val listAction = remember(listState) { ListAction(listState, isReverseList = true) }
    FocusContainer(
        //LocalSearchProvider provides viewModel, // TODO CV search
        LocalKeyboardActionProvider provides viewModel.hierarchicalKeyboardActionProvider(),
        LocalListActionProvider provides listAction,
        modifier = modifier,
    ) {
        Column(Modifier.widthIn(max = ScPrefs.MAX_WIDTH_CONVERSATION.value().dp)) {
            // Double reverse helps with stick-to-bottom while paging backwards or receiving messages
            LazyColumn(Modifier.fillMaxWidth().weight(1f), reverseLayout = true, state = listState) {
                val renderedItems = timelineItems.reversed()
                itemsIndexed(renderedItems) { index, item ->
                    // Reversed list, let's not confuse us too much and still say "previous = older"
                    val next = renderedItems.getOrNull(index - 1)
                    val previous = renderedItems.getOrNull(index + 1)
                    ConversationItemRow(
                        viewModel = viewModel,
                        item = item,
                        next = next,
                        previous = previous,
                    )
                }
            }
            val shouldShowComposer = viewModel.shouldShowComposer.collectAsState().value
            LaunchedEffect(shouldShowComposer) {
                if (shouldShowComposer) {
                    keyHandler.focusByRole(FocusRole.MESSAGE_COMPOSER)
                }
            }
            AnimatedVisibility(
                visible = shouldShowComposer,
                enter = slideInVertically(tween(Anim.DURATION)) { it } +
                        expandVertically(tween(Anim.DURATION), expandFrom = Alignment.Bottom),
                exit = slideOutVertically(tween(Anim.DURATION)) { it } +
                        shrinkVertically(tween(Anim.DURATION), shrinkTowards = Alignment.Bottom),
            ) {
                ComposerRow(viewModel)
            }
        }
    }
}
