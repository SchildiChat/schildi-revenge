package chat.schildi.revenge.compose.destination.conversation.event

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.ActionContext
import chat.schildi.revenge.actions.ActionResult
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.HierarchicalKeyboardActionProvider
import chat.schildi.revenge.actions.InteractionAction
import chat.schildi.revenge.actions.currentActionContext
import chat.schildi.revenge.actions.actionProvider
import chat.schildi.revenge.actions.hierarchicalKeyboardActionProvider
import chat.schildi.revenge.compose.components.WithContextMenu
import chat.schildi.revenge.compose.destination.conversation.event.message.timestampOverlayContent
import chat.schildi.revenge.compose.destination.conversation.event.reaction.ReactionsRow
import chat.schildi.revenge.compose.destination.conversation.virtual.ConversationDividerLine
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.revenge.compose.focus.rememberFocusId
import chat.schildi.revenge.model.conversation.ConversationViewModel
import chat.schildi.revenge.model.conversation.MessageMetadata
import chat.schildi.revenge.model.conversation.TimestampSettings
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.room.RoomMember
import io.element.android.libraries.matrix.api.timeline.item.event.EventOrTransactionId
import io.element.android.libraries.matrix.api.timeline.item.event.EventTimelineItem
import io.element.android.libraries.matrix.api.timeline.item.event.MessageContent
import io.element.android.libraries.matrix.api.timeline.item.event.MessageTypeWithAttachment
import kotlinx.collections.immutable.ImmutableMap

enum class EventHighlight {
    NONE,
    JUMP_TARGET,
    ACTION_TARGET,
}

@Composable
fun EventRow(
    viewModel: ConversationViewModel,
    event: EventTimelineItem,
    messageMetadata: MessageMetadata?,
    isSameAsPreviousSender: Boolean,
    roomMembersById: ImmutableMap<UserId, RoomMember>,
    highlight: EventHighlight,
    timestampSettings: TimestampSettings,
    modifier: Modifier = Modifier
) {
    val backgroundHighlightColor = animateColorAsState(
        if (highlight == EventHighlight.ACTION_TARGET) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        } else {
            Color.Transparent
        }
    ).value
    val focusId = rememberFocusId()
    WithContextMenu(
        focusId,
        event.contextMenu(),
    ) { openContextMenu ->
        Column(
            modifier
                .keyFocusable(
                    FocusRole.LIST_ITEM,
                    focusId,
                    actionProvider = actionProvider(
                        keyActions = eventRowKeyboardActionProvider(viewModel, event, messageMetadata),
                        primaryAction = eventClickAction(viewModel, currentActionContext(), event),
                        secondaryAction = openContextMenu,
                    ),
                )
                .background(backgroundHighlightColor, Dimens.Conversation.messageBubbleShape)
                .padding(horizontal = Dimens.windowPadding)
        ) {
            if (highlight == EventHighlight.JUMP_TARGET) {
                // TODO revise design once there's a better idea
                ConversationDividerLine(MaterialTheme.colorScheme.error)
            }
            EventContentLayout(
                content = event.content,
                messageMetadata = messageMetadata,
                senderId = event.sender,
                senderProfile = event.senderProfile,
                isOwn = event.isOwn,
                timestamp = remember(event, timestampSettings) {
                    event.timestampOverlayContent(timestampSettings)
                },
                isSameAsPreviousSender = isSameAsPreviousSender,
                inReplyTo = event.inReplyTo(),
            )
            ReactionsRow(
                viewModel = viewModel,
                eventOrTransactionId = EventOrTransactionId.from(event.eventId, event.transactionId),
                reactions = event.reactions,
                roomMembersById = roomMembersById,
                messageIsOwn = event.isOwn,
            )
            ReadReceiptsRow(
                receipts = event.receipts,
                roomMembersById = roomMembersById,
            )
        }
    }
}

@Composable
private fun eventRowKeyboardActionProvider(
    viewModel: ConversationViewModel,
    event: EventTimelineItem,
    messageMetadata: MessageMetadata?,
): HierarchicalKeyboardActionProvider {
    val ownHandler = remember(viewModel, event) {
        viewModel.getKeyboardActionProviderForEvent(event, messageMetadata)
    }
    return ownHandler.hierarchicalKeyboardActionProvider()
}

private fun eventClickAction(
    viewModel: ConversationViewModel,
    context: ActionContext,
    event: EventTimelineItem,
): InteractionAction? {
    return when (val content = event.content) {
        is MessageContent -> {
            when (content.type) {
                is MessageTypeWithAttachment -> InteractionAction.Invoke {
                    viewModel.downloadFileAndOpen(context, event) is ActionResult.Success
                }
                else -> null
            }
        }
        else -> null
    }
}
