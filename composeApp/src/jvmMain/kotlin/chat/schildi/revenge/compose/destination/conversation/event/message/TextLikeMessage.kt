package chat.schildi.revenge.compose.destination.conversation.event.message

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import chat.schildi.matrixsdk.urlpreview.UrlPreviewInfo
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.compose.media.imageLoader
import chat.schildi.revenge.compose.util.containsOnlyEmojis
import chat.schildi.revenge.model.conversation.MessageMetadata
import co.touchlab.kermit.Logger
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
        TextLikeMessageContent(text, allowBigEmojiOnly, textColor = textColor) {
            textLayoutResult.value = it
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
                        info.alt ?: info.title ?: "\uFFFD",
                        modifier,
                        style = textStyle,
                        color = textColor,
                    )
                }
            }
        }
    }
}
