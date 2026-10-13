package chat.schildi.revenge.actions

import coil3.PlatformContext
import java.io.File

internal expect val platformGlobalContext: PlatformContext

internal expect val platformHasUserFacingFilePaths: Boolean

internal expect val platformOpenUri: (suspend (String) -> Unit)?

internal expect fun platformOpenFile(context: PlatformContext, file: File, mimeType: String?, preferNewTask: Boolean = false): ActionResult

internal expect fun platformPersistDownload(context: PlatformContext, file: File, filename: String?, mimeType: String?): ActionResult
