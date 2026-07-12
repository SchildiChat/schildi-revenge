package chat.schildi.revenge.compose.components

import androidx.compose.runtime.Composable

@Composable
actual fun WithPlatformTextContextMenuDisabled(content: @Composable () -> Unit) = content()
