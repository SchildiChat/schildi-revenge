package chat.schildi.revenge.compose.components

import androidx.compose.runtime.Composable

/**
 * Disable platform's context menu for rendered selectable text.
 */
@Composable
expect fun WithPlatformTextContextMenuDisabled(content: @Composable () -> Unit)
