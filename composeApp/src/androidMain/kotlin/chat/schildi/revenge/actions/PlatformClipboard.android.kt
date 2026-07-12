package chat.schildi.revenge.actions

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry

actual fun platformTextClipEntry(content: String): ClipEntry = ClipEntry(ClipData.newPlainText(null, content))
