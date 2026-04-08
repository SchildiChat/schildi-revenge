package chat.schildi.revenge.compose.destination.conversation.userlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.lifecycle.viewmodel.compose.viewModel
import chat.schildi.revenge.Destination
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.ListActions
import chat.schildi.revenge.actions.LocalListActionProvider
import chat.schildi.revenge.actions.LocalRoomContextSuggestionsProvider
import chat.schildi.revenge.actions.LocalUserIdSuggestionsProvider
import chat.schildi.revenge.compose.components.EmptyListScreen
import chat.schildi.revenge.compose.focus.FocusContainer
import chat.schildi.revenge.compose.search.LocalSearchProvider
import chat.schildi.revenge.compose.util.toStringHolder
import chat.schildi.revenge.model.userlist.MessageReadReceiptListViewModel
import chat.schildi.revenge.viewModelKey
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.empty_screen_placeholder_message_read_receipts
import shire.composeapp.generated.resources.message_read_receipts_title

@Composable
fun MessageReadReceiptsScreen(
    destination: Destination.MessageReadReceipts,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
) {
    val viewModel: MessageReadReceiptListViewModel =
        viewModel(
            key = viewModelKey(destination),
            factory =
                MessageReadReceiptListViewModel.factory(
                    destination.sessionId,
                    destination.roomId,
                    destination.eventId,
                ),
        )

    val listState = rememberLazyListState()
    val listAction = remember(listState) { ListActions(listState) }
    FocusContainer(
        LocalSearchProvider provides viewModel,
        LocalUserIdSuggestionsProvider provides viewModel,
        LocalRoomContextSuggestionsProvider provides viewModel.roomContextSuggestionsProvider,
        LocalListActionProvider provides listAction,
        role = FocusRole.DESTINATION_ROOT_CONTAINER,
        modifier = modifier.fillMaxSize(),
    ) {
        val state = viewModel.entries.collectAsState(null).value
        val items = state?.items
        Column(contentModifier.fillMaxSize()) {
            ConversationDetailsTopNavigation(stringResource(Res.string.message_read_receipts_title))
            if (items.isNullOrEmpty()) {
                EmptyListScreen(
                    title = Res.string.empty_screen_placeholder_message_read_receipts.toStringHolder(),
                    icon = rememberVectorPainter(Icons.Default.Visibility),
                    renderedSearchTerm = state?.searchTerm,
                    isLoading = items == null,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            } else {
                LazyColumn(
                    Modifier.fillMaxWidth().weight(1f),
                    state = listState,
                ) {
                    items(
                        items,
                        key = { it.userId },
                    ) { item ->
                        ReadReceiptListRow(
                            receiptItem = item,
                            viewModel = viewModel,
                        )
                    }
                }
            }
        }
    }
}
