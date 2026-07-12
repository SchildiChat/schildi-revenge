package chat.schildi.revenge.actions

import androidx.compose.ui.platform.ClipEntry
import java.awt.datatransfer.StringSelection

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
actual fun platformTextClipEntry(content: String): ClipEntry = ClipEntry(StringSelection(content))
