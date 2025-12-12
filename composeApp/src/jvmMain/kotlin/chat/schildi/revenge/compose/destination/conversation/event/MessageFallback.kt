package chat.schildi.revenge.compose.destination.conversation.event;

import androidx.compose.runtime.Composable;
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import chat.schildi.revenge.compose.destination.conversation.event.message.TextLikeMessage
import io.element.android.libraries.matrix.api.timeline.item.event.InReplyTo

@Composable
fun MessageFallback(
    text: String,
    isOwn: Boolean,
    inReplyTo: InReplyTo?,
    modifier: Modifier = Modifier
) {
    val formatted = buildAnnotatedString {
        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
            append(text)
        }
    }
    TextLikeMessage(formatted, isOwn, inReplyTo, modifier)
}
