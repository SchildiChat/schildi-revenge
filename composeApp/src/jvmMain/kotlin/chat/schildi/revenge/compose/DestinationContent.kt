package chat.schildi.revenge.compose

import androidx.compose.runtime.Composable
import chat.schildi.revenge.compose.components.ComposeSessionScope
import chat.schildi.revenge.compose.destination.AccountManagementScreen
import chat.schildi.revenge.compose.destination.ChatScreen
import chat.schildi.revenge.compose.destination.inbox.InboxScreen
import chat.schildi.revenge.compose.destination.SplashScreen
import chat.schildi.revenge.navigation.Destination

@Composable
fun DestinationContent(destination: Destination) {
    DestinationWrapper(destination) {
        when (destination) {
            is Destination.AccountManagement -> AccountManagementScreen()
            is Destination.Inbox -> InboxScreen()
            is Destination.Splash -> SplashScreen()
            is Destination.Chat -> ChatScreen(destination)
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
