package chat.schildi.revenge.actions

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.core.content.FileProvider
import chat.schildi.revenge.RevengeApplication
import java.io.File

internal actual val platformHasUserFacingFilePaths = false

internal actual val platformOpenUri: (suspend (String) -> Unit)? = null

internal actual fun platformOpenFile(file: File, mimeType: String?): ActionResult {
    val context = RevengeApplication.instance
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
    val resolvedMimeType = mimeType?.takeIf { it.isNotBlank() }
        ?: context.contentResolver.getType(uri)
        ?: "application/octet-stream"
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, resolvedMimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return try {
        context.startActivity(intent)
        ActionResult.Success()
    } catch (_: ActivityNotFoundException) {
        ActionResult.Failure("No application found to open this file")
    }
}
