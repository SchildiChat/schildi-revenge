package chat.schildi.revenge.compose.focus

import androidx.compose.ui.Modifier

actual fun Modifier.platformPointerClick(
    enabled: Boolean,
    button: PlatformPointerButton,
    onClick: () -> Unit,
): Modifier = this
