package chat.schildi.revenge.compose.destination.conversation.event.reaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chat.schildi.revenge.Dimens
import io.element.android.libraries.matrix.api.timeline.item.event.EventReaction
import kotlinx.collections.immutable.ImmutableList

@Composable
fun ColumnScope.ReactionsRow(
    reactions: ImmutableList<EventReaction>,
    messageIsOwn: Boolean,
    modifier: Modifier = Modifier,
) {
    if (reactions.isEmpty()) return
    val alignment = if (messageIsOwn) Alignment.End else Alignment.Start
    FlowRow(
        modifier = modifier
            .padding(
                bottom = Dimens.Conversation.reactionPadding,
                start = if (messageIsOwn)
                    Dimens.Conversation.otherSidePadding
                else
                    Dimens.Conversation.avatarReservation,
                end = if (messageIsOwn)
                    0.dp
                else
                    Dimens.Conversation.otherSidePadding,
            )
            .align(alignment),
        verticalArrangement = Arrangement.spacedBy(Dimens.Conversation.reactionPadding),
        horizontalArrangement = Arrangement.spacedBy(
            Dimens.Conversation.reactionPadding,
            alignment,
        ),
    ) {
        reactions.forEach { reaction ->
            ReactionsBubble(reaction)
        }
    }
}
