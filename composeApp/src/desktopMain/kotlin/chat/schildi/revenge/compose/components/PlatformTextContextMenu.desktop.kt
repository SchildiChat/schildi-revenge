package chat.schildi.revenge.compose.components

import androidx.compose.foundation.ContextMenuState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.LocalTextContextMenu
import androidx.compose.foundation.text.TextContextMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@OptIn(ExperimentalFoundationApi::class)
private object EmptyTextContextMenu : TextContextMenu {
    @Composable
    override fun Area(
        textManager: TextContextMenu.TextManager,
        state: ContextMenuState,
        content: @Composable () -> Unit
    ) = content()
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
actual fun WithPlatformTextContextMenuDisabled(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalTextContextMenu provides EmptyTextContextMenu, content = content)
}
