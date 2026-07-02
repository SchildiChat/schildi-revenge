package chat.schildi.revenge.compose.destination.inbox

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import chat.schildi.revenge.Destination
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.compose.components.TopNavigation
import chat.schildi.revenge.compose.components.TopNavigationIcon
import chat.schildi.revenge.compose.components.TopNavigationSearchOrTitle
import org.jetbrains.compose.resources.stringResource
import shire.res.generated.resources.Res
import shire.res.generated.resources.app_title_short
import shire.res.generated.resources.hint_settings
import shire.res.generated.resources.manage_accounts

@Composable
fun InboxTopNavigation(title: String?) {
    TopNavigation {
        val destinationState = LocalDestinationState.current
        TopNavigationSearchOrTitle(title ?: stringResource(Res.string.app_title_short))
        if (destinationState != null) {
            TopNavigationIcon(
                Icons.Default.AccountCircle,
                stringResource(Res.string.manage_accounts)
            ) {
                destinationState.navigate(Destination.AccountManagement)
            }
            TopNavigationIcon(
                Icons.Default.Settings,
                stringResource(Res.string.hint_settings)
            ) {
                destinationState.navigate(Destination.Settings())
            }
        }
    }
}
