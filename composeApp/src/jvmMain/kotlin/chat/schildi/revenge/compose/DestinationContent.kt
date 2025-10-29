package chat.schildi.revenge.compose

import androidx.compose.runtime.Composable
import chat.schildi.revenge.compose.destination.account.AccountManagementScreen
import chat.schildi.revenge.navigation.AccountManagementDestination
import chat.schildi.revenge.navigation.Destination

@Composable
fun DestinationContent(state: Destination) {
    when (state) {
        is AccountManagementDestination -> AccountManagementScreen()
    }
}
