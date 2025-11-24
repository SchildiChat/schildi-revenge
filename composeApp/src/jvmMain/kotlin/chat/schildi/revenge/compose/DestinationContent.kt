package chat.schildi.revenge.compose

import androidx.compose.runtime.Composable
import chat.schildi.revenge.compose.destination.AccountManagementScreen
import chat.schildi.revenge.compose.destination.ChatScreen
import chat.schildi.revenge.compose.destination.InboxScreen
import chat.schildi.revenge.compose.destination.SplashScreen
import chat.schildi.revenge.navigation.AccountManagementDestination
import chat.schildi.revenge.navigation.ChatDestination
import chat.schildi.revenge.navigation.Destination
import chat.schildi.revenge.navigation.InboxDestination
import chat.schildi.revenge.navigation.SplashDestination

@Composable
fun DestinationContent(destination: Destination) {
    when (destination) {
        is AccountManagementDestination -> AccountManagementScreen()
        is InboxDestination -> InboxScreen()
        is SplashDestination -> SplashScreen()
        is ChatDestination -> ChatScreen(destination)
    }
}
