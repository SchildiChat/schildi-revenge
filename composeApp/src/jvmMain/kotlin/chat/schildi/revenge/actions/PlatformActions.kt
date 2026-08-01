package chat.schildi.revenge.actions

import java.io.File

internal expect val platformHasUserFacingFilePaths: Boolean

internal expect val platformOpenUri: (suspend (String) -> Unit)?

internal expect fun platformOpenFile(file: File, mimeType: String?): ActionResult
