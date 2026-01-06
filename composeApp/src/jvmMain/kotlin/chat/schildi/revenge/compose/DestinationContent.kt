package chat.schildi.revenge.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import chat.schildi.preferences.ScPrefs
import chat.schildi.preferences.value
import chat.schildi.revenge.DestinationStateHolder
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.compose.components.ComposeSessionScope
import chat.schildi.revenge.compose.destination.AccountManagementScreen
import chat.schildi.revenge.compose.destination.conversation.ConversationScreen
import chat.schildi.revenge.compose.destination.inbox.InboxScreen
import chat.schildi.revenge.compose.destination.SplashScreen
import chat.schildi.revenge.compose.destination.split.SplitHorizontal
import chat.schildi.revenge.compose.destination.split.SplitVertical
import chat.schildi.revenge.Destination
import chat.schildi.revenge.compose.destination.AboutScreen
import chat.schildi.revenge.compose.destination.settings.SettingsScreen

@Composable
fun DestinationContent(destinationHolder: DestinationStateHolder, modifier: Modifier = Modifier) {
    CompositionLocalProvider(
        LocalDestinationState provides destinationHolder,
    ) {
        val destination = destinationHolder.state.collectAsState().value.destination
        val contentModifier = Modifier.fillMaxSize()
        DestinationWrapper(destination, modifier) {
            when (destination) {
                is Destination.AccountManagement -> AccountManagementScreen(contentModifier)
                is Destination.Inbox -> InboxScreen(contentModifier)
                is Destination.Splash -> SplashScreen(contentModifier)
                is Destination.Conversation -> ConversationScreen(destination, contentModifier)
                is Destination.SplitHorizontal -> SplitHorizontal(destination, contentModifier)
                is Destination.SplitVertical -> SplitVertical(destination, contentModifier)
                is Destination.About -> AboutScreen(contentModifier)
                is Destination.Settings -> SettingsScreen(contentModifier)
            }
        }
    }
}

@Composable
private fun DestinationWrapper(
    destination: Destination,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier) {
        if (destination is Destination.WithSession) {
            ComposeSessionScope(destination.sessionId, content = content)
        } else {
            content()
        }
        if (ScPrefs.FRAME_DROP_SPINNER.value()) {
            CircularProgressIndicator()
        }
    }
}
