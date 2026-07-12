package chat.schildi.revenge.util.filepicker

import java.io.File

// TODO launch file picker
actual object FilePicker {
    actual suspend fun requestFilePicker(title: String): Result<List<File>?> = Result.failure(NotImplementedError())
}
