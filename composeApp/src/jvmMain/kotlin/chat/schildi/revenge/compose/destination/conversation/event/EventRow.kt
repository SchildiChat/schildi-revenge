package chat.schildi.revenge.compose.destination.conversation.event

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.HierarchicalKeyboardActionProvider
import chat.schildi.revenge.actions.defaultActionProvider
import chat.schildi.revenge.actions.hierarchicalKeyboardActionProvider
import chat.schildi.revenge.compose.destination.conversation.event.message.timestampOverlayContent
import chat.schildi.revenge.compose.destination.conversation.event.reaction.ReactionsRow
import chat.schildi.revenge.compose.destination.conversation.virtual.ConversationDividerLine
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.revenge.model.ConversationViewModel
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.room.RoomMember
import io.element.android.libraries.matrix.api.timeline.item.event.EventTimelineItem
import kotlinx.collections.immutable.ImmutableMap

@Composable
fun EventRow(
    viewModel: ConversationViewModel,
    event: EventTimelineItem,
    isSameAsPreviousSender: Boolean,
    roomMembersById: ImmutableMap<UserId, RoomMember>,
    highlight: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .keyFocusable(
                FocusRole.SEARCHABLE_ITEM,
                actionProvider = defaultActionProvider(
                    keyActions = eventRowKeyboardActionProvider(viewModel, event),
                ),
            )
            .padding(horizontal = Dimens.windowPadding)
    ) {
        if (highlight) {
            // TODO revise design once there's a better idea
            ConversationDividerLine(MaterialTheme.colorScheme.error)
        }
        EventContentLayout(
            content = event.content,
            senderId = event.sender,
            senderProfile = event.senderProfile,
            isOwn = event.isOwn,
            timestamp = remember(event) { event.timestampOverlayContent() },
            isSameAsPreviousSender = isSameAsPreviousSender,
            inReplyTo = event.inReplyTo(),
        )
        ReactionsRow(
            reactions = event.reactions,
            messageIsOwn = event.isOwn,
        )
        ReadReceiptsRow(
            receipts = event.receipts,
            roomMembersById = roomMembersById,
        )
    }
}

@Composable
private fun eventRowKeyboardActionProvider(
    viewModel: ConversationViewModel,
    event: EventTimelineItem
): HierarchicalKeyboardActionProvider {
    val ownHandler = remember(viewModel, event) {
        viewModel.getKeyboardActionProviderForEvent(event)
    }
    return ownHandler.hierarchicalKeyboardActionProvider()
}
