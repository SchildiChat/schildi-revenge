package chat.schildi.revenge.compose.destination.inbox

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import chat.schildi.revenge.Destination
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.compose.mouse.TopNavigation
import chat.schildi.revenge.compose.search.SearchBar
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.hint_settings

@Composable
fun InboxTopNavigation() {
    TopNavigation {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                SearchBar()
            }
            val destinationState = LocalDestinationState.current
            if (destinationState != null) {
                IconButton(
                    onClick = {
                        destinationState.navigate(Destination.AccountManagement)
                    }
                ) {
                    Icon(
                        Icons.Default.Settings,
                        stringResource(Res.string.hint_settings),
                    )
                }
            }
        }
    }
}
