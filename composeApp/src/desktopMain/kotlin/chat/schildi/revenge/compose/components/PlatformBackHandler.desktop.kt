package chat.schildi.revenge.compose.components

import androidx.compose.runtime.Composable

actual val platformHasBackHandler = false

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit
