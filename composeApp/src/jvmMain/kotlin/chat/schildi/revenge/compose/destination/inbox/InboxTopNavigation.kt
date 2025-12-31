package chat.schildi.revenge.compose.destination.inbox

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import chat.schildi.revenge.Destination
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.compose.components.TopNavigation
import chat.schildi.revenge.compose.components.TopNavigationIcon
import chat.schildi.revenge.compose.components.TopNavigationSearchOrTitle
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.about
import shire.composeapp.generated.resources.app_title
import shire.composeapp.generated.resources.hint_settings

@Composable
fun InboxTopNavigation(title: String?) {
    TopNavigation {
        val destinationState = LocalDestinationState.current
        TopNavigationSearchOrTitle(title ?: stringResource(Res.string.app_title))
        if (destinationState != null) {
            TopNavigationIcon(
                Icons.Default.Info,
                stringResource(Res.string.about)
            ) {
                destinationState.navigate(Destination.About)
            }
            TopNavigationIcon(
                Icons.Default.Settings,
                stringResource(Res.string.hint_settings)
            ) {
                destinationState.navigate(Destination.AccountManagement)
            }
        }
    }
}
