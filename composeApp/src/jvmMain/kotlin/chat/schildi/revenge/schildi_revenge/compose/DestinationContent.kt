package chat.schildi.revenge.schildi_revenge.compose

import androidx.compose.runtime.Composable
import chat.schildi.revenge.schildi_revenge.compose.destination.account.AccountManagementScreen
import chat.schildi.revenge.schildi_revenge.navigation.AccountManagementDestination
import chat.schildi.revenge.schildi_revenge.navigation.Destination

@Composable
fun DestinationContent(state: Destination) {
    when (state) {
        is AccountManagementDestination -> AccountManagementScreen()
    }
}
