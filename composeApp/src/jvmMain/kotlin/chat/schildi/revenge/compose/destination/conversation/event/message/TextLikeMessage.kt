package chat.schildi.revenge.compose.destination.conversation.event.message

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.AnnotatedString
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
    }
    // TODO
    MessageBubble(
        isOwn = isOwn,
        modifier = modifier.alpha(alpha),
    ) {
        inReplyTo?.let { ReplyContent(it) }
        // TODO message formatting, links, url previews, ...
        TextLikeMessageContent(AnnotatedString(message.body))
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
