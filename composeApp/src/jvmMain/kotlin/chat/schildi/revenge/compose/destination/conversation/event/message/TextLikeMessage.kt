package chat.schildi.revenge.compose.destination.conversation.event.message

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import chat.schildi.revenge.Dimens
import chat.schildi.theme.scExposures
import io.element.android.libraries.matrix.api.timeline.item.event.NoticeMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.TextLikeMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.TextMessageType

@Composable
fun TextLikeMessage(message: TextLikeMessageType, isOwn: Boolean, modifier: Modifier = Modifier) {
    val alpha = when (message) {
        is TextMessageType -> 1f
        is NoticeMessageType -> 0.7f
    }
    // TODO
    Box(
        modifier
            .background(
                color = if (isOwn)
                    MaterialTheme.scExposures.bubbleBgOutgoing
                else
                    MaterialTheme.scExposures.bubbleBgIncoming,
                shape = Dimens.Conversation.messageBubbleShape,
            )
            .padding(Dimens.Conversation.messageBubbleInnerPadding)
            .alpha(alpha)
    ) {
        // TODO message formatting, links, url previews, ...
        SelectionContainer {
            Text(
                message.body,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
