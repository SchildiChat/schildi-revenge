package chat.schildi.revenge.model

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

data class SharedFile(
    val file: File,
    val mimeType: String? = null,
    val isAppOwned: Boolean = false,
)

data class PendingShare(
    val text: String?,
    val file: SharedFile?,
)

/**
 * Tracks incoming shared content (Android share intents, `file:` URIs on desktop)
 * waiting for the user to select a destination room from the inbox.
 */
object IncomingShare {
    private val log = Logger.withTag("IncomingShare")

    private val _share = MutableStateFlow<PendingShare?>(null)
    val share: StateFlow<PendingShare?> = _share.asStateFlow()

    fun set(text: String?, file: SharedFile?) {
        _share.update { old ->
            old?.file?.takeIf { it.isAppOwned }?.let(::discardAppOwnedFile)
            if (text.isNullOrBlank() && file == null) {
                null
            } else {
                PendingShare(text, file)
            }
        }
    }

    /** Take the pending share without discarding its file; it becomes the composer draft's. */
    fun consume(): PendingShare? {
        val share = _share.value
        if (share != null) {
            _share.value = null
        }
        return share
    }

    fun clear(): Boolean {
        var didClear = false
        _share.update { old ->
            old?.file?.takeIf { it.isAppOwned }?.let(::discardAppOwnedFile)
            didClear = old != null
            null
        }
        return didClear
    }

    private fun discardAppOwnedFile(sharedFile: SharedFile) {
        if (sharedFile.file.parentFile?.deleteRecursively() == false) {
            log.w("Failed to delete shared file ${sharedFile.file.absolutePath}")
        }
    }
}
