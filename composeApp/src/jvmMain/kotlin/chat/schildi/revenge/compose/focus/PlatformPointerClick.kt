package chat.schildi.revenge.compose.focus

import androidx.compose.ui.Modifier

enum class PlatformPointerButton {
    Secondary,
    Tertiary,
}

expect fun Modifier.platformPointerClick(
    enabled: Boolean,
    button: PlatformPointerButton,
    onClick: () -> Unit,
): Modifier
