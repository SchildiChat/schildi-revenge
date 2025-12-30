package chat.schildi.revenge.model

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import chat.schildi.theme.ScColors
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
import kotlinx.collections.immutable.toImmutableList
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

data class DraftMention(
    val start: Int,
    val end: Int,
    val mention: IntentionalMention,
) {
    val range: IntRange
        get() = IntRange(start, end-1)
    val textRange: TextRange
        get() = TextRange(start, end)
}

data class DraftValue(
    val type: DraftType = DraftType.TEXT,
    val textFieldValue: TextFieldValue = TextFieldValue(""),
    val mentions: ImmutableList<DraftMention> = persistentListOf(),
    val inReplyTo: InReplyTo.Ready? = null,
    val editEventId: EventOrTransactionId? = null, // Only for DraftType.EDIT and DraftType.EDIT_CAPTION
    val isSendInProgress: Boolean = false,
    val initialBody: String = "", // For edits the original message content, else empty
    val attachment: Attachment? = null, // Mandatory for DraftType.ATTACHMENT, otherwise unused
) {
    val body: String
        get() = textFieldValue.text.trim()
    val htmlBody: String?
        get() = ComposerHtmlGenerator.generateFormattedHtmlBody(body, mentions)
    val intentionalMentions = mentions.map { it.mention }

    fun isEmpty() = attachment?.takeIf { type == DraftType.ATTACHMENT } == null &&
            (textFieldValue.text.isBlank() || textFieldValue.text == initialBody)
    fun canSend() = !isSendInProgress && !isEmpty()
    /** Whether an attachment can be added to the current composer state without dropping state. */
    fun canAddAttachment() = editEventId == null && type == DraftType.TEXT
}

// TODO may add some persistent storage to this one to survive restarts & crashes
object DraftRepo {
    private val drafts = MutableStateFlow<ImmutableMap<DraftKey, DraftValue>>(persistentMapOf())

    val roomsWithDrafts = drafts.map {
        it.filter { (k, v) -> !v.isEmpty() }.keys
    }

    fun update(draftKey: DraftKey, draftValue: DraftValue, allowWhileSendInProgress: Boolean = false) {
        drafts.update {
            val oldValue = it[draftKey]
            if (oldValue?.isSendInProgress == true && !allowWhileSendInProgress) {
                return@update it
            }
            (it + (draftKey to maintainAnnotations(draftValue, oldValue))).toPersistentMap()
        }
    }

    fun update(draftKey: DraftKey, allowWhileSendInProgress: Boolean = false, transform: (DraftValue?) -> DraftValue?) {
        drafts.update {
            val oldValue = it[draftKey]
            if (oldValue?.isSendInProgress == true && !allowWhileSendInProgress) {
                return@update it
            }
            val value = transform(oldValue)
            if (value == null) {
                it - draftKey
            } else {
                it + (draftKey to maintainAnnotations(value, it[draftKey]))
            }.toPersistentMap()
        }
    }

    private fun maintainAnnotations(newValue: DraftValue, oldValue: DraftValue?): DraftValue {
        val newText = newValue.textFieldValue.text
        val mentionsToRemove = mutableSetOf<DraftMention>()
        val mentionsToAdd = mutableListOf<DraftMention>()
        oldValue?.mentions?.forEach { mention ->
            if (!newValue.mentions.contains(mention)) {
                // Was already dropped anyway
                return@forEach
            }
            val mentionText = oldValue.textFieldValue.text.substring(mention.range)
            val mentionTextCheck = if (mention.end <= newText.length)
                newText.substring(mention.range)
            else
                null
            if (mentionText == mentionTextCheck) {
                // Still applicable
                return@forEach
            } else {
                mentionsToRemove.add(mention)
                // Check if text was just moved?
                val newIndex = newText.indexOf(mentionText)
                if (newIndex >= 0) {
                    val newMention = mention.copy(start = newIndex, end = newIndex + mentionText.length)
                    // Avoid duplicates
                    if (newMention !in newValue.mentions) {
                        mentionsToAdd.add(newMention)
                    }
                }
            }
        }
        return if (mentionsToAdd.isEmpty() && mentionsToRemove.isEmpty() && newValue.mentions.isEmpty()) {
            newValue
        } else {
            val mentions = (newValue.mentions - mentionsToRemove + mentionsToAdd).toImmutableList()
            newValue.copy(
                mentions = mentions,
                textFieldValue = newValue.textFieldValue.copy(
                    annotatedString = buildAnnotatedString {
                        append(newText)
                        mentions.forEach { mention ->
                            addStyle(
                                // TODO get color from theme
                                SpanStyle(color = ScColors.colorAccentGreen),
                                start = mention.start,
                                end = mention.end,
                            )
                        }
                    }
                )
            )
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
