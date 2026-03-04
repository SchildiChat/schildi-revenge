package chat.schildi.revenge.compose.destination.split

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
        val collapseSinglePane = maxWidth < 800.dp // TODO config or something
        val hasConversation =
            destination.conversation.state.collectAsState().value.destination !is Destination.MultiPanePlaceholder
        MultiPaneLayout(
            outerDestination = destination.type,
            innerDestinations = listOfNotNull(
                if (collapseSinglePane && hasConversation)
                    null
                else
                    destination.inbox.wrapped(destination, DestinationCategory.INBOX),
                if (collapseSinglePane && !hasConversation)
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
