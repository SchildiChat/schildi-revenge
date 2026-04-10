package chat.schildi.revenge.compose.destination.conversation.event.message

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import chat.schildi.matrixsdk.urlpreview.UrlPreviewInfo
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.LocalMatrixBodyDrawStyle
import chat.schildi.revenge.LocalMatrixBodyFormatter
import chat.schildi.revenge.MessageFormatDefaults
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.compose.media.rememberAnimatedImageTransform
import chat.schildi.revenge.compose.media.imageLoader
import chat.schildi.revenge.compose.util.containsOnlyEmojis
import chat.schildi.revenge.model.conversation.MessageMetadata
import coil3.PlatformContext
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.maxBitmapSize
import coil3.size.Size
import com.beeper.android.messageformat.InlineImageInfo
import com.beeper.android.messageformat.MatrixBodyParseResult
import com.beeper.android.messageformat.MatrixFormatInteractionState
import com.beeper.android.messageformat.MatrixStyledFormattedText
import com.beeper.android.messageformat.rememberMatrixFormatInteractionState
import com.beeper.android.messageformat.toInlineContent
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.api.timeline.item.EventThreadInfo
import io.element.android.libraries.matrix.api.timeline.item.event.EmoteMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.InReplyTo
import io.element.android.libraries.matrix.api.timeline.item.event.NoticeMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.TextLikeMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.TextMessageType
import io.element.android.libraries.matrix.ui.media.MediaRequestData
import kotlin.math.roundToInt

private const val INLINE_IMG_MIN_WIDTH_DP = 16
private const val INLINE_IMG_MAX_WIDTH_DP = 256
private const val INLINE_IMG_MIN_HEIGHT_DP = 16
private const val INLINE_IMG_MAX_HEIGHT_DP = 256

@Composable
fun TextLikeMessage(
    message: TextLikeMessageType,
    messageMetadata: MessageMetadata?,
    isOwn: Boolean,
    timestamp: TimestampOverlayContent?,
    inReplyTo: InReplyTo?,
    threadInfo: EventThreadInfo?,
    modifier: Modifier = Modifier,
    allowBigEmojiOnly: Boolean = true,
    interactionState: MatrixFormatInteractionState? = null,
) {
    val alpha = when (message) {
        is NoticeMessageType -> 0.7f
        is EmoteMessageType,
        is TextMessageType -> 1f
    }
    val text = messageMetadata?.preFormattedContent ?: MatrixBodyParseResult(message.body)
    TextLikeMessage(
        text = text,
        isOwn = isOwn,
        timestamp = timestamp,
        inReplyTo = inReplyTo,
        threadInfo = threadInfo,
        modifier = modifier.alpha(alpha),
        urlPreview = resolveUrlPreview(text),
        allowBigEmojiOnly = allowBigEmojiOnly,
        outlined = message is EmoteMessageType,
        interactionState = interactionState ?: rememberMatrixFormatInteractionState(text),
    )
}

@Composable
fun TextLikeMessage(
    text: MatrixBodyParseResult,
    isOwn: Boolean,
    timestamp: TimestampOverlayContent?,
    inReplyTo: InReplyTo?,
    threadInfo: EventThreadInfo?,
    modifier: Modifier = Modifier,
    urlPreview: UrlPreviewInfo? = null,
    allowBigEmojiOnly: Boolean = true,
    outlined: Boolean = false,
    textColor: Color = MaterialTheme.colorScheme.primary,
    interactionState: MatrixFormatInteractionState = rememberMatrixFormatInteractionState(text),
) {
    val textLayoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
    MessageBubble(
        isOwn = isOwn,
        timestamp = timestamp,
        modifier = modifier,
        outlined = outlined,
        contentTextLayoutResult = textLayoutResult.value,
    ) {
        inReplyTo?.let { ReplyContent(it, threadInfo) }
        urlPreview?.let {
            val keyHandler = LocalKeyboardActionHandler.current
            UrlPreviewView(urlPreview.preview) {
                keyHandler.openLinkInExternalBrowser(urlPreview.url)
            }
        }
        SelectionContainer {
            TextLikeMessageContent(
                text,
                textColor = textColor,
                allowBigEmojiOnly = allowBigEmojiOnly,
                interactionState = interactionState,
            ) {
                textLayoutResult.value = it
            }
        }
    }
}

