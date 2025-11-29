package chat.schildi.revenge.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import chat.schildi.revenge.Anim
import chat.schildi.revenge.DestinationStateHolder
import chat.schildi.revenge.actions.KeyboardActionMode
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.compose.focus.windowFocusContainer
import chat.schildi.revenge.compose.search.SearchBar
import chat.schildi.theme.ScTheme

@Composable
fun WindowContent(destinationHolder: DestinationStateHolder) {
    ScTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .safeContentPadding()
                .fillMaxSize()
                .windowFocusContainer(),
        ) {
            DestinationContent(destinationHolder, Modifier.fillMaxWidth().weight(1f))
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
}
