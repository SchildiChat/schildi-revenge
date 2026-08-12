package chat.schildi.revenge.bugreport

import io.element.android.libraries.core.data.tryOrNull
import timber.log.Timber
import java.io.IOException
import java.io.OutputStreamWriter

private val logcatCommandDebug = arrayOf("logcat", "-d", "-v", "threadtime", "*:*")

actual val canReadPlatformLog = true
actual fun getPlatformLogContent(streamWriter: OutputStreamWriter) {
    val logcatProcess = tryOrNull {
        Runtime.getRuntime().exec(logcatCommandDebug)
    } ?: return
    try {
        val separator = System.lineSeparator()
        logcatProcess.inputStream
            .reader()
            .buffered(RageshakeConfig.MAX_LOG_UPLOAD_SIZE.toInt())
            .forEachLine { line ->
                streamWriter.append(line)
                streamWriter.append(separator)
            }
    } catch (e: IOException) {
        Timber.e(e, "getPlatformLogContent fails")
    }
}
