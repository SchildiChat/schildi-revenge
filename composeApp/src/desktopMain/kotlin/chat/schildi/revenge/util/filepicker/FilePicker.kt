package chat.schildi.revenge.util.filepicker

import chat.schildi.revenge.WindowId
import chat.schildi.revenge.dbus.FreedesktopPortal
import chat.schildi.lib.util.OperatingSystem
import chat.schildi.lib.util.SystemInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame

actual object FilePicker {

    actual suspend fun requestFilePicker(title: String, windowId: WindowId): Result<FilePickerResult?> = when (SystemInfo.getOs()) {
        OperatingSystem.Linux -> runCatching {
            FreedesktopPortal.requestFile(title)?.let(::FilePickerResult)
        }
        else -> awtAttachmentPicker(title)
    }

    private suspend fun awtAttachmentPicker(title: String): Result<FilePickerResult?> = withContext(Dispatchers.Swing) {
        runCatching {
            val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
            dialog.isMultipleMode = false
            dialog.isVisible = true

            dialog.files.singleOrNull()?.let(::FilePickerResult)
        }
    }
}
