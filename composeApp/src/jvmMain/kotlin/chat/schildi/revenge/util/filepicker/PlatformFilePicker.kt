package chat.schildi.revenge.util.filepicker

import chat.schildi.revenge.WindowId
import java.io.File

data class FilePickerResult(
    val file: File,
    val mimeType: String? = null,
    val isAppOwned: Boolean = false,
)

expect object FilePicker {
    suspend fun requestFilePicker(title: String, windowId: WindowId): Result<FilePickerResult?>
}
