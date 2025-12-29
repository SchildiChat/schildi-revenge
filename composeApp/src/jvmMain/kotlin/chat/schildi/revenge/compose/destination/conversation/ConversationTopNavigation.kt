package chat.schildi.revenge.compose.destination.conversation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.runtime.Composable
import chat.schildi.revenge.Destination
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.compose.components.TopNavigation
import chat.schildi.revenge.compose.components.TopNavigationIcon
import chat.schildi.revenge.compose.components.TopNavigationTitle
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.action_open_inbox

@Composable
fun ConversationTopNavigation(title: String) {
    val destinationState = LocalDestinationState.current ?: return
    TopNavigation {
        TopNavigationTitle(title)
        TopNavigationIcon(
            Icons.Default.Inbox,
            stringResource(Res.string.action_open_inbox)
        ) {
            destinationState.navigate(Destination.Inbox)
        }
    }
}
