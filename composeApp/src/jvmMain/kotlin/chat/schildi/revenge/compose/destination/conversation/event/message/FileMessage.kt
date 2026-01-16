package chat.schildi.revenge.compose.destination.conversation.event.message

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.VoiceChat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.model.conversation.MessageMetadata
import com.beeper.android.messageformat.MatrixBodyParseResult
import io.element.android.libraries.matrix.api.timeline.item.event.AudioMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.FileMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.ImageMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.InReplyTo
import io.element.android.libraries.matrix.api.timeline.item.event.MessageTypeWithAttachment
import io.element.android.libraries.matrix.api.timeline.item.event.StickerMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.VideoMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.VoiceMessageType

enum class FileMessageRenderType(val icon: ImageVector) {
    FILE(Icons.Default.AttachFile),
    IMAGE(Icons.Default.Image),
    STICKER(Icons.Default.Image),
    VIDEO(Icons.Default.VideoFile),
    AUDIO(Icons.Default.AudioFile),
    VOICE(Icons.Default.VoiceChat),
}

@Composable
fun FileMessage(
    file: MessageTypeWithAttachment,
    messageMetadata: MessageMetadata?,
    isOwn: Boolean,
    timestamp: TimestampOverlayContent?,
    inReplyTo: InReplyTo?,
    modifier: Modifier = Modifier,
) {
    FileMessage(
        type = when (file) {
            is AudioMessageType -> FileMessageRenderType.AUDIO
            is FileMessageType -> FileMessageRenderType.FILE
            is ImageMessageType -> FileMessageRenderType.IMAGE
            is StickerMessageType -> FileMessageRenderType.STICKER
            is VideoMessageType -> FileMessageRenderType.VIDEO
            is VoiceMessageType -> FileMessageRenderType.VOICE
        },
        messageMetadata = messageMetadata,
        filename = file.filename,
        caption = file.caption,
        isOwn = isOwn,
        timestamp = timestamp,
        inReplyTo = inReplyTo,
        modifier = modifier,
    )
}

@Composable
fun FileMessage(
    type: FileMessageRenderType,
    messageMetadata: MessageMetadata?,
    filename: String,
    caption: String?,
    isOwn: Boolean,
    timestamp: TimestampOverlayContent?,
    inReplyTo: InReplyTo?,
    modifier: Modifier = Modifier,
) {
    val captionLayoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
    MessageBubble(
        isOwn = isOwn,
        timestamp = timestamp,
        modifier = modifier,
        padding = PaddingValues(Dimens.Conversation.messageBubbleInnerPadding),
        contentTextLayoutResult = captionLayoutResult.value,
        verticalArrangement = Arrangement.spacedBy(Dimens.Conversation.captionPadding),
        nonTextWidth = if (caption == null) Dimens.Conversation.fileIconSize + Dimens.horizontalItemPadding else 0.dp,
    ) {
        inReplyTo?.let {
            ReplyContent(it)
        }
        FileMessageContent(
            type = type,
            messageMetadata = messageMetadata,
            filename = filename,
            // TODO formatted caption
            caption = caption?.let { AnnotatedString(it) },
        ) {
            captionLayoutResult.value = it
        }
    }
}

@Composable
fun ColumnScope.FileMessageContent(
    type: FileMessageRenderType,
    messageMetadata: MessageMetadata?,
    filename: String,
    caption: AnnotatedString? = null,
    onCaptionTextLayout: (TextLayoutResult?) -> Unit = {},
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Dimens.horizontalArrangement) {
        Icon(
            type.icon,
            null,
            modifier = Modifier
                .size(Dimens.Conversation.fileIconSize)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                .padding(Dimens.Conversation.messageBubbleInnerPadding)
        )
        TextLikeMessageContent(
            MatrixBodyParseResult(filename),
            allowBigEmojiOnly = false,
            onTextLayout = if (caption == null) onCaptionTextLayout else {{}},
        )
    }
    if (caption != null) {
        TextLikeMessageContent(
            messageMetadata?.preFormattedContent ?: MatrixBodyParseResult(caption),
            allowBigEmojiOnly = false,
            modifier = Modifier.padding(top = Dimens.Conversation.captionPadding),
            onTextLayout = onCaptionTextLayout,
        )
    }
}
