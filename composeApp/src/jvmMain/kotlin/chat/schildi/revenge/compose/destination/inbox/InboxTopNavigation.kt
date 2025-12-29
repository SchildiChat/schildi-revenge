package chat.schildi.revenge.compose.destination.inbox

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import chat.schildi.revenge.Destination
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.compose.components.TopNavigation
import chat.schildi.revenge.compose.components.TopNavigationIcon
import chat.schildi.revenge.compose.search.SearchBar
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.about
import shire.composeapp.generated.resources.action_clear_search
import shire.composeapp.generated.resources.hint_search
import shire.composeapp.generated.resources.hint_settings

@Composable
fun InboxTopNavigation() {
    TopNavigation {
        val destinationState = LocalDestinationState.current
        val keyHandler = LocalKeyboardActionHandler.current
        val searchBarVisible = keyHandler.needsKeyboardSearchBar.collectAsState().value
        Box(Modifier.weight(1f)) {
            androidx.compose.animation.AnimatedVisibility(searchBarVisible, enter = fadeIn(), exit = fadeOut()) {
                SearchBar()
            }
        }
        AnimatedVisibility(searchBarVisible) {
            TopNavigationIcon(
                Icons.Default.Clear,
                stringResource(Res.string.action_clear_search)
            ) {
                keyHandler.clearSearch()
            }
        }
        TopNavigationIcon(
            Icons.Default.Search,
            stringResource(Res.string.hint_search)
        ) {
            keyHandler.onSearchEnter("")
        }
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
