package chat.schildi.revenge.util.filepicker

import java.io.File

expect object FilePicker {
    suspend fun requestFilePicker(title: String): Result<List<File>?>
}
