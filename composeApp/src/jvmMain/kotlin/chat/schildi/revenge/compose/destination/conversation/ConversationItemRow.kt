package chat.schildi.revenge.compose.destination.conversation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chat.schildi.lib.preferences.ScPrefs
import chat.schildi.revenge.preferences.value
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.compose.destination.conversation.event.EventHighlight
import chat.schildi.revenge.compose.destination.conversation.event.EventRow
import chat.schildi.revenge.compose.destination.conversation.virtual.DayHeaderRow
import chat.schildi.revenge.compose.destination.conversation.virtual.DebugLinePosition
import chat.schildi.revenge.compose.destination.conversation.virtual.TimelineItemDebugLine
import chat.schildi.revenge.compose.destination.conversation.virtual.TimelineItemDebugLineInstance
import chat.schildi.revenge.compose.destination.conversation.virtual.NewMessageLineInstance
import chat.schildi.revenge.compose.destination.conversation.virtual.NewMessagesLine
import chat.schildi.revenge.compose.destination.conversation.virtual.PagingIndicator
import chat.schildi.revenge.compose.destination.conversation.virtual.RoomBeginning
import chat.schildi.revenge.compose.destination.conversation.virtual.drawTimelineItemDebugLineBehind
import chat.schildi.revenge.model.conversation.ConversationViewModel
import chat.schildi.revenge.model.conversation.ScTimelineItem
import chat.schildi.revenge.model.conversation.TimestampSettings
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.room.RoomMember
import io.element.android.libraries.matrix.api.timeline.MatrixTimelineItem
import io.element.android.libraries.matrix.api.timeline.item.event.MessageContent
import io.element.android.libraries.matrix.api.timeline.item.event.perMessageProfile
import io.element.android.libraries.matrix.api.timeline.item.virtual.VirtualTimelineItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf

@Composable
fun ConversationItemRow(
    viewModel: ConversationViewModel,
    item: ScTimelineItem,
    roomMembersById: ImmutableMap<UserId, RoomMember>,
    next: ScTimelineItem?,
    previous: ScTimelineItem?,
    previousEvent: MatrixTimelineItem.Event?,
    highlight: EventHighlight,
    timestampSettings: TimestampSettings,
    showBackwardPagingIndicator: Boolean,
    showForwardPagingIndicator: Boolean,
    modifier: Modifier = Modifier,
    debugSeparatorLines: ImmutableList<TimelineItemDebugLineInstance> = persistentListOf(),
) {
    val fullyReadEvent = viewModel.cachedFullyRead.collectAsState().value
    Column(modifier.fillMaxWidth()) {
        if (previous == null) {
            Spacer(Modifier.height(Dimens.windowPadding))
            if (showBackwardPagingIndicator) {
                PagingIndicator()
            }
        }
        debugSeparatorLines.filter { it.position == DebugLinePosition.Above }.forEach {
            TimelineItemDebugLine(it)
        }
        when (item.item) {
            is MatrixTimelineItem.Virtual -> {
                when (val virtualItem = item.item.virtual) {
                    is VirtualTimelineItem.DayDivider -> DayHeaderRow(virtualItem)
                    is VirtualTimelineItem.LoadingIndicator -> PagingIndicator()
                    VirtualTimelineItem.ReadMarker -> NewMessagesLine(
                        instance = NewMessageLineInstance.Sdk,
                        isThreadedTimeline = viewModel.threadId.collectAsState().value != null,
                    )
                    VirtualTimelineItem.RoomBeginning -> RoomBeginning()
                    // Not sure if we're supposed to render something for that one
                    VirtualTimelineItem.LastForwardIndicator -> {}
                    VirtualTimelineItem.TypingNotification -> TypingUsersRow(
                        viewModel.typingUsers.collectAsState(null).value ?: persistentListOf(),
                        roomMembersById,
                    )
                }
            }

            is MatrixTimelineItem.Event -> {
                val isFirstUnreadEvent = fullyReadEvent?.has(previousEvent?.eventId) == true
                val showUnreadLine = isFirstUnreadEvent && (!item.item.event.isOwn || fullyReadEvent.usedAsJumpTarget)
                if (isFirstUnreadEvent) {
                    val targetEvent = viewModel.targetEvent.collectAsState().value
                    SideEffect(item.item.eventId, targetEvent) {
                        viewModel.markUnreadLineSeen()
                    }
                }
                if (showUnreadLine) {
                    NewMessagesLine(
                        instance = NewMessageLineInstance.ReadMarker,
                        isThreadedTimeline = viewModel.threadId.collectAsState().value != null,
                    )
                }

                val directPreviousEvent = (previous?.item as? MatrixTimelineItem.Event)?.event
                val previousSender = directPreviousEvent?.sender?.takeIf { !showUnreadLine }
                val isSameAsPreviousSender = previousSender == item.item.event.sender &&
                        directPreviousEvent.content is MessageContent &&
                        directPreviousEvent.content.perMessageProfile() == item.item.event.content.perMessageProfile()
                val padding = when (previousSender) {
                    null -> 0.dp
                    item.item.event.sender -> Dimens.Conversation.messageSameSenderPadding
                    else -> Dimens.Conversation.messageOtherSenderPadding
                }
                Spacer(Modifier.height(padding))
                EventRow(
                    viewModel,
                    item.item.event,
                    item.messageMetadata,
                    isSameAsPreviousSender = isSameAsPreviousSender,
                    roomMembersById = roomMembersById,
                    highlight = highlight,
                    timestampSettings = timestampSettings,
                    modifier = debugSeparatorLines.firstOrNull { it.position == DebugLinePosition.Start }?.let {
                        Modifier.drawTimelineItemDebugLineBehind(it)
                    } ?: Modifier,
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
            if (showForwardPagingIndicator) {
                PagingIndicator()
            }
            Spacer(Modifier.height(Dimens.windowPadding))
        }
    }
}
