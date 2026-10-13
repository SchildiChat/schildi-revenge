package chat.schildi.revenge.actions

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import chat.schildi.revenge.RevengeApplication
import coil3.PlatformContext
import java.io.File

internal actual val platformGlobalContext: PlatformContext
    get() = RevengeApplication.instance

internal actual val platformHasUserFacingFilePaths = false

internal actual val platformOpenUri: (suspend (String) -> Unit)? = null

val Context.fileProviderAuthority: String
    get() = "${packageName}.fileprovider"

internal actual fun platformOpenFile(
    context: PlatformContext,
    file: File,
    mimeType: String?,
    preferNewTask: Boolean,
): ActionResult {
    val uri = FileProvider.getUriForFile(
        context,
        context.fileProviderAuthority,
        file,
    )
    val resolvedMimeType = mimeType?.takeIf { it.isNotBlank() }
        ?: context.contentResolver.getType(uri)
        ?: "application/octet-stream"
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, resolvedMimeType)
        if (preferNewTask || context is Application) {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        } else {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    return try {
        context.startActivity(intent)
        ActionResult.Success()
    } catch (_: ActivityNotFoundException) {
        ActionResult.Failure("No application found to open this file")
    }
}

internal actual fun platformPersistDownload(
    context: PlatformContext,
    file: File,
    filename: String?,
    mimeType: String?
): ActionResult {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return ActionResult.Inapplicable

    val displayName = filename
        ?.replace('\\', '/')
        ?.substringAfterLast('/')
        ?.replace(Regex("[\\u0000-\\u001f\\u007f]"), "_")
        ?.takeIf { it.isNotBlank() }
        ?: file.name
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
        mimeType?.takeIf { it.isNotBlank() }?.let { put(MediaStore.MediaColumns.MIME_TYPE, it) }
        put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/SchildiChat")
        put(MediaStore.MediaColumns.IS_PENDING, 1)
    }
    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        ?: return ActionResult.Failure("Failed to create file in Downloads")

    return try {
        val output = resolver.openOutputStream(uri)
            ?: error("Failed to open file in Downloads")
        file.inputStream().use { input ->
            output.use { input.copyTo(it) }
        }
        val completedValues = ContentValues().apply {
            put(MediaStore.MediaColumns.IS_PENDING, 0)
        }
        check(resolver.update(uri, completedValues, null, null) == 1) {
            "Failed to finish file in Downloads"
        }
        ActionResult.Success()
    } catch (t: Throwable) {
        runCatching { resolver.delete(uri, null, null) }
        t.toActionResult()
    }
}
