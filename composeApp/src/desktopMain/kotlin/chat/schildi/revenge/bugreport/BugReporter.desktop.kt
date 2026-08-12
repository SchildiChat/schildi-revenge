package chat.schildi.revenge.bugreport

import java.io.OutputStreamWriter

actual val canReadPlatformLog = false
actual fun getPlatformLogContent(streamWriter: OutputStreamWriter) {}
