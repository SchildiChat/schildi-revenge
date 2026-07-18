package chat.schildi.revenge.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import chat.schildi.lib.preferences.ScPrefs
import chat.schildi.revenge.preferences.value
import chat.schildi.revenge.Anim
import chat.schildi.revenge.DestinationStateHolder
import chat.schildi.revenge.actions.KeyboardActionMode
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.compose.command.CommandBar
import chat.schildi.revenge.compose.components.AppMessages
import chat.schildi.revenge.compose.focus.windowFocusContainer
import chat.schildi.revenge.compose.search.SearchBar
import chat.schildi.revenge.compose.util.rememberInvalidating
import chat.schildi.theme.ScTheme
import chat.schildi.theme.scExposures

@Composable
fun WindowContent(
    destinationHolder: DestinationStateHolder,
    modifier: Modifier = Modifier,
) {
    ScTheme {
        val backgroundAlpha = if (MaterialTheme.scExposures.isDarkTheme) {
            ScPrefs.BACKGROUND_ALPHA_DARK.value()
        } else {
            ScPrefs.BACKGROUND_ALPHA_LIGHT.value()
        }
        Box(modifier) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = backgroundAlpha))
                    .fillMaxSize()
                    .windowFocusContainer(),
            ) {
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    DestinationContent(destinationHolder, Modifier.fillMaxSize())
                }
                val keyboardActionHandler = LocalKeyboardActionHandler.current

                // App messages
                val publishedMessages = keyboardActionHandler.messageBoard.collectAsState().value
                val showSearchBar = ScPrefs.MINIMAL_MODE.value() &&
                        keyboardActionHandler.needsKeyboardSearchBar(null).collectAsState().value
                val showCommandBar = keyboardActionHandler.mode.collectAsState().value is KeyboardActionMode.Command
                val showsBottomContent = showSearchBar || showCommandBar ||
                        publishedMessages.any { it.dismissedTimestamp == null }

                Column(
                    if (showsBottomContent) Modifier.navigationBarsPadding().imePadding() else Modifier,
                ) {
                    rememberInvalidating(
                        500L.takeIf { publishedMessages.any { it.dismissedTimestamp == null && it.autoDismissDuration != null } },
                        publishedMessages
                    ) {
                        keyboardActionHandler.cleanUpMessageBoard()
                    }
                    AppMessages(publishedMessages, destinationHolder)

                    // Search bar
                    AnimatedVisibility(
                        visible = showSearchBar,
                        enter = slideInVertically(tween(Anim.DURATION)) { it } +
                                expandVertically(tween(Anim.DURATION), expandFrom = Alignment.Bottom),
                        exit = slideOutVertically(tween(Anim.DURATION)) { it } +
                                shrinkVertically(tween(Anim.DURATION), shrinkTowards = Alignment.Bottom),
                    ) {
                        SearchBar(null, null)
                    }

                    // Command bar
                    AnimatedVisibility(
                        visible = showCommandBar,
                        enter = slideInVertically(tween(Anim.DURATION)) { it } +
                                expandVertically(tween(Anim.DURATION), expandFrom = Alignment.Bottom),
                        exit = slideOutVertically(tween(Anim.DURATION)) { it } +
                                shrinkVertically(tween(Anim.DURATION), shrinkTowards = Alignment.Bottom),
                    ) {
                        CommandBar()
                    }
                }
            }
            if (ScPrefs.FRAME_DROP_SPINNER.value()) {
                CircularProgressIndicator()
            }
        }
    }
}
