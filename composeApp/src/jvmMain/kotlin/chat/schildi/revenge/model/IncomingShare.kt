package chat.schildi.revenge.model

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import chat.schildi.lib.preferences.ComposerFormat
import chat.schildi.lib.preferences.ScPreferencesStore
import chat.schildi.lib.preferences.ScPrefs
import chat.schildi.resources.HardcodedStringHolder
import chat.schildi.resources.StringResourceHolder
import chat.schildi.revenge.actions.ActionContext
import chat.schildi.revenge.actions.AppMessage
import chat.schildi.revenge.notification.NotificationId
import chat.schildi.revenge.notification.platformNotifyAppMessage
import chat.schildi.revenge.preferences.RevengePrefs
import chat.schildi.revenge.util.tryOrNull
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import shire.res.generated.resources.Res
import shire.res.generated.resources.incoming_share_file_error
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

    suspend fun applyShare(
        context: ActionContext?,
        draftKey: DraftKey,
        share: PendingShare,
        scPreferencesStore: ScPreferencesStore = RevengePrefs,
    ) {
        val text = share.text?.takeUnless { it.isBlank() }
        val sharedFile = share.file
        if (text == null && sharedFile == null) return

        val attachment = sharedFile?.let {
            buildAttachmentForFile(it.file, it.mimeType, it.isAppOwned)
                ?: run {
                    log.w("Failed to build attachment for shared file ${it.file.name}")
                    val message =
                        AppMessage(
                            StringResourceHolder(
                                Res.string.incoming_share_file_error,
                                HardcodedStringHolder(it.file.name),
                            ),
                            isError = true,
                        )
                    if (context == null) {
                        platformNotifyAppMessage(
                            NotificationId.AppMessage("ShareFailure"),
                            message,
                            transient = true,
                        )
                    } else {
                        context.publishMessage(message)
                    }
                    null
                }
        }

        val composerFormat = tryOrNull {
            ComposerFormat.valueOf(scPreferencesStore.getSetting(ScPrefs.PREFERRED_MESSAGE_FORMAT))
        } ?: ComposerFormat.valueOf(ScPrefs.PREFERRED_MESSAGE_FORMAT.defaultValue)

        DraftRepo.update(draftKey) { current ->
            DraftValue(
                textFieldValue = text?.let { TextFieldValue(it, TextRange(it.length)) }
                    ?: current?.textFieldValue
                    ?: TextFieldValue(),
                attachment = attachment,
                type = if (attachment == null) DraftType.TEXT else DraftType.ATTACHMENT,
                preferredFormat = if (text != null) {
                    ComposerFormat.PLAIN
                } else {
                    current?.preferredFormat ?: composerFormat
                }
            )
        }
    }
}
