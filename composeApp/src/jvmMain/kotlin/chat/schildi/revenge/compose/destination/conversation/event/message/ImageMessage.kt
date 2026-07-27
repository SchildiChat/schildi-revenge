package chat.schildi.revenge.compose.destination.conversation.event.message

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import chat.schildi.lib.preferences.ScPrefs
import chat.schildi.revenge.preferences.value
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.compose.media.BlurHashPlaceholder
import chat.schildi.revenge.compose.media.rememberAnimatedImageTransform
import chat.schildi.revenge.compose.media.imageLoader
import chat.schildi.revenge.compose.media.onAsyncImageState
import coil3.compose.AsyncImagePainter
import chat.schildi.revenge.model.conversation.MessageMetadata
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.beeper.android.messageformat.MatrixBodyParseResult
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.api.timeline.item.EventThreadInfo
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
    threadInfo: EventThreadInfo?,
    modifier: Modifier = Modifier,
) {
    ImageMessage(
        source = image.source,
        blurhash = image.info?.blurhash,
        width = image.info?.width,
        height = image.info?.height,
        messageMetadata = messageMetadata,
        isOwn = isOwn,
        timestamp = timestamp,
        inReplyTo = inReplyTo,
        threadInfo = threadInfo,
        isSticker = image is StickerMessageType,
        modifier = modifier,
    )
}

@Composable
fun ImageMessage(
    source: MediaSource,
    blurhash: String? = null,
    width: Long? = null,
    height: Long? = null,
    messageMetadata: MessageMetadata?,
    isOwn: Boolean,
    timestamp: TimestampOverlayContent?,
    inReplyTo: InReplyTo?,
    threadInfo: EventThreadInfo?,
    isSticker: Boolean,
    modifier: Modifier = Modifier,
) {
    val captionLayoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
    val isTransparent = isSticker && messageMetadata?.preFormattedContent == null && inReplyTo == null
    MessageBubble(
        isOwn = isOwn,
        timestamp = timestamp,
        modifier = modifier,
        padding = PaddingValues(Dimens.Conversation.imageBubbleInnerPadding),
        contentTextLayoutResult = captionLayoutResult.value,
        isMediaOverlay = true,
        transparent = isTransparent,
        allowTimestampOverlay = messageMetadata?.preFormattedContent?.allowTimestampOverlay() != false,
    ) {
        inReplyTo?.let {
            ReplyContent(
                it,
                threadInfo,
                Modifier.padding(Dimens.Conversation.messageBubbleInnerPadding),
            )
        }
        val topRadius = if (inReplyTo == null) Dimens.Conversation.messageBubbleCornerRadius else 0.dp
        val bottomRadius = if (messageMetadata?.preFormattedContent == null)
            Dimens.Conversation.messageBubbleCornerRadius
        else
            0.dp
        val shape = RoundedCornerShape(
            topStart = topRadius,
            topEnd = topRadius,
            bottomStart = bottomRadius,
            bottomEnd = bottomRadius,
        )
        ImageMessageContent(
            model = MediaRequestData(source, MediaRequestData.Kind.Content),
            blurhash = blurhash,
            width = width,
            height = height,
            minWidth = Dimens.Conversation.imageMinWidth,
            minHeight = Dimens.Conversation.imageMinHeight,
            maxWidth = Dimens.Conversation.imageMaxWidth,
            maxHeight = when (LocalMessageRenderContext.current) {
                MessageRenderContext.NORMAL -> Dimens.Conversation.imageMaxHeight
                MessageRenderContext.IN_REPLY_TO -> Dimens.Conversation.imageRepliedToMaxHeight
            },
            caption = messageMetadata?.preFormattedContent,
            shape = shape,
            onCaptionTextLayout = { captionLayoutResult.value = it }
        )
    }
}

@Composable
fun ColumnScope.ImageMessageContent(
    model: Any,
    blurhash: String? = null,
    width: Long? = null,
    height: Long? = null,
    minWidth: Dp,
    minHeight: Dp,
    maxWidth: Dp,
    maxHeight: Dp,
    caption: MatrixBodyParseResult? = null,
    shape: Shape = Dimens.Conversation.messageBubbleShape,
    onCaptionTextLayout: (TextLayoutResult?) -> Unit = {},
    overlay: @Composable () -> Unit = {},
) {
    Box(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        contentAlignment = Alignment.Center,
    ) {
        SubcomposeAsyncImage(
            model = model,
            contentDescription = null,
            imageLoader = imageLoader(),
            onState = ::onAsyncImageState,
            transform = rememberAnimatedImageTransform(),
            filterQuality = FilterQuality.High,
            contentScale = ContentScale.Fit,
            modifier = Modifier.clip(shape),
        ) {
            if (blurhash != null && ScPrefs.FORCE_RENDER_BLURHASH.value()) {
                BlurHashPlaceholder(
                    blurHash = blurhash,
                    width = width,
                    height = height,
                    modifier = Modifier.boundedMediaSize(
                        minWidth,
                        minHeight,
                        maxWidth,
                        maxHeight,
                        aspectRatioOrNull(width, height),
                    ),
                )
                return@SubcomposeAsyncImage
            }
            when (painter.state.collectAsState().value) {
                is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent(
                    modifier = Modifier.boundedMediaSize(
                        minWidth,
                        minHeight,
                        maxWidth,
                        maxHeight,
                        painter.intrinsicSize.width / painter.intrinsicSize.height,
                    ),
                )
                AsyncImagePainter.State.Empty,
                is AsyncImagePainter.State.Loading,
                is AsyncImagePainter.State.Error -> {
                    BlurHashPlaceholder(
                        blurHash = blurhash,
                        width = width,
                        height = height,
                        modifier = Modifier.boundedMediaSize(
                            minWidth,
                            minHeight,
                            maxWidth,
                            maxHeight,
                            aspectRatioOrNull(width, height),
                        ),
                    )
                }
            }
        }
        overlay()
    }
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

private fun aspectRatioOrNull(width: Long?, height: Long?): Float? {
    val safeWidth = width?.takeIf { it > 0 } ?: return null
    val safeHeight = height?.takeIf { it > 0 } ?: return null
    return safeWidth.toFloat() / safeHeight
}

private fun Modifier.boundedMediaSize(
    minWidth: Dp,
    minHeight: Dp,
    maxWidth: Dp,
    maxHeight: Dp,
    aspectRatio: Float? = null,
): Modifier = sizeIn(
    minWidth = minWidth,
    minHeight = minHeight,
    maxWidth = maxWidth,
    maxHeight = maxHeight,
).then(
    aspectRatio
        ?.takeIf { it.isFinite() && it > 0f }
        ?.let { Modifier.aspectRatio(it) }
        ?: Modifier,
)
