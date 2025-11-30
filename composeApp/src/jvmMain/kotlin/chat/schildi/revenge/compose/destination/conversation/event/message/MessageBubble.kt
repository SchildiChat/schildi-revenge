package chat.schildi.revenge.compose.destination.conversation.event.message

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import chat.schildi.revenge.Dimens
import chat.schildi.theme.scExposures

@Composable
fun MessageBubble(
    isOwn: Boolean,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(Dimens.Conversation.messageBubbleInnerPadding),
    content: @Composable ColumnScope.() -> Unit
) {
    // Bypass double message bubble when nested in a reply
    if (LocalMessageRenderContext.current == MessageRenderContext.IN_REPLY_TO) {
        Column(modifier.padding(padding), content = content)
    } else {
        Column(
            modifier
                .background(
                    color = if (isOwn)
                        MaterialTheme.scExposures.bubbleBgOutgoing
                    else
                        MaterialTheme.scExposures.bubbleBgIncoming,
                    shape = Dimens.Conversation.messageBubbleShape,
                )
                .padding(padding),
            content = content
        )
    }
}
