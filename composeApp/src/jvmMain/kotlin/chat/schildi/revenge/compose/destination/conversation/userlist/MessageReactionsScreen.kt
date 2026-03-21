package chat.schildi.revenge.compose.destination.conversation.userlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiPeople
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.lifecycle.viewmodel.compose.viewModel
import chat.schildi.revenge.Destination
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.ListAction
import chat.schildi.revenge.actions.LocalListActionProvider
import chat.schildi.revenge.actions.LocalRoomContextSuggestionsProvider
import chat.schildi.revenge.actions.LocalUserIdSuggestionsProvider
import chat.schildi.revenge.compose.components.EmptyListScreen
import chat.schildi.revenge.compose.focus.FocusContainer
import chat.schildi.revenge.compose.search.LocalSearchProvider
import chat.schildi.revenge.compose.util.toStringHolder
import chat.schildi.revenge.model.userlist.MessageReactionListViewModel
import chat.schildi.revenge.viewModelKey
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.empty_screen_placeholder_message_reactions
import shire.composeapp.generated.resources.message_reactions_title

@Composable
fun MessageReactionsScreen(
    destination: Destination.MessageReactions,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
) {
    val viewModel: MessageReactionListViewModel =
        viewModel(
            key = viewModelKey(destination),
            factory =
                MessageReactionListViewModel.factory(
                    destination.sessionId,
                    destination.roomId,
                    destination.eventId,
                ),
        )

    val listState = rememberLazyListState()
    val listAction = remember(listState) { ListAction(listState) }
    FocusContainer(
        LocalSearchProvider provides viewModel,
        LocalUserIdSuggestionsProvider provides viewModel,
        LocalRoomContextSuggestionsProvider provides viewModel.roomContextSuggestionsProvider,
        LocalListActionProvider provides listAction,
        role = FocusRole.DESTINATION_ROOT_CONTAINER,
        modifier = modifier.fillMaxSize(),
    ) {
        val reactionsState = viewModel.entries.collectAsState(null).value
        val reactions = reactionsState?.items
        Column(contentModifier.fillMaxSize()) {
            ConversationDetailsTopNavigation(stringResource(Res.string.message_reactions_title))
            if (reactions.isNullOrEmpty()) {
                EmptyListScreen(
                    title = Res.string.empty_screen_placeholder_message_reactions.toStringHolder(),
                    icon = rememberVectorPainter(Icons.Default.EmojiPeople),
                    renderedSearchTerm = reactionsState?.searchTerm,
                    isLoading = reactions == null,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            } else {
                LazyColumn(
                    Modifier.fillMaxWidth().weight(1f),
                    state = listState,
                ) {
                    items(
                        reactions,
                        key = { Pair(it.reactionSender, it.reaction) },
                    ) { item ->
                        UserReactionRow(
                            reactionItem = item,
                            viewModel = viewModel,
                        )
                    }
                }
            }
        }
    }
}
