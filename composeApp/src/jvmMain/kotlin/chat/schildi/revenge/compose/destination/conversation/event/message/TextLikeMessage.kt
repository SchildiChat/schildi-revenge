package chat.schildi.revenge.compose.destination.conversation.event.message

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.min
import chat.schildi.matrixsdk.urlpreview.UrlPreviewInfo
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.LocalMatrixBodyDrawStyle
import chat.schildi.revenge.LocalMatrixBodyFormatter
import chat.schildi.revenge.MessageFormatDefaults
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.compose.media.imageLoader
import chat.schildi.revenge.compose.util.containsOnlyEmojis
import chat.schildi.revenge.model.conversation.MessageMetadata
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.beeper.android.messageformat.InlineImageInfo
import com.beeper.android.messageformat.MatrixBodyParseResult
import com.beeper.android.messageformat.MatrixStyledFormattedText
import com.beeper.android.messageformat.toInlineContent
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.api.timeline.item.event.EmoteMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.InReplyTo
import io.element.android.libraries.matrix.api.timeline.item.event.NoticeMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.TextLikeMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.TextMessageType
import io.element.android.libraries.matrix.ui.media.MediaRequestData

@Composable
fun TextLikeMessage(
    message: TextLikeMessageType,
    messageMetadata: MessageMetadata?,
    isOwn: Boolean,
    timestamp: TimestampOverlayContent?,
    inReplyTo: InReplyTo?,
    modifier: Modifier = Modifier,
    allowBigEmojiOnly: Boolean = true,
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
        modifier = modifier.alpha(alpha),
        urlPreview = resolveUrlPreview(text),
        allowBigEmojiOnly = allowBigEmojiOnly,
        outlined = message is EmoteMessageType,
    )
}

@Composable
fun TextLikeMessage(
    text: MatrixBodyParseResult,
    isOwn: Boolean,
    timestamp: TimestampOverlayContent?,
    inReplyTo: InReplyTo?,
    modifier: Modifier = Modifier,
    urlPreview: UrlPreviewInfo? = null,
    allowBigEmojiOnly: Boolean = true,
    outlined: Boolean = false,
    textColor: Color = MaterialTheme.colorScheme.primary,
) {
    val textLayoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
    MessageBubble(
        isOwn = isOwn,
        timestamp = timestamp,
        modifier = modifier,
        outlined = outlined,
        contentTextLayoutResult = textLayoutResult.value,
    ) {
        inReplyTo?.let { ReplyContent(it) }
        urlPreview?.let {
            val keyHandler = LocalKeyboardActionHandler.current
            UrlPreviewView(urlPreview.preview) {
                keyHandler.openLinkInExternalBrowser(urlPreview.url)
            }
        }
        SelectionContainer {
            TextLikeMessageContent(text, allowBigEmojiOnly, textColor = textColor) {
                textLayoutResult.value = it
            }
        }
    }
}

@Composable
fun TextLikeMessageContent(
    text: MatrixBodyParseResult,
    allowBigEmojiOnly: Boolean,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.primary,
    onTextLayout: (TextLayoutResult) -> Unit,
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
        onTextLayout = onTextLayout,
        inlineContent = text.inlineImages.toInlineContent(
            density = LocalDensity.current,
            defaultHeight = textStyle.lineHeight,
        ) { info, modifier ->
            InlineImage(info, textStyle, textColor, modifier)
        }
    )
}

@Composable
fun IndentionHackFormattedText(
    blockQuotes: List<AnnotatedString.Range<String>>,
    text: MatrixBodyParseResult,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.primary,
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
            inlineContent = text.inlineImages.toInlineContent(
                density = LocalDensity.current,
                defaultHeight = textStyle.lineHeight,
            ) { info, modifier ->
                InlineImage(info, textStyle, textColor, modifier)
            }
        )
    }
}

@Composable
private fun InlineImage(
    info: InlineImageInfo,
    textStyle: TextStyle,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    SubcomposeAsyncImage(
        modifier = modifier,
        imageLoader = imageLoader(),
        model = MediaRequestData(MediaSource(info.uri), MediaRequestData.Kind.Content),
        contentScale = ContentScale.Crop,
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
                is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent(modifier)
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
