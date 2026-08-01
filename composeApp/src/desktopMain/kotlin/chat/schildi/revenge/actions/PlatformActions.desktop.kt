package chat.schildi.revenge.actions

import chat.schildi.revenge.dbus.FreedesktopPortal
import chat.schildi.revenge.util.SystemInfo
import java.awt.Desktop
import java.io.File

internal actual val platformHasUserFacingFilePaths = true

internal actual val platformOpenUri: (suspend (String) -> Unit)? =
    if (SystemInfo.isLinux()) FreedesktopPortal::openUri else null

@Suppress("UNUSED_PARAMETER")
internal actual fun platformOpenFile(file: File, mimeType: String?): ActionResult {
    Desktop.getDesktop().open(file)
    return ActionResult.Success()
}

@Suppress("UNUSED_PARAMETER")
internal actual fun platformPersistDownload(file: File, filename: String?, mimeType: String?): ActionResult {
    Desktop.getDesktop().open(file.parentFile)
    return ActionResult.Success()
}
