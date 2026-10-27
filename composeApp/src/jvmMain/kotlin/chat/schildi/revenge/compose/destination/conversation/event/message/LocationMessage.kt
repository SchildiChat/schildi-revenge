package chat.schildi.revenge.compose.destination.conversation.event.message

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import chat.schildi.revenge.model.conversation.MessageMetadata
import com.beeper.android.messageformat.MatrixBodyParseResult
import io.element.android.libraries.matrix.api.timeline.item.EventThreadInfo
import io.element.android.libraries.matrix.api.timeline.item.event.InReplyTo
import io.element.android.libraries.matrix.api.timeline.item.event.LocationMessageType
import org.jetbrains.compose.resources.stringResource
import shire.res.generated.resources.Res
import shire.res.generated.resources.message_placeholder_location

@Composable
fun LocationMessage(
    location: LocationMessageType,
    messageMetadata: MessageMetadata?,
    isOwn: Boolean,
    timestamp: TimestampOverlayContent?,
    inReplyTo: InReplyTo?,
    threadInfo: EventThreadInfo?,
    modifier: Modifier = Modifier,
) {
    // TODO use messageMetadata.preFormattedContent and render as caption/tooltip to some rendered location instead?
    val text = location.body.takeIf(String::isNotBlank) ?: stringResource(Res.string.message_placeholder_location)
    val formatted = buildAnnotatedString {
        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
            append(text)
        }
    }
    TextLikeMessage(
        text = MatrixBodyParseResult(formatted),
        isOwn = isOwn,
        timestamp = timestamp,
        inReplyTo = inReplyTo,
        threadInfo = threadInfo,
        modifier = modifier,
        textColor = MaterialTheme.colorScheme.secondary,
    )
}
