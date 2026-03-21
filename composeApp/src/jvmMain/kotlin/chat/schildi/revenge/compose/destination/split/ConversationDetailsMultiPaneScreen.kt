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
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.NavigationPreference

@Composable
fun ConversationDetailsMultiPaneScreen(
    destination: Destination.ConversationDetailsMultiPane,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        val minSplitWidth = ScPrefs.CONVERSATION_DETAILS_SPLIT_MIN_WIDTH.value().dp
        val collapseSinglePane = maxWidth < minSplitWidth
        val hasDetails =
            destination.details.state.collectAsState().value.destination !is Destination.MultiPaneRoomInfoPlaceholder
        MultiPaneLayout(
            outerDestination = destination.type,
            innerDestinations =
                listOfNotNull(
                    if (collapseSinglePane && hasDetails) {
                        null
                    } else {
                        destination.conversation.wrapped(
                            destination = destination,
                            category = DestinationCategory.CONVERSATION,
                        )
                    },
                    if (!hasDetails && (collapseSinglePane || ScPrefs.HIDE_EMPTY_CONVERSATION_DETAILS_PANE.value())) {
                        null
                    } else {
                        destination.details.wrapped(
                            destination = destination,
                            category = DestinationCategory.CONVERSATION_DETAILS,
                        )
                    },
                ),
            contentModifier = contentModifier,
        )
    }
}

@Composable
private fun DestinationStateHolder.wrapped(
    destination: Destination.ConversationDetailsMultiPane,
    category: DestinationCategory,
    parent: DestinationStateHolder? = LocalDestinationState.current,
) = MultiPaneLayoutDestinationStateHolderWrapper(
    parent = parent,
    inner = this,
    close = if (category == DestinationCategory.CONVERSATION_DETAILS) {
        {
            destination.details.navigate(
                Destination.MultiPaneRoomInfoPlaceholder,
                NavigationPreference.REPLACE
            )
        }
    } else if (parent != null) {
        parent::closeScreen
    } else {
        null
    }
) { navDestination ->
    when (navDestination.category) {
        DestinationCategory.CONVERSATION -> {
            destination.details.navigate(
                Destination.MultiPaneRoomInfoPlaceholder,
                NavigationPreference.REPLACE,
            )
            destination.conversation.navigate(navDestination, NavigationPreference.REPLACE)
            true
        }
        DestinationCategory.CONVERSATION_DETAILS -> {
            destination.details.navigate(navDestination, NavigationPreference.REPLACE)
            true
        }
        else -> {
            false
        }
    }
}
