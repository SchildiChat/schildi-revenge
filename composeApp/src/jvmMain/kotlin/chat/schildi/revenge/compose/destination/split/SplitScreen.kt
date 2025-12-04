package chat.schildi.revenge.compose.destination.split

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import chat.schildi.revenge.compose.DestinationContent
import chat.schildi.revenge.Destination
import chat.schildi.revenge.actions.LocalKeyboardActionProvider

@Composable
fun SplitHorizontal(destination: Destination.SplitHorizontal) {
    Row(Modifier.fillMaxSize()) {
        CompositionLocalProvider(
            LocalKeyboardActionProvider provides splitKeyboardActionProvider(true),
        ) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(destination.fraction), Alignment.Center) {
                DestinationContent(destination.primary, Modifier.fillMaxSize())
            }
        }
        CompositionLocalProvider(
            LocalKeyboardActionProvider provides splitKeyboardActionProvider(false),
        ) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                DestinationContent(destination.secondary, Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun SplitVertical(destination: Destination.SplitVertical) {
    Column(Modifier.fillMaxSize()) {
        CompositionLocalProvider(
            LocalKeyboardActionProvider provides splitKeyboardActionProvider(true),
        ) {
            Box(Modifier.fillMaxWidth().fillMaxHeight(destination.fraction), Alignment.Center) {
                DestinationContent(destination.primary, Modifier.fillMaxSize())
            }
        }
        CompositionLocalProvider(
            LocalKeyboardActionProvider provides splitKeyboardActionProvider(false),
        ) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                DestinationContent(destination.secondary, Modifier.fillMaxSize())
            }
        }
    }
}
