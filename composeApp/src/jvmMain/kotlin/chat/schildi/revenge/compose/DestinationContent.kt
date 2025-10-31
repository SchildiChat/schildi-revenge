package chat.schildi.revenge.compose

import androidx.compose.runtime.Composable
import chat.schildi.revenge.compose.destination.AccountManagementScreen
import chat.schildi.revenge.compose.destination.InboxScreen
import chat.schildi.revenge.navigation.AccountManagementDestination
import chat.schildi.revenge.navigation.Destination
import chat.schildi.revenge.navigation.InboxDestination

@Composable
fun DestinationContent(state: Destination) {
    when (state) {
        is AccountManagementDestination -> AccountManagementScreen()
        is InboxDestination -> InboxScreen()
    }
}