@Composable
fun TextLikeMessageContent(
    text: MatrixBodyParseResult,
    modifier: Modifier = Modifier,
    allowBigEmojiOnly: Boolean = false,
    textColor: Color = MaterialTheme.colorScheme.primary,
    interactionState: MatrixFormatInteractionState = rememberMatrixFormatInteractionState(text),
    onTextLayout: (TextLayoutResult) -> Unit = {},
) {
    val isEmojiOnly = allowBigEmojiOnly && LocalMessageRenderContext.current == MessageRenderContext.NORMAL &&
            remember(text) {
                text.toString().containsOnlyEmojis()
            }
    val textStyle = if (isEmojiOnly) {
        Dimens.Conversation.emojiOnlyMessageStyle
    } else {
        Dimens.Conversation.textMessageStyle
    }
    TextLikeMessageContent(
        text = text,
        modifier = modifier,
        textColor = textColor,
        textStyle = textStyle,
        interactionState = interactionState,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun TextLikeMessageContent(
    text: MatrixBodyParseResult,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.primary,
    textAlign: TextAlign? = null,
    interactionState: MatrixFormatInteractionState = rememberMatrixFormatInteractionState(text),
    onTextLayout: (TextLayoutResult) -> Unit = {},
) {
    val blockQuotes = remember(text) {
        text.text.getStringAnnotations("mx:BLOCK_QUOTE", 0, text.text.length)
    }
    if (blockQuotes.isNotEmpty()) {
        // HACK - blockquotes are dumb on JVM, they tend to forget the indent if there's no soft wrap
        IndentionHackFormattedText(
            blockQuotes = blockQuotes,
            text = text,
            textStyle = textStyle,
            modifier = modifier,
            textColor = textColor,
            onTextLayout = onTextLayout,
            textAlign = textAlign,
        )
        return
    }
    MatrixStyledFormattedText(
        text,
        color = textColor,
        style = textStyle,
        modifier = modifier,
        formatter = LocalMatrixBodyFormatter.current,
        drawStyle = LocalMatrixBodyDrawStyle.current,
        interactionState = interactionState,
        onTextLayout = onTextLayout,
        maxLines = if (LocalMessageRenderContext.current == MessageRenderContext.IN_REPLY_TO) {
            20
        } else {
            Integer.MAX_VALUE
        },
        overflow = TextOverflow.Ellipsis,
        inlineContent = text.inlineImages.toInlineContent(textStyle, textColor),
        textAlign = textAlign,
    )
}

@Composable
fun IndentionHackFormattedText(
    blockQuotes: List<AnnotatedString.Range<String>>,
    text: MatrixBodyParseResult,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.primary,
    textAlign: TextAlign? = null,
    onTextLayout: (TextLayoutResult) -> Unit,
) {
    BoxWithConstraints(modifier) {
        var textWidth by remember(maxWidth) { mutableIntStateOf(-1) }
        val density = LocalDensity.current
        val maxDepth = remember(blockQuotes) {
            blockQuotes.maxOfOrNull {
                it.item.toIntOrNull() ?: 1
            } ?: 1
        }
        MatrixStyledFormattedText(
            text,
            color = textColor,
            style = textStyle,
            modifier =
                // HACK - blockquotes are dumb on JVM, they tend to forget the indent if there's no soft wrap
                // and no sufficient fixed width
                textWidth.takeIf { it > 0 }?.let {
                    val forcedWidth = min(
                        maxWidth,
                        density.run { textWidth.toDp() + MessageFormatDefaults.blockIndention.toDp() * maxDepth }
                    )
                    Modifier.width(forcedWidth)
                } ?: Modifier,
            formatter = LocalMatrixBodyFormatter.current,
            drawStyle = LocalMatrixBodyDrawStyle.current,
            onTextLayout = {
                onTextLayout(it)
                if (textWidth == -1) {
                    textWidth = it.size.width
                }
            },
            inlineContent = text.inlineImages.toInlineContent(textStyle, textColor),
            textAlign = textAlign,
        )
    }
}

@Composable
private fun Map<String, InlineImageInfo>.toInlineContent(
    textStyle: TextStyle,
    textColor: Color,
): Map<String, InlineTextContent> {
    val inlineImageSizes = remember { mutableStateMapOf<String, IntSize>() }
    return toInlineContent(
        density = LocalDensity.current,
        defaultHeight = textStyle.lineHeight,
        actualImageSizes = inlineImageSizes,
        minWidth = INLINE_IMG_MIN_WIDTH_DP.dp,
        maxWidth = INLINE_IMG_MAX_WIDTH_DP.dp,
        minHeight = INLINE_IMG_MIN_HEIGHT_DP.dp,
        maxHeight = INLINE_IMG_MAX_HEIGHT_DP.dp,
    ) { info, modifier ->
        InlineImage(info, textStyle, textColor, modifier) {
            inlineImageSizes[info.uri] = IntSize(it.image.width, it.image.height)
        }
    }
}

@Composable
private fun InlineImage(
    info: InlineImageInfo,
    textStyle: TextStyle,
    textColor: Color,
    modifier: Modifier = Modifier,
    onPainterSuccess: (SuccessResult) -> Unit = {},
) {
    val density = LocalDensity.current
    SubcomposeAsyncImage(
        modifier = modifier,
        imageLoader = imageLoader(),
        model = ImageRequest
            .Builder(PlatformContext.INSTANCE)
            .data(MediaRequestData(MediaSource(info.uri), MediaRequestData.Kind.Content))
            .size(Size.ORIGINAL)
            .maxBitmapSize(
                Size(
                    width = (INLINE_IMG_MAX_WIDTH_DP * density.density.coerceIn(1f, 4f)).roundToInt(),
                    height = (INLINE_IMG_MAX_HEIGHT_DP * density.density.coerceIn(1f, 4f)).roundToInt(),
                )
            )
            .build(),
        transform = rememberAnimatedImageTransform(),
        contentScale = ContentScale.Fit,
        alignment = Alignment.Center,
        contentDescription = info.alt ?: info.title,
    ) {
        AnimatedContent(
            painter.state.collectAsState().value,
            transitionSpec = {
                fadeIn(
                    animationSpec = Dimens.tween()
                ) togetherWith fadeOut(
                    animationSpec = Dimens.tween()
                )
            },
        ) { state ->
            when (state) {
                is AsyncImagePainter.State.Success -> {
                    SubcomposeAsyncImageContent(modifier)
                    LaunchedEffect(state.result) {
                        onPainterSuccess(state.result)
                    }
                }
                else -> {
                    Text(
                        info.alt ?: info.title ?: MessageFormatDefaults.INLINE_IMAGE_PLACEHOLDER,
                        modifier,
                        style = textStyle,
                        color = textColor,
                    )
                }
            }
        }
    }
}
