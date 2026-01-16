package chat.schildi.revenge.compose.destination.conversation.event.message

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.compose.media.imageLoader
import chat.schildi.revenge.compose.media.onAsyncImageError
import chat.schildi.revenge.model.conversation.MessageMetadata
import coil3.compose.AsyncImage
import com.beeper.android.messageformat.MatrixBodyParseResult
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.api.timeline.item.event.ImageLikeMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.InReplyTo
import io.element.android.libraries.matrix.api.timeline.item.event.StickerMessageType
import io.element.android.libraries.matrix.ui.media.MediaRequestData

@Composable
fun ImageMessage(
    image: ImageLikeMessageType,
    messageMetadata: MessageMetadata?,
    isOwn: Boolean,
    timestamp: TimestampOverlayContent?,
    inReplyTo: InReplyTo?,
    modifier: Modifier = Modifier,
) {
    ImageMessage(
        source = image.source,
        messageMetadata = messageMetadata,
        caption = image.caption,
        isOwn = isOwn,
        timestamp = timestamp,
        inReplyTo = inReplyTo,
        isSticker = image is StickerMessageType,
        modifier = modifier,
    )
}

@Composable
fun ImageMessage(
    source: MediaSource,
    messageMetadata: MessageMetadata?,
    caption: String?,
    isOwn: Boolean,
    timestamp: TimestampOverlayContent?,
    inReplyTo: InReplyTo?,
    isSticker: Boolean,
    modifier: Modifier = Modifier,
) {
    val captionLayoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
    val isTransparent = isSticker && caption == null && inReplyTo == null
    MessageBubble(
        isOwn = isOwn,
        timestamp = timestamp,
        modifier = modifier,
        padding = PaddingValues(Dimens.Conversation.imageBubbleInnerPadding),
        contentTextLayoutResult = captionLayoutResult.value,
        isMediaOverlay = true,
        transparent = isTransparent,
    ) {
        inReplyTo?.let {
            ReplyContent(
                it,
                Modifier.padding(Dimens.Conversation.messageBubbleInnerPadding),
            )
        }
        val topRadius = if (inReplyTo == null) Dimens.Conversation.messageBubbleCornerRadius else 0.dp
        // TODO formatted caption
        val bottomRadius = if (caption == null) Dimens.Conversation.messageBubbleCornerRadius else 0.dp
        val shape = RoundedCornerShape(
            topStart = topRadius,
            topEnd = topRadius,
            bottomStart = bottomRadius,
            bottomEnd = bottomRadius,
        )
        ImageMessageContent(
            model = MediaRequestData(source, MediaRequestData.Kind.Content),
            minWidth = Dimens.Conversation.imageMinWidth,
            minHeight = Dimens.Conversation.imageMinHeight,
            maxWidth = Dimens.Conversation.imageMaxWidth,
            maxHeight = when (LocalMessageRenderContext.current) {
                MessageRenderContext.NORMAL -> Dimens.Conversation.imageMaxHeight
                MessageRenderContext.IN_REPLY_TO -> Dimens.Conversation.imageRepliedToMaxHeight
            },
            caption = caption?.let { messageMetadata?.preFormattedContent ?: MatrixBodyParseResult(it) },
            shape = shape,
        ) {
            captionLayoutResult.value = it
        }
    }
}

@Composable
fun ColumnScope.ImageMessageContent(
    model: Any,
    minWidth: Dp,
    minHeight: Dp,
    maxWidth: Dp,
    maxHeight: Dp,
    caption: MatrixBodyParseResult? = null,
    shape: Shape = Dimens.Conversation.messageBubbleShape,
    onCaptionTextLayout: (TextLayoutResult?) -> Unit = {},
) {
    // TODO placeholder, ...
    AsyncImage(
        model,
        null,
        imageLoader = imageLoader(),
        onError = ::onAsyncImageError,
        filterQuality = FilterQuality.High,
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .sizeIn(
                minWidth = minWidth,
                minHeight = minHeight,
                maxWidth = maxWidth,
                maxHeight = maxHeight,
            )
            .clip(shape),
    )
    if (caption != null) {
        TextLikeMessageContent(
            caption,
            allowBigEmojiOnly = false,
            modifier = Modifier.padding(
                top = Dimens.Conversation.captionPadding,
                bottom = Dimens.Conversation.messageBubbleInnerPadding,
                start = Dimens.Conversation.messageBubbleInnerPadding,
                end = Dimens.Conversation.messageBubbleInnerPadding,
            ),
            onTextLayout = onCaptionTextLayout,
        )
    } else {
        LaunchedEffect(Unit) {
            onCaptionTextLayout(null)
        }
    }
}
