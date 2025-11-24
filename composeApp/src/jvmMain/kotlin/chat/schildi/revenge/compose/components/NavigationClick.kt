package chat.schildi.revenge.compose.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.clickable
import androidx.compose.foundation.onClick
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.UiState
import chat.schildi.revenge.navigation.ComposableStringHolder
import chat.schildi.revenge.navigation.Destination

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.clickToNavigate(
    enabled: Boolean = true,
    initialTitle: ComposableStringHolder? = null,
    buildDestination: () -> Destination,
): Modifier {
    val destinationState = LocalDestinationState.current
    val canNavigateCurrentWindow = destinationState != null
    return clickable(enabled = enabled && canNavigateCurrentWindow) {
        destinationState?.navigate(buildDestination())
    }.onClick(enabled = enabled, matcher = PointerMatcher.mouse(PointerButton.Tertiary)) {
        UiState.openWindow(buildDestination(), initialTitle)
    }
}
