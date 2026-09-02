package chat.schildi.revenge.compose.components

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

actual val platformHasBackHandler = true

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled, onBack)
}
