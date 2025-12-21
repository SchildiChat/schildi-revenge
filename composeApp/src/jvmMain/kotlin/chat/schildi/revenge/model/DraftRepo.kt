package chat.schildi.revenge.model

import androidx.compose.ui.text.input.TextFieldValue
import io.element.android.libraries.matrix.api.media.AudioInfo
import io.element.android.libraries.matrix.api.media.FileInfo
import io.element.android.libraries.matrix.api.media.ImageInfo
import io.element.android.libraries.matrix.api.media.VideoInfo
import io.element.android.libraries.matrix.api.room.IntentionalMention
import io.element.android.libraries.matrix.api.timeline.item.event.EventOrTransactionId
import io.element.android.libraries.matrix.api.timeline.item.event.InReplyTo
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.io.File

typealias DraftKey = ScopedRoomKey

enum class DraftType {
    TEXT,
    NOTICE,
    EMOTE,
    EDIT,
    EDIT_CAPTION,
    REACTION,
    ATTACHMENT,
}

sealed interface Attachment {
    val file: File

    sealed interface VisualAttachment : Attachment {
        val thumbnailFile: File?
    }

    // TODO metadata as appropriate?
    data class Audio(override val file: File, val audioInfo: AudioInfo) : Attachment
    data class Generic(override val file: File, val fileInfo: FileInfo) : Attachment // Not called "File" to make it less confusing with java File
    data class Image(
        override val file: File,
        override val thumbnailFile: File?,
        val imageInfo: ImageInfo,
    ) : VisualAttachment
    data class Video(
        override val file: File,
        override val thumbnailFile: File?,
        val videoInfo: VideoInfo,
    ) : VisualAttachment
}

data class DraftValue(
    val type: DraftType = DraftType.TEXT,
    val textFieldValue: TextFieldValue = TextFieldValue(""),
    val intentionalMentions: ImmutableList<IntentionalMention> = persistentListOf(),
    val inReplyTo: InReplyTo.Ready? = null,
    val editEventId: EventOrTransactionId? = null, // Only for DraftType.EDIT and DraftType.EDIT_CAPTION
    val isSendInProgress: Boolean = false,
    val initialBody: String = "", // For edits the original message content, else empty
    val attachment: Attachment? = null, // Mandatory for DraftType.ATTACHMENT, otherwise unused
) {
    val body: String
        get() = textFieldValue.text
    val htmlBody: String?
        get() = null // TODO?

    fun isEmpty() = attachment?.takeIf { type == DraftType.ATTACHMENT } == null &&
            (textFieldValue.text.isBlank() || textFieldValue.text == initialBody)
    fun canSend() = !isSendInProgress && !isEmpty()
    /** Whether an attachment can be added to the current composer state without dropping state. */
    fun canAddAttachment() = editEventId == null && !isSendInProgress && type == DraftType.TEXT
}

// TODO may add some persistent storage to this one to survive restarts & crashes
object DraftRepo {
    private val drafts = MutableStateFlow<ImmutableMap<DraftKey, DraftValue>>(persistentMapOf())

    val roomsWithDrafts = drafts.map {
        it.filter { (k, v) -> !v.isEmpty() }.keys
    }

    fun update(draftKey: DraftKey, draftValue: DraftValue) {
        drafts.update {
            (it + (draftKey to draftValue)).toPersistentMap()
        }
    }

    fun update(draftKey: DraftKey, transform: (DraftValue?) -> DraftValue?) {
        drafts.update {
            val value = transform(it[draftKey])
            if (value == null) {
                it - draftKey
            } else {
                it + (draftKey to value)
            }.toPersistentMap()
        }
    }

    fun deleteDraft(draftKey: DraftKey) {
        drafts.update {
            it.minus(draftKey).toPersistentMap()
        }
    }

    fun followDraft(draftKey: DraftKey) = drafts.map {
        it[draftKey]
    }
}
