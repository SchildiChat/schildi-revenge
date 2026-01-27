package chat.schildi.revenge.compose.destination.conversation.event;

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable;
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import chat.schildi.revenge.compose.destination.conversation.event.message.TextLikeMessage
import chat.schildi.revenge.compose.destination.conversation.event.message.TimestampOverlayContent
import com.beeper.android.messageformat.MatrixBodyParseResult
import io.element.android.libraries.matrix.api.timeline.item.event.InReplyTo

@Composable
fun MessageFallback(
    text: String,
    isOwn: Boolean,
    timestamp: TimestampOverlayContent?,
    inReplyTo: InReplyTo?,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.secondary,
) {
    // TODO make clearer that this is a fallback and not actual message content?
    val formatted = buildAnnotatedString {
        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
            append(text)
        }
    }
    TextLikeMessage(
        MatrixBodyParseResult(formatted),
        isOwn,
        timestamp,
        inReplyTo,
        modifier,
        textColor = textColor,
    )
}
