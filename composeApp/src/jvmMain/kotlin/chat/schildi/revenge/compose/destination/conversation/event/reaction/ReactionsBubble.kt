package chat.schildi.revenge.compose.destination.conversation.event.reaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.compose.components.LocalSessionId
import io.element.android.libraries.matrix.api.timeline.item.event.EventReaction

@Composable
fun ReactionsBubble(
    reaction: EventReaction,
    modifier: Modifier = Modifier
) {
    val includesSelf = reaction.senders.any { it.senderId == LocalSessionId.current }
    Row(
        modifier.let {
            if (includesSelf) {
                it.background(MaterialTheme.colorScheme.surfaceVariant, Dimens.Conversation.reactionShape)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.onSurface,
                        Dimens.Conversation.reactionShape
                    )
            } else {
                it.background(MaterialTheme.colorScheme.surfaceContainerHigh, Dimens.Conversation.reactionShape)
            }
        }
            .padding(
                horizontal = Dimens.Conversation.reactionInnerPaddingHorizontal,
                vertical = Dimens.Conversation.reactionInnerPaddingVertical,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // TODO image reactions
        Text(
            reaction.key.take(Dimens.Conversation.reactionMaxLength),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            reaction.senders.size.toString(),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
