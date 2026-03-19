package chat.schildi.revenge.compose.destination.conversation.event.message

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.model.conversation.MessageMetadata
import com.beeper.android.messageformat.MatrixBodyParseResult
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.api.timeline.item.event.InReplyTo
import io.element.android.libraries.matrix.api.timeline.item.event.VideoMessageType
import io.element.android.libraries.matrix.ui.media.MediaRequestData

@Composable
fun VideoMessage(
    video: VideoMessageType,
    messageMetadata: MessageMetadata?,
    isOwn: Boolean,
    timestamp: TimestampOverlayContent?,
    inReplyTo: InReplyTo?,
    modifier: Modifier = Modifier,
) {
    VideoMessage(
        source = video.info?.thumbnailSource ?: video.source,
        messageMetadata = messageMetadata,
        caption = video.caption,
        isOwn = isOwn,
        timestamp = timestamp,
        inReplyTo = inReplyTo,
        modifier = modifier,
    )
}

@Composable
fun VideoMessage(
    source: MediaSource,
    messageMetadata: MessageMetadata?,
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
        padding = PaddingValues(Dimens.Conversation.imageBubbleInnerPadding),
        contentTextLayoutResult = captionLayoutResult.value,
        isMediaOverlay = true,
    ) {
        inReplyTo?.let {
            ReplyContent(
                it,
                Modifier.padding(Dimens.Conversation.messageBubbleInnerPadding),
            )
        }
        val topRadius = if (inReplyTo == null) Dimens.Conversation.messageBubbleCornerRadius else 0.dp
        val bottomRadius = if (caption == null && messageMetadata?.preFormattedContent == null)
            Dimens.Conversation.messageBubbleCornerRadius
        else
            0.dp
        val shape = RoundedCornerShape(
            topStart = topRadius,
            topEnd = topRadius,
            bottomStart = bottomRadius,
            bottomEnd = bottomRadius,
        )
        VideoMessageContent(
            model = MediaRequestData(source, MediaRequestData.Kind.Thumbnail(1000)),
            minWidth = Dimens.Conversation.imageMinWidth,
            minHeight = Dimens.Conversation.imageMinHeight,
            maxWidth = Dimens.Conversation.imageMaxWidth,
            maxHeight = when (LocalMessageRenderContext.current) {
                MessageRenderContext.NORMAL -> Dimens.Conversation.imageMaxHeight
                MessageRenderContext.IN_REPLY_TO -> Dimens.Conversation.imageRepliedToMaxHeight
            },
            caption = caption?.let { messageMetadata?.preFormattedContent ?: MatrixBodyParseResult(it) },
            shape = shape,
            onCaptionTextLayout = { captionLayoutResult.value = it }
        )
    }
}

@Composable
fun ColumnScope.VideoMessageContent(
    model: Any,
    minWidth: Dp,
    minHeight: Dp,
    maxWidth: Dp,
    maxHeight: Dp,
    caption: MatrixBodyParseResult? = null,
    shape: Shape = Dimens.Conversation.messageBubbleShape,
    onCaptionTextLayout: (TextLayoutResult?) -> Unit = {},
) {
    ImageMessageContent(
        model = model,
        minWidth = minWidth,
        minHeight = minHeight,
        maxWidth = maxWidth,
        maxHeight = maxHeight,
        caption = caption,
        shape = shape,
        onCaptionTextLayout = onCaptionTextLayout,
    ) {
        VideoContentOverlay()
    }
}

@Composable
fun VideoContentOverlay(modifier: Modifier = Modifier) {
    Icon(
        Icons.Default.PlayArrow,
        null,
        modifier.size(32.dp).background(MaterialTheme.colorScheme.onSurface, CircleShape).padding(4.dp),
        tint = MaterialTheme.colorScheme.surface,
    )
}
