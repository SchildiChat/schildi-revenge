package chat.schildi.revenge.compose.destination.conversation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.compose.destination.conversation.event.EventRow
import chat.schildi.revenge.compose.destination.conversation.virtual.DayHeader
import chat.schildi.revenge.compose.destination.conversation.virtual.NewMessagesLine
import chat.schildi.revenge.compose.destination.conversation.virtual.PagingIndicator
import chat.schildi.revenge.model.ConversationViewModel
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.room.RoomMember
import io.element.android.libraries.matrix.api.timeline.MatrixTimelineItem
import io.element.android.libraries.matrix.api.timeline.item.virtual.VirtualTimelineItem
import kotlinx.collections.immutable.ImmutableMap

@Composable
fun ConversationItemRow(
    viewModel: ConversationViewModel,
    item: MatrixTimelineItem,
    roomMembersById: ImmutableMap<UserId, RoomMember>,
    next: MatrixTimelineItem?,
    previous: MatrixTimelineItem?,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        if (previous == null) {
            Spacer(Modifier.height(Dimens.windowPadding))
        }
        when (item) {
            is MatrixTimelineItem.Virtual -> {
                // TODO
                when (val virtualItem = item.virtual) {
                    is VirtualTimelineItem.DayDivider -> DayHeader(virtualItem)
                    VirtualTimelineItem.LastForwardIndicator -> Text("FWD")
                    is VirtualTimelineItem.LoadingIndicator -> PagingIndicator()
                    VirtualTimelineItem.ReadMarker -> NewMessagesLine()
                    VirtualTimelineItem.RoomBeginning -> Text("BEGINNING")
                    VirtualTimelineItem.TypingNotification -> {
                        // TODO?
                    }
                }
            }

            is MatrixTimelineItem.Event -> {
                val previousSender = (previous as? MatrixTimelineItem.Event)?.event?.sender
                val isSameAsPreviousSender = previousSender == item.event.sender
                val padding = when (previousSender) {
                    null -> 0.dp
                    item.event.sender -> Dimens.Conversation.messageSameSenderPadding
                    else -> Dimens.Conversation.messageOtherSenderPadding
                }
                Spacer(Modifier.height(padding))
                EventRow(
                    viewModel,
                    item.event,
                    isSameAsPreviousSender = isSameAsPreviousSender,
                    roomMembersById = roomMembersById,
                )
            }

            MatrixTimelineItem.Other -> {
                // TODO what is this?
                Text("OTHER???")
            }
        }
        if (next == null) {
            Spacer(Modifier.height(Dimens.windowPadding))
        }
    }
}
