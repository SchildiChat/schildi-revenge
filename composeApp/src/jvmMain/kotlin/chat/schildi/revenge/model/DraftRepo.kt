package chat.schildi.revenge.model

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import chat.schildi.matrixsdk.ImagePackImageSource
import chat.schildi.matrixsdk.ImagePackImageWithRawInfo
import chat.schildi.revenge.model.conversation.ConversationPermissions
import chat.schildi.theme.ScColors
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.core.ThreadId
import io.element.android.libraries.matrix.api.media.AudioInfo
import io.element.android.libraries.matrix.api.media.FileInfo
import io.element.android.libraries.matrix.api.media.ImageInfo
import io.element.android.libraries.matrix.api.media.VideoInfo
import io.element.android.libraries.matrix.api.room.IntentionalMention
import io.element.android.libraries.matrix.api.timeline.InMemoryMediaThumbnail
import io.element.android.libraries.matrix.api.timeline.item.event.EventOrTransactionId
import io.element.android.libraries.matrix.api.timeline.item.event.InReplyTo
import io.ktor.http.encodeURLPath
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File

data class DraftKey(
    val sessionId: SessionId,
    val roomId: RoomId,
    val threadId: ThreadId?,
)

enum class DraftType {
    TEXT,
    NOTICE,
    EMOTE,
    EDIT,
    EDIT_CAPTION,
    REACTION,
    STICKER,
    ATTACHMENT,
    CUSTOM_EVENT,
    CUSTOM_STATE_EVENT,
}

enum class ComposerFormat {
    PLAIN,
    MARKDOWN,
    HTML,
}

fun DraftType.shouldSendTypingIndicator() = when (this) {
    DraftType.TEXT,
    DraftType.EMOTE,
    DraftType.ATTACHMENT -> true
    DraftType.NOTICE,
    DraftType.EDIT,
    DraftType.EDIT_CAPTION,
    DraftType.REACTION,
    DraftType.STICKER,
    DraftType.CUSTOM_EVENT,
    DraftType.CUSTOM_STATE_EVENT -> false
}

sealed interface Attachment {
    val file: File

    sealed interface VisualAttachment : Attachment {
        val thumbnail: InMemoryMediaThumbnail?
    }

    // TODO metadata as appropriate?
    data class Audio(override val file: File, val audioInfo: AudioInfo) : Attachment
    data class Generic(override val file: File, val fileInfo: FileInfo) : Attachment // Not called "File" to make it less confusing with java File
    data class Image(
        override val file: File,
        override val thumbnail: InMemoryMediaThumbnail?,
        val imageInfo: ImageInfo,
    ) : VisualAttachment
    data class Video(
        override val file: File,
        override val thumbnail: InMemoryMediaThumbnail?,
        val videoInfo: VideoInfo,
    ) : VisualAttachment
}

sealed interface DraftSpan {
    val start: Int
    val end: Int
    val range: IntRange
        get() = IntRange(start, end-1)
    val textRange: TextRange
        get() = TextRange(start, end)
    fun withAdjustedRange(start: Int, end: Int): DraftSpan
    fun formatContentToHtml(content: String): String
    fun formatContentToPlaintext(content: String): String = content
    // TODO themed?
    fun draftStyle(): SpanStyle = SpanStyle(color = ScColors.colorAccentGreen)
}

data class DraftMention(
    override val start: Int,
    override val end: Int,
    val mention: IntentionalMention,
) : DraftSpan {
    override fun withAdjustedRange(start: Int, end: Int) = copy(start = start, end = end)
    override fun formatContentToHtml(content: String): String = when (mention) {
        IntentionalMention.Room -> "@room"
        is IntentionalMention.User -> {
            "<a href=\"https://matrix.to/#/${mention.userId.value.encodeURLPath()}\">$content</a>"
        }
    }
    override fun formatContentToPlaintext(content: String) = when (mention) {
        IntentionalMention.Room -> "@room"
        is IntentionalMention.User -> mention.userId.value
    }
}

data class DraftCustomEmote(
    override val start: Int,
    override val end: Int,
    val shortcode: String,
    val image: ImagePackImageWithRawInfo,
    val source: ImagePackImageSource,
) : DraftSpan {
    override fun withAdjustedRange(start: Int, end: Int) = copy(start = start, end = end)
    override fun formatContentToHtml(content: String) = buildString {
        append("<img data-mx-emoticon height=\"32\" src=\"")
        append(image.url)
        append("\" title=\"")
        append(shortcode)
        append("\"")
        if (image.body != null) {
            append(" alt=\"")
            append(image.body)
            append("\"")
        }
        append(" />")
    }
}

