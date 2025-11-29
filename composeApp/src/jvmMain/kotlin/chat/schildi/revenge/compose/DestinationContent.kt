package chat.schildi.revenge.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import chat.schildi.revenge.Anim
import chat.schildi.revenge.actions.KeyboardActionMode
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.compose.components.ComposeSessionScope
import chat.schildi.revenge.compose.destination.AccountManagementScreen
import chat.schildi.revenge.compose.destination.ChatScreen
import chat.schildi.revenge.compose.destination.inbox.InboxScreen
import chat.schildi.revenge.compose.destination.SplashScreen
import chat.schildi.revenge.compose.focus.FocusContainer
import chat.schildi.revenge.compose.search.SearchBar
import chat.schildi.revenge.navigation.Destination

@Composable
fun DestinationContent(destination: Destination, modifier: Modifier = Modifier) {
    Column(modifier) {
        FocusContainer(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            DestinationWrapper(destination) {
                when (destination) {
                    is Destination.AccountManagement -> AccountManagementScreen()
                    is Destination.Inbox -> InboxScreen()
                    is Destination.Splash -> SplashScreen()
                    is Destination.Chat -> ChatScreen(destination)
                }
            }
        }
        val keyboardActionHandler = LocalKeyboardActionHandler.current
        AnimatedVisibility(
            visible = keyboardActionHandler.mode.collectAsState().value is KeyboardActionMode.Search,
            enter = slideInVertically(tween(Anim.DURATION)) { it } +
                    expandVertically(tween(Anim.DURATION), expandFrom = Alignment.Bottom),
            exit = slideOutVertically(tween(Anim.DURATION)) { it } +
                    shrinkVertically(tween(Anim.DURATION), shrinkTowards = Alignment.Bottom),
        ) {
            SearchBar()
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
