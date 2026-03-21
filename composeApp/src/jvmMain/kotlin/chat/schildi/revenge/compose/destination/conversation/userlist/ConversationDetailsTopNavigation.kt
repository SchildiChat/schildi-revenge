package chat.schildi.revenge.compose.destination.conversation.userlist

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.compose.components.TopNavigation
import chat.schildi.revenge.compose.components.TopNavigationIcon
import chat.schildi.revenge.compose.components.TopNavigationTitle
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.action_close

@Composable
fun ConversationDetailsTopNavigation(
    title: String,
) {
    val destinationState = LocalDestinationState.current
    val keyHandler = LocalKeyboardActionHandler.current
    TopNavigation {
        TopNavigationTitle(title)
        TopNavigationIcon(
            Icons.Default.Close,
            stringResource(Res.string.action_close),
        ) {
            destinationState?.closeScreen(keyHandler)
        }
    }
}
