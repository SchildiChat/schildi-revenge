package chat.schildi.revenge.compose.destination.conversation.event.message

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import chat.schildi.matrixsdk.urlpreview.UrlPreviewInfo
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.compose.util.containsOnlyEmojis
import io.element.android.libraries.matrix.api.timeline.item.event.EmoteMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.InReplyTo
import io.element.android.libraries.matrix.api.timeline.item.event.NoticeMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.TextLikeMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.TextMessageType

@Composable
fun TextLikeMessage(
    message: TextLikeMessageType,
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
    // TODO
    TextLikeMessage(
        // TODO message formatting, links, ...
        text = AnnotatedString(message.body),
        isOwn = isOwn,
        timestamp = timestamp,
        inReplyTo = inReplyTo,
        modifier = modifier.alpha(alpha),
        urlPreview = resolveUrlPreview(message.body),
        allowBigEmojiOnly = allowBigEmojiOnly,
        outlined = message is EmoteMessageType,
    )
}

@Composable
fun TextLikeMessage(
    text: AnnotatedString,
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
    text: AnnotatedString,
    allowBigEmojiOnly: Boolean,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.primary,
    onTextLayout: (TextLayoutResult) -> Unit,
) {
    val isEmojiOnly = allowBigEmojiOnly && LocalMessageRenderContext.current == MessageRenderContext.NORMAL &&
         remember(text) {
            text.toString().containsOnlyEmojis()
        }
    Text(
        text,
        color = textColor,
        style = if (isEmojiOnly)
            Dimens.Conversation.emojiOnlyMessageStyle
        else
            Dimens.Conversation.textMessageStyle,
        modifier = modifier,
        onTextLayout = onTextLayout,
    )
}
