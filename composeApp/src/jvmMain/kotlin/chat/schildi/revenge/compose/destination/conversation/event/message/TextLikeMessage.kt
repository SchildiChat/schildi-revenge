package chat.schildi.revenge.compose.destination.conversation.event.message

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.AnnotatedString
import io.element.android.libraries.matrix.api.timeline.item.event.EmoteMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.InReplyTo
import io.element.android.libraries.matrix.api.timeline.item.event.NoticeMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.TextLikeMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.TextMessageType

@Composable
fun TextLikeMessage(
    message: TextLikeMessageType,
    isOwn: Boolean,
    inReplyTo: InReplyTo?,
    modifier: Modifier = Modifier
) {
    val alpha = when (message) {
        is TextMessageType -> 1f
        is NoticeMessageType -> 0.7f
        is EmoteMessageType -> 0f // TODO?
    }
    // TODO
    TextLikeMessage(
        // TODO message formatting, links, url previews, ...
        text = AnnotatedString(message.body),
        isOwn = isOwn,
        inReplyTo = inReplyTo,
        modifier = modifier.alpha(alpha),
    )
}

@Composable
fun TextLikeMessage(
    text: AnnotatedString,
    isOwn: Boolean,
    inReplyTo: InReplyTo?,
    modifier: Modifier = Modifier
) {
    MessageBubble(
        isOwn = isOwn,
        modifier = modifier,
    ) {
        inReplyTo?.let { ReplyContent(it) }
        TextLikeMessageContent(text)
    }
}

@Composable
fun TextLikeMessageContent(text: AnnotatedString, modifier: Modifier = Modifier) {
    SelectionContainer {
        Text(
            text,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier,
        )
    }
}
