package chat.schildi.revenge.compose.destination.split

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chat.schildi.lib.preferences.ScPrefs
import chat.schildi.revenge.preferences.value
import chat.schildi.revenge.Destination
import chat.schildi.revenge.DestinationCategory
import chat.schildi.revenge.DestinationStateHolder
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.NavigationPreference
import chat.schildi.revenge.compose.components.PlatformBackHandler
import chat.schildi.revenge.config.keybindings.DestinationEnum

@Composable
fun SpaceDiscoveryDetailsMultiPaneScreen(
    destination: Destination.SpaceDiscoveryDetailsMultiPane,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
) {
    if (requireMultiPaneLayout(ScPrefs.PREFER_CONVERSATION_DETAILS_SPLIT) {
        destination.spaceDiscovery.state.value
    }) {
        return
    }

    BoxWithConstraints(modifier) {
        val minSplitWidth = ScPrefs.CONVERSATION_DETAILS_SPLIT_MIN_WIDTH.value().dp
        val collapseSinglePane = maxWidth < minSplitWidth
        val hasDetails =
            destination.details.state.collectAsState().value.destination !is Destination.MultiPaneRoomInfoPlaceholder
        val shouldHidePlaceholders = ScPrefs.HIDE_EMPTY_CONVERSATION_DETAILS_PANE.value()
        PlatformBackHandler(enabled = hasDetails) {
            destination.details.navigate(Destination.MultiPaneRoomInfoPlaceholder, NavigationPreference.REPLACE)
        }
        MultiPaneLayout(
            outerDestination = destination.destinationId,
            innerDestinations =
                listOfNotNull(
                    if (collapseSinglePane && hasDetails) {
                        null
                    } else {
                        destination.spaceDiscovery.wrapped(
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
    destination: Destination.SpaceDiscoveryDetailsMultiPane,
    isDetails: Boolean,
    parent: DestinationStateHolder? = LocalDestinationState.current,
): MultiPaneLayoutDestinationStateHolderWrapper {
    val primaryRoomId = (destination.spaceDiscovery.state.collectAsState().value.destination as? Destination.SpaceDiscovery)?.roomId
    return remember(destination, isDetails, parent) {
        buildMultiPaneDestinationStateHolderWrapper(
            parent = parent,
            inner = this,
            isDetails = isDetails,
            accessMain = { destination.spaceDiscovery },
            accessDetails = { destination.details },
            createPlaceholder = { Destination.MultiPaneRoomInfoPlaceholder },
            mainDestination = DestinationEnum.SpaceDiscovery,
            allowedDetailsDestinations = listOf(
                DestinationEnum.RoomDetails,
                DestinationEnum.RoomMembers,
                DestinationEnum.UserDetails,
            ),
            allowedDetailsCategories = listOf(
                DestinationCategory.CONVERSATION_DETAILS,
            ),
            isCompatibleDetails = {
                it is Destination.WithRoomOptional && (it.roomId == null || it.roomId == primaryRoomId)
            }
        )
    }
}
