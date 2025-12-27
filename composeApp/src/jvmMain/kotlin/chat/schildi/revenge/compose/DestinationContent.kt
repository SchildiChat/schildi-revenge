package chat.schildi.revenge.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
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

@Composable
fun DestinationContent(destinationHolder: DestinationStateHolder, modifier: Modifier = Modifier) {
    CompositionLocalProvider(
        LocalDestinationState provides destinationHolder,
    ) {
        val destination = destinationHolder.state.collectAsState().value.destination
        DestinationWrapper(destination) {
            when (destination) {
                is Destination.AccountManagement -> AccountManagementScreen(modifier)
                is Destination.Inbox -> InboxScreen(modifier)
                is Destination.Splash -> SplashScreen(modifier)
                is Destination.Conversation -> ConversationScreen(destination, modifier)
                is Destination.SplitHorizontal -> SplitHorizontal(destination, modifier)
                is Destination.SplitVertical -> SplitVertical(destination, modifier)
                is Destination.About -> AboutScreen(modifier)
            }
        }
    }
}

@Composable
private fun DestinationWrapper(destination: Destination, content: @Composable () -> Unit) {
    if (destination is Destination.WithSession) {
        ComposeSessionScope(destination.sessionId, content = content)
    } else {
        content()
    }
}
