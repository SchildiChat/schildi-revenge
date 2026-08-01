package chat.schildi.revenge.util.filepicker

import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import chat.schildi.revenge.RevengeApplication
import chat.schildi.revenge.WindowId
import chat.schildi.revenge.androidWindowManager
import chat.schildi.revenge.util.toSafeFilename
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.uuid.Uuid

actual object FilePicker {
    actual suspend fun requestFilePicker(title: String, windowId: WindowId): Result<FilePickerResult?> {
        return try {
            val uri = withContext(Dispatchers.Main.immediate) {
                val activity = androidWindowManager.getActivity(windowId)
                    ?: error("The window is not attached to an Android activity")
                activity.filePickerLauncher.requestDocument()
            }
            if (uri == null) {
                Result.success(null)
            } else {
                Result.success(copyToCache(uri))
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (throwable: Throwable) {
            Result.failure(throwable)
        }
    }

    private suspend fun copyToCache(uri: Uri): FilePickerResult = withContext(Dispatchers.IO) {
        val application = RevengeApplication.instance
        val resolver = application.contentResolver
        val displayName = resolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameColumn >= 0 && cursor.moveToFirst()) cursor.getString(nameColumn) else null
        }
        val mimeType = resolver.getType(uri)
        val filename = safeFilename(displayName, mimeType)
        val importDir = ComposerAttachmentCache.createImportDir()
        val destination = File(importDir, filename)

        try {
            val input = resolver.openInputStream(uri)
                ?: error("The selected document could not be opened")
            input.use { source ->
                destination.outputStream().use(source::copyTo)
            }
            FilePickerResult(
                file = destination,
                mimeType = mimeType,
                isAppOwned = true,
            )
        } catch (throwable: Throwable) {
            importDir.deleteRecursively()
            throw throwable
        }
    }

    private fun safeFilename(displayName: String?, mimeType: String?): String {
        val candidate = displayName
            ?.substringAfterLast('/')
            ?.substringAfterLast('\\')
            ?.takeIf(String::isNotEmpty)
            ?.toSafeFilename(fallback = "attachment")
            ?: "attachment"
        val existingExtension = candidate.substringAfterLast('.', missingDelimiterValue = "")
            .takeIf(String::isNotEmpty)
        val inferredExtension = mimeType
            ?.let(MimeTypeMap.getSingleton()::getExtensionFromMimeType)
            ?.takeIf(String::isNotEmpty)
        val extension = existingExtension ?: inferredExtension
        val suffix = extension?.let { ".$it" }.orEmpty()
        val stem = if (existingExtension == null) candidate else candidate.dropLast(existingExtension.length + 1)
        return (stem + suffix).toSafeFilename(
            fallback = "attachment$suffix",
        )
    }
}

internal object ComposerAttachmentCache {
    private val baseDir: File
        get() = File(RevengeApplication.instance.cacheDir, "composerAttachments")

    fun createImportDir(): File = File(baseDir, Uuid.random().toString()).also {
        check(it.mkdirs()) { "Failed to create composer attachment cache directory" }
    }

    fun clear() {
        baseDir.deleteRecursively()
    }
}

internal class AndroidFilePickerLauncher(activity: ComponentActivity) {
    private var pendingRequest: CancellableContinuation<Uri?>? = null
    private val launcher = activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        takePendingRequest()?.resume(uri)
    }

    suspend fun requestDocument(): Uri? = suspendCancellableCoroutine { continuation ->
        synchronized(this) {
            check(pendingRequest == null) { "An attachment picker is already open for this window" }
            pendingRequest = continuation
        }
        continuation.invokeOnCancellation {
            synchronized(this) {
                if (pendingRequest === continuation) pendingRequest = null
            }
        }
        try {
            launcher.launch(arrayOf("*/*"))
        } catch (throwable: Throwable) {
            synchronized(this) {
                if (pendingRequest === continuation) pendingRequest = null
            }
            continuation.resumeWith(Result.failure(throwable))
        }
    }

    fun close() {
        takePendingRequest()?.cancel(CancellationException("The attachment picker activity was destroyed"))
    }

    private fun takePendingRequest(): CancellableContinuation<Uri?>? = synchronized(this) {
        pendingRequest.also { pendingRequest = null }
    }
}
