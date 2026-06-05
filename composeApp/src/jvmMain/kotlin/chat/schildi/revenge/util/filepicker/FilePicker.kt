package chat.schildi.revenge.util.filepicker

import chat.schildi.revenge.dbus.FreedesktopPortal
import chat.schildi.revenge.util.OperatingSystem
import chat.schildi.revenge.util.SystemInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

object FilePicker {

    suspend fun requestFilePicker(title: String): Result<List<File>?> = when (SystemInfo.getOs()) {
        OperatingSystem.Linux -> runCatching {
            FreedesktopPortal.requestFiles(title)
        }
        else -> awtAttachmentPicker(title)
    }

    private suspend fun awtAttachmentPicker(title: String): Result<List<File>> = withContext(Dispatchers.Swing) {
        runCatching {
            val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
            dialog.isMultipleMode = false
            dialog.isVisible = true

            return@runCatching dialog.files.toList()
        }
    }
}
