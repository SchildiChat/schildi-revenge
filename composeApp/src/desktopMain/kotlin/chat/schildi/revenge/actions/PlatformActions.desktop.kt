package chat.schildi.revenge.actions

import chat.schildi.revenge.dbus.FreedesktopPortal
import chat.schildi.lib.util.SystemInfo
import coil3.PlatformContext
import java.awt.Desktop
import java.io.File

internal actual val platformGlobalContext: PlatformContext
    get() = PlatformContext.INSTANCE

internal actual val platformHasUserFacingFilePaths = true

internal actual val platformOpenUri: (suspend (String) -> Unit)? =
    if (SystemInfo.isLinux()) FreedesktopPortal::openUri else null

@Suppress("UNUSED_PARAMETER")
internal actual fun platformOpenFile(
    context: PlatformContext,
    file: File,
    mimeType: String?,
    preferNewTask: Boolean,
): ActionResult {
    return try {
        if (!Desktop.isDesktopSupported()) {
            return ActionResult.Failure("Desktop integration is not available")
        }
        val desktop = Desktop.getDesktop()
        if (!desktop.isSupported(Desktop.Action.OPEN)) {
            return ActionResult.Failure("Opening files is not supported")
        }
        desktop.open(file)
        ActionResult.Success()
    } catch (t: Throwable) {
        t.toActionResult()
    }
}

@Suppress("UNUSED_PARAMETER")
internal actual fun platformPersistDownload(
    context: PlatformContext,
    file: File,
    filename: String?,
    mimeType: String?
): ActionResult {
    val parent = file.parentFile ?: return ActionResult.Failure("File has no parent directory")
    return platformOpenFile(context, parent, mimeType, preferNewTask = true)
}
