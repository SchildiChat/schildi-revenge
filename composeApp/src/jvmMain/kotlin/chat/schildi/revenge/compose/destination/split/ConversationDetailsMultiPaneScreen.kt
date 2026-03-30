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
                    if (!hasDetails && (collapseSinglePane || ScPrefs.HIDE_EMPTY_CONVERSATION_DETAILS_PANE.value())) {
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
) = buildMultiPaneDestinationStateHolderWrapper(
    parent = parent,
    inner = this,
    isDetails = isDetails,
    accessDetails = { destination.details },
    createPlaceholder = { Destination.MultiPaneRoomInfoPlaceholder },
    mainDestination = DestinationEnum.Conversation,
    allowedDetailsDestinations = listOf(
        DestinationEnum.RoomMembers,
        DestinationEnum.MessageReactions,
        DestinationEnum.MessageReadReceipts,
        DestinationEnum.UserDetails,
    ),
    allowedDetailsCategories = listOf(DestinationCategory.CONVERSATION_DETAILS),
)
