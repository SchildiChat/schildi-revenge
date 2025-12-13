package chat.schildi.revenge.compose.destination.conversation.event.message

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chat.schildi.revenge.Dimens
import chat.schildi.theme.scExposures

@Composable
fun MessageBubble(
    isOwn: Boolean,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(Dimens.Conversation.messageBubbleInnerPadding),
    outlined: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    // Bypass double message bubble when nested in a reply
    if (LocalMessageRenderContext.current == MessageRenderContext.IN_REPLY_TO) {
        Column(modifier.padding(padding), content = content)
    } else {
        val color = if (isOwn)
            MaterialTheme.scExposures.bubbleBgOutgoing
        else
            MaterialTheme.scExposures.bubbleBgIncoming
        Column(
            modifier
                .then(
                    if (outlined) {
                        Modifier.border(
                            width = 1.dp,
                            color = color,
                            shape = Dimens.Conversation.messageBubbleShape,
                        )
                    } else {
                        Modifier.background(
                            color = color,
                            shape = Dimens.Conversation.messageBubbleShape,
                        )
                    }
                )
                .padding(padding),
            content = content
        )
    }
}
