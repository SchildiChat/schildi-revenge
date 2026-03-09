package chat.schildi.revenge.compose.destination.split

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chat.schildi.preferences.ScPrefs
import chat.schildi.preferences.value
import chat.schildi.revenge.Destination
import chat.schildi.revenge.DestinationCategory
import chat.schildi.revenge.DestinationStateHolder
import chat.schildi.revenge.NavigationPreference

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
            destination.conversation.state.collectAsState().value.destination !is Destination.MultiPanePlaceholder
        MultiPaneLayout(
            outerDestination = destination.type,
            innerDestinations = listOfNotNull(
                if (collapseSinglePane && hasConversation)
                    null
                else
                    destination.inbox.wrapped(destination, DestinationCategory.INBOX),
                if (!hasConversation && (collapseSinglePane || ScPrefs.HIDE_EMPTY_INBOX_PANE.value()))
                    null
                else
                    destination.conversation.wrapped(destination, DestinationCategory.CONVERSATION),
            ),
            contentModifier = contentModifier,
        )
    }
}

private fun DestinationStateHolder.wrapped(
    destination: Destination.InboxConversationMultiPane,
    category: DestinationCategory,
) = MultiPaneLayoutDestinationStateHolderWrapper(
    inner = this,
    close = if (category == DestinationCategory.CONVERSATION) {
        {
            destination.conversation.navigate(
                Destination.MultiPanePlaceholder(DestinationCategory.CONVERSATION),
                NavigationPreference.REPLACE
            )
        }
    } else
        null
) { navDestination ->
    when (navDestination.category) {
        DestinationCategory.INBOX -> {
            destination.conversation.navigate(
                Destination.MultiPanePlaceholder(DestinationCategory.CONVERSATION),
                NavigationPreference.REPLACE
            )
            true
        }
        DestinationCategory.CONVERSATION -> {
            destination.conversation.navigate(navDestination, NavigationPreference.REPLACE)
            true
        }
        else -> {
            false
        }
    }
}