sealed interface ComposerState {
    fun isEmpty(): Boolean

    data object ComposerLessTimeline : ComposerState {
        override fun isEmpty() = true
    }

    data object NoSendPermission : ComposerState {
        override fun isEmpty() = true
    }
}

data class DraftValue(
    val type: DraftType = DraftType.TEXT,
    val preferredFormat: ComposerFormat = ComposerFormat.MARKDOWN,
    val textFieldValue: TextFieldValue = TextFieldValue(""),
    val spans: ImmutableList<DraftSpan> = persistentListOf(),
    val inReplyTo: InReplyTo.Ready? = null,
    val editEventId: EventOrTransactionId? = null, // Only for DraftType.EDIT and DraftType.EDIT_CAPTION
    val isSendInProgress: Boolean = false,
    val initialBody: String = "", // For edits the original message content, else empty
    val attachment: Attachment? = null, // Mandatory for DraftType.ATTACHMENT, otherwise unused
    val customEventType: String? = null, // Only for DraftType.CUSTOM_EVENT and DraftType.CUSTOM_STATE_EVENT
    val stateKey: String? = null, // Only for DraftType.CUSTOM_STATE_EVENT
): ComposerState {
    val rawBody: String
        get() = textFieldValue.text.trim()
    val format: ComposerFormat = when (type) {
        DraftType.TEXT,
        DraftType.NOTICE,
        DraftType.EMOTE,
        DraftType.EDIT -> preferredFormat
        // TODO I haven't taught captions to not do auto-markdown on the SDK side yet
        DraftType.ATTACHMENT,
        DraftType.EDIT_CAPTION -> preferredFormat.takeIf { it != ComposerFormat.PLAIN } ?: ComposerFormat.MARKDOWN
        else -> ComposerFormat.PLAIN
    }
    val body: String
        get() = reactionCustomEmoteBody?.image?.url ?: ComposerBodyFormatter.expandDraftSpans(rawBody, spans, allowHtml = format != ComposerFormat.PLAIN)
    val htmlBody: String?
        get() = when (format) {
            // SDK generates Markdown for us
            ComposerFormat.MARKDOWN,
            ComposerFormat.PLAIN -> null
            ComposerFormat.HTML -> body
        }
    val intentionalMentions = spans.mapNotNull { (it as? DraftMention)?.mention }
    val hasRoomMention = spans.any { (it as? DraftMention)?.mention == IntentionalMention.Room }

    val fullBodyCustomEmote = if (spans.size == 1) {
        (spans.first() as? DraftCustomEmote)?.takeIf {
            it.start == 0 && it.end == textFieldValue.text.length
        }
    } else {
        null
    }
    private val reactionCustomEmoteBody = if (type == DraftType.REACTION) fullBodyCustomEmote else null
    val isValidReaction = spans.isEmpty() || fullBodyCustomEmote?.source?.supportsCustomEmoji == true
    val isValidSticker = (spans.size == 1 && fullBodyCustomEmote?.source?.supportsSticker == true)

    val allowsMention = when (type) {
        DraftType.TEXT,
        DraftType.NOTICE,
        DraftType.EMOTE,
        DraftType.ATTACHMENT,
        DraftType.EDIT,
        DraftType.EDIT_CAPTION -> true
        DraftType.REACTION,
        DraftType.STICKER,
        DraftType.CUSTOM_EVENT,
        DraftType.CUSTOM_STATE_EVENT -> false
    }
    val allowsCustomEmote = when (type) {
        DraftType.TEXT,
        DraftType.NOTICE,
        DraftType.EMOTE,
        DraftType.ATTACHMENT,
        DraftType.EDIT,
        DraftType.EDIT_CAPTION,
        DraftType.REACTION -> true
        DraftType.STICKER,
        DraftType.CUSTOM_EVENT,
        DraftType.CUSTOM_STATE_EVENT -> false
    }
    val allowsImagePackImage = type == DraftType.STICKER || allowsCustomEmote

    val shouldSendAsPlaintext: Boolean
        get() = format == ComposerFormat.PLAIN

    override fun isEmpty() = attachment?.takeIf { type == DraftType.ATTACHMENT } == null &&
            (textFieldValue.text.isBlank() || textFieldValue.text == initialBody)
    fun canSend() = !isSendInProgress && !isEmpty() && when (type) {
        DraftType.STICKER -> isValidSticker
        DraftType.REACTION -> isValidReaction
        else -> true
    }
    /** Whether an attachment can be added to the current composer state without dropping state. */
    fun canAddAttachment() = editEventId == null && type == DraftType.TEXT

    fun bodyValidationError() = when (type) {
        DraftType.CUSTOM_STATE_EVENT,
        DraftType.CUSTOM_EVENT -> {
            try {
                Json.parseToJsonElement(body)
                null
            } catch (e: SerializationException) {
                e.message
            }
        }
        else -> null
    }
}

