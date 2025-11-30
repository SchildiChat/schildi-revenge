package chat.schildi.revenge.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import chat.schildi.revenge.DestinationStateHolder
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.compose.components.ComposeSessionScope
import chat.schildi.revenge.compose.destination.AccountManagementScreen
import chat.schildi.revenge.compose.destination.conversation.ChatScreen
import chat.schildi.revenge.compose.destination.inbox.InboxScreen
import chat.schildi.revenge.compose.destination.SplashScreen
import chat.schildi.revenge.compose.destination.SplitHorizontal
import chat.schildi.revenge.compose.destination.SplitVertical
import chat.schildi.revenge.compose.focus.FocusContainer
import chat.schildi.revenge.Destination

@Composable
fun DestinationContent(destinationHolder: DestinationStateHolder, modifier: Modifier = Modifier) {
    CompositionLocalProvider(
        LocalDestinationState provides destinationHolder,
    ) {
        val destination = destinationHolder.state.collectAsState().value.destination
            FocusContainer(
                modifier = modifier,
                contentAlignment = Alignment.Center,
            ) {
                DestinationWrapper(destination) {
                    when (destination) {
                        is Destination.AccountManagement -> AccountManagementScreen()
                        is Destination.Inbox -> InboxScreen()
                        is Destination.Splash -> SplashScreen()
                        is Destination.Conversation -> ChatScreen(destination)
                        is Destination.SplitHorizontal -> SplitHorizontal(destination)
                        is Destination.SplitVertical -> SplitVertical(destination)
                    }
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
