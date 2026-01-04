package chat.schildi.revenge.compose.destination.conversation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chat.schildi.preferences.ScPrefs
import chat.schildi.preferences.value
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.compose.destination.conversation.event.EventHighlight
import chat.schildi.revenge.compose.destination.conversation.event.EventRow
import chat.schildi.revenge.compose.destination.conversation.virtual.DayHeader
import chat.schildi.revenge.compose.destination.conversation.virtual.NewMessagesLine
import chat.schildi.revenge.compose.destination.conversation.virtual.PagingIndicator
import chat.schildi.revenge.compose.destination.conversation.virtual.RoomBeginning
import chat.schildi.revenge.model.ConversationViewModel
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.room.RoomMember
import io.element.android.libraries.matrix.api.timeline.MatrixTimelineItem
import io.element.android.libraries.matrix.api.timeline.item.event.MessageContent
import io.element.android.libraries.matrix.api.timeline.item.virtual.VirtualTimelineItem
import kotlinx.collections.immutable.ImmutableMap

@Composable
fun ConversationItemRow(
    viewModel: ConversationViewModel,
    item: MatrixTimelineItem,
    roomMembersById: ImmutableMap<UserId, RoomMember>,
    next: MatrixTimelineItem?,
    previous: MatrixTimelineItem?,
    highlight: EventHighlight,
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
                    is VirtualTimelineItem.LoadingIndicator -> PagingIndicator()
                    VirtualTimelineItem.ReadMarker -> NewMessagesLine()
                    VirtualTimelineItem.RoomBeginning -> RoomBeginning()
                    // Not sure if we're supposed to render something for that one
                    VirtualTimelineItem.LastForwardIndicator -> {}
                    VirtualTimelineItem.TypingNotification -> TypingUsersRow(
                        viewModel.typingUsers.collectAsState(null).value.orEmpty(),
                        roomMembersById,
                    )
                }
            }

            is MatrixTimelineItem.Event -> {
                val previousEvent = (previous as? MatrixTimelineItem.Event)?.event
                val previousSender = previousEvent?.sender
                val isSameAsPreviousSender = previousSender == item.event.sender &&
                        previousEvent.content is MessageContent
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
                    highlight = highlight,
                )
            }

            MatrixTimelineItem.Other -> {
                // TODO what is this?
                if (ScPrefs.VIEW_HIDDEN_EVENTS.value()) {
                    Text("OTHER???")
                }
            }
        }
        if (next == null) {
            Spacer(Modifier.height(Dimens.windowPadding))
        }
    }
}
