package chat.schildi.revenge.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chat.schildi.preferences.ScPrefs
import chat.schildi.preferences.value
import chat.schildi.revenge.Anim
import chat.schildi.revenge.DestinationStateHolder
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.compose.components.AppMessages
import chat.schildi.revenge.compose.focus.windowFocusContainer
import chat.schildi.revenge.compose.search.SearchBar
import chat.schildi.revenge.compose.util.rememberInvalidating
import chat.schildi.theme.ScTheme
import chat.schildi.theme.scExposures
import co.touchlab.kermit.Logger

@Composable
fun WindowContent(destinationHolder: DestinationStateHolder) {
    ScTheme {
        val backgroundAlpha = if (MaterialTheme.scExposures.isDarkTheme) {
            ScPrefs.BACKGROUND_ALPHA_DARK.value()
        } else {
            ScPrefs.BACKGROUND_ALPHA_LIGHT.value()
        }
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface.copy(alpha = backgroundAlpha))
                .safeContentPadding()
                .fillMaxSize()
                .windowFocusContainer(),
        ) {
            DestinationContent(destinationHolder, Modifier.fillMaxWidth().weight(1f))
            val keyboardActionHandler = LocalKeyboardActionHandler.current

            // App messages
            val publishedMessages = keyboardActionHandler.messageBoard.collectAsState().value
            rememberInvalidating(
                500L.takeIf { publishedMessages.any { it.dismissedTimestamp == null && it.canAutoDismiss } },
                publishedMessages
            ) {
                keyboardActionHandler.cleanUpMessageBoard()
            }
            AppMessages(publishedMessages)

            // Search bar
            AnimatedVisibility(
                visible = keyboardActionHandler.needsKeyboardSearchBar.collectAsState().value,
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
