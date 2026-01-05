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
import chat.schildi.revenge.model.ConversationViewModel
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.room.RoomMember
import io.element.android.libraries.matrix.api.timeline.item.event.EventOrTransactionId
import io.element.android.libraries.matrix.api.timeline.item.event.EventReaction
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap

@Composable
fun ColumnScope.ReactionsRow(
    viewModel: ConversationViewModel,
    eventOrTransactionId: EventOrTransactionId,
    reactions: ImmutableList<EventReaction>,
    roomMembersById: ImmutableMap<UserId, RoomMember>,
    messageIsOwn: Boolean,
    modifier: Modifier = Modifier,
) {
    if (reactions.isEmpty()) return
    val alignment = if (messageIsOwn) Alignment.End else Alignment.Start
    FlowRow(
        modifier = modifier
            .padding(
                top = Dimens.Conversation.reactionPaddingVertical,
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
        verticalArrangement = Arrangement.spacedBy(Dimens.Conversation.reactionPaddingVertical),
        horizontalArrangement = Arrangement.spacedBy(
            Dimens.Conversation.reactionPaddingHorizontal,
            alignment,
        ),
    ) {
        reactions.forEach { reaction ->
            ReactionsBubble(viewModel, eventOrTransactionId, reaction, roomMembersById)
        }
    }
}
