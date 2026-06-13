package chat.schildi.revenge.compose.destination.split

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chat.schildi.preferences.ScPrefs
import chat.schildi.preferences.value
import chat.schildi.revenge.Destination
import chat.schildi.revenge.DestinationCategory
import chat.schildi.revenge.DestinationStateHolder
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.config.keybindings.DestinationEnum

@Composable
fun InboxConversationMultiPaneScreen(
    destination: Destination.InboxConversationMultiPane,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        val minSplitWidth = ScPrefs.INBOX_CONVERSATION_SPLIT_MIN_WIDTH.value().dp
        val collapseSinglePane = maxWidth < minSplitWidth
        val hasConversation =
            destination.conversation.state.collectAsState().value.destination !is Destination.MultiPaneConversationPlaceholder
        val shouldHidePlaceholders = ScPrefs.HIDE_EMPTY_INBOX_PANE.value()
        MultiPaneLayout(
            outerDestination = destination.type,
            innerDestinations = listOfNotNull(
                if (collapseSinglePane && hasConversation)
                    null
                else
                    destination.inbox.wrapped(destination, isDetails = false),
                if (!hasConversation && (collapseSinglePane || shouldHidePlaceholders))
                    null
                else
                    destination.conversation.wrapped(destination, isDetails = true),
            ),
            contentModifier = contentModifier,
        )
    }
}

@Composable
private fun DestinationStateHolder.wrapped(
    destination: Destination.InboxConversationMultiPane,
    isDetails: Boolean,
    parent: DestinationStateHolder? = LocalDestinationState.current,
) = remember(destination, isDetails, parent) {
    buildMultiPaneDestinationStateHolderWrapper(
        parent = parent,
        inner = this,
        isDetails = isDetails,
        accessMain = { destination.inbox },
        accessDetails = { destination.conversation },
        createPlaceholder = { Destination.MultiPaneConversationPlaceholder },
        mainDestination = DestinationEnum.Inbox,
        allowedDetailsDestinations = listOf(DestinationEnum.Conversation, DestinationEnum.ConversationDetailsSplit, DestinationEnum.SplitConversationPlaceholder),
        allowedDetailsCategories = listOf(DestinationCategory.CONVERSATION),
    )
}
