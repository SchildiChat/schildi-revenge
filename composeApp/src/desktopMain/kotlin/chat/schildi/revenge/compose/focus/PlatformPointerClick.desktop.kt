package chat.schildi.revenge.compose.focus

import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.onClick
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
actual fun Modifier.platformPointerClick(
    enabled: Boolean,
    button: PlatformPointerButton,
    onClick: () -> Unit,
): Modifier =
    this.onClick(
        enabled = enabled,
        matcher = PointerMatcher.mouse(
            when (button) {
                PlatformPointerButton.Secondary -> PointerButton.Secondary
                PlatformPointerButton.Tertiary -> PointerButton.Tertiary
            }
        ),
        onClick = onClick,
    )
