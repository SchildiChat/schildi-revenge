package chat.schildi.revenge.compose.destination.split

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chat.schildi.preferences.ScPrefs
import chat.schildi.preferences.ScPrefs.ALLOW_THREADS_IN_DETAILS_PANE
import chat.schildi.preferences.value
import chat.schildi.revenge.Destination
import chat.schildi.revenge.DestinationCategory
import chat.schildi.revenge.DestinationStateHolder
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.config.keybindings.DestinationEnum

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
        val shouldHidePlaceholders = ScPrefs.HIDE_EMPTY_CONVERSATION_DETAILS_PANE.value()
        MultiPaneLayout(
            outerDestination = destination.type,
            innerDestinations =
                listOfNotNull(
                    if (collapseSinglePane && hasDetails) {
                        null
                    } else {
                        destination.conversation.wrapped(
                            destination = destination,
                            isDetails = false,
                        )
                    },
                    if (!hasDetails && (collapseSinglePane || shouldHidePlaceholders)) {
                        null
                    } else {
                        destination.details.wrapped(
                            destination = destination,
                            isDetails = true,
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
    isDetails: Boolean,
    parent: DestinationStateHolder? = LocalDestinationState.current,
): MultiPaneLayoutDestinationStateHolderWrapper {
    val primaryDestinationIsThread = (destination.conversation.state.value.destination as? Destination.Conversation)?.threadId != null
    val allowThreadsInDetails = ALLOW_THREADS_IN_DETAILS_PANE.value() && !primaryDestinationIsThread
    return remember(destination, isDetails, parent, primaryDestinationIsThread, allowThreadsInDetails) {
        buildMultiPaneDestinationStateHolderWrapper(
            parent = parent,
            inner = this,
            isDetails = isDetails,
            accessDetails = { destination.details },
            createPlaceholder = { Destination.MultiPaneRoomInfoPlaceholder },
            mainDestination = if (primaryDestinationIsThread) {
                DestinationEnum.ConversationThread
            } else {
                DestinationEnum.Conversation
            },
            allowedDetailsDestinations = listOfNotNull(
                DestinationEnum.RoomMembers,
                DestinationEnum.MessageReactions,
                DestinationEnum.MessageReadReceipts,
                DestinationEnum.UserDetails,
                DestinationEnum.ConversationThread.takeIf { allowThreadsInDetails },
            ),
            allowedDetailsCategories = listOfNotNull(
                DestinationCategory.CONVERSATION_DETAILS,
                DestinationCategory.CONVERSATION_THREAD.takeIf { allowThreadsInDetails },
            ),
        )
    }
}