// TODO may add some persistent storage to this one to survive restarts & crashes
object DraftRepo {
    private val drafts = MutableStateFlow<ImmutableMap<DraftKey, DraftValue>>(persistentMapOf())

    val roomsWithDrafts = drafts.map {
        it.filter { (k, v) -> !v.isEmpty() }.keys.map { ScopedRoomKey(it.sessionId, it.roomId) }.toSet()
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

    fun update(
        draftKey: DraftKey,
        allowWhileSendInProgress: Boolean = false,
        transform: (DraftValue?) -> DraftValue?,
    ): Boolean {
        var updated = false
        drafts.update {
            val oldValue = it[draftKey]
            if (oldValue?.isSendInProgress == true && !allowWhileSendInProgress) {
                updated = false
                return@update it
            }
            val value = transform(oldValue)
            updated = value != oldValue
            if (value == null) {
                it - draftKey
            } else {
                it + (draftKey to maintainAnnotations(value, it[draftKey]))
            }.toPersistentMap()
        }
        return updated
    }

    private fun maintainAnnotations(newValue: DraftValue, oldValue: DraftValue?): DraftValue {
        val newText = newValue.textFieldValue.text
        val spansToRemove = mutableSetOf<DraftSpan>()
        val spansToAdd = mutableListOf<DraftSpan>()
        oldValue?.spans?.forEach { span ->
            if (!newValue.spans.contains(span)) {
                // Was already dropped anyway
                return@forEach
            }
            val spanText = oldValue.textFieldValue.text.substring(span.range)
            val spanTextCheck = if (span.end <= newText.length)
                newText.substring(span.range)
            else
                null
            if (spanText == spanTextCheck) {
                // Still applicable
                return@forEach
            } else {
                spansToRemove.add(span)
                // Check if text was just moved?
                val newIndex = newText.indexOf(spanText)
                if (newIndex >= 0) {
                    val newSpan = span.withAdjustedRange(start = newIndex, end = newIndex + spanText.length)
                    // Avoid duplicates
                    if (newSpan !in newValue.spans) {
                        spansToAdd.add(newSpan)
                    }
                }
            }
        }
        return if (spansToAdd.isEmpty() && spansToRemove.isEmpty() && newValue.spans.isEmpty()) {
            newValue
        } else {
            val spans = (newValue.spans - spansToRemove + spansToAdd).toImmutableList()
            newValue.copy(
                spans = spans,
                textFieldValue = newValue.textFieldValue.copy(
                    annotatedString = buildAnnotatedString {
                        append(newText)
                        spans.forEach { span ->
                            addStyle(
                                span.draftStyle(),
                                start = span.start,
                                end = span.end,
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

    fun lookupDraft(draftKey: DraftKey?) = draftKey?.let { drafts.value[draftKey] }

    fun followDraft(draftKey: DraftKey) = drafts.map {
        it[draftKey]
    }

    fun followComposerState(draftKey: DraftKey?, permissions: Flow<ConversationPermissions?>): Flow<ComposerState?> {
        return if (draftKey == null) {
            flowOf(ComposerState.ComposerLessTimeline)
        } else {
            combine(
                drafts,
               permissions
            ) { drafts, permissions ->
                val draft = drafts[draftKey]
                val draftAllowed = when (draft?.type) {
                    DraftType.REACTION -> permissions?.canSendReactions != false
                    else -> permissions?.canSendMessages != false
                }
                if (draftAllowed) {
                    draft
                } else {
                    ComposerState.NoSendPermission
                }
            }
        }
    }
}
