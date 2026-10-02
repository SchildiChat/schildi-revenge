package chat.schildi.revenge.compose.destination.conversation.event

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.ActionContext
import chat.schildi.revenge.actions.ActionResult
import chat.schildi.revenge.actions.CopyActions
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.HierarchicalKeyboardActionProvider
import chat.schildi.revenge.actions.InteractionAction
import chat.schildi.revenge.actions.currentActionContext
import chat.schildi.revenge.actions.actionProvider
import chat.schildi.revenge.actions.hierarchicalKeyboardActionProvider
import chat.schildi.revenge.compose.components.WithContextMenu
import chat.schildi.revenge.compose.destination.conversation.event.message.timestampOverlayContent
import chat.schildi.revenge.compose.destination.conversation.event.reaction.ReactionsRow
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.revenge.compose.focus.rememberFocusId
import chat.schildi.revenge.model.conversation.ConversationViewModel
import chat.schildi.revenge.model.conversation.MessageMetadata
import chat.schildi.revenge.model.conversation.TimestampSettings
import chat.schildi.revenge.plaintext.EventTextFormat
import com.beeper.android.messageformat.MatrixFormatInteractionState
import com.beeper.android.messageformat.rememberMatrixFormatInteractionState
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.room.RoomMember
import io.element.android.libraries.matrix.api.timeline.item.EventThreadInfo
import io.element.android.libraries.matrix.api.timeline.item.event.EventContent
import io.element.android.libraries.matrix.api.timeline.item.event.EventOrTransactionId
import io.element.android.libraries.matrix.api.timeline.item.event.EventTimelineItem
import io.element.android.libraries.matrix.api.timeline.item.event.MessageContent
import io.element.android.libraries.matrix.api.timeline.item.event.MessageTypeWithAttachment
import io.element.android.libraries.matrix.api.timeline.item.event.UnknownContent
import kotlinx.collections.immutable.ImmutableMap
import kotlin.math.min

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
    val formatInteractionState = messageMetadata?.preFormattedContent?.let {
        rememberMatrixFormatInteractionState(it)
    }
    val roomPermissions = viewModel.roomPermissions.collectAsState().value
    WithContextMenu(
        focusId,
        event.contextMenu(
            viewModel.sessionId,
            viewModel.roomId,
            roomPermissions,
            messageMetadata,
            viewModel.threadId.collectAsState().value,
        ),
    ) { openContextMenu ->
        Column(
            modifier
                .keyFocusable(
                    FocusRole.LIST_ITEM,
                    focusId,
                    actionProvider = actionProvider(
                        keyActions = eventRowKeyboardActionProvider(
                            viewModel,
                            event,
                            messageMetadata,
                            formatInteractionState,
                        ),
                        primaryAction = eventClickAction(viewModel, currentActionContext(), event),
                        secondaryAction = openContextMenu,
                        copyActions = eventCopyActions(event, messageMetadata),
                    ),
                )
                .background(backgroundHighlightColor, Dimens.Conversation.messageBubbleShape)
                .drawJumpTargetDecoration(enabled = highlight == EventHighlight.JUMP_TARGET)
                .padding(horizontal = Dimens.windowPadding)
        ) {
            val threadInfo = event.threadInfo()
            EventSwipeable(focusId, event.isOwn) { modifier ->
                EventContentLayout(
                    eventId = event.eventId,
                    content = event.content,
                    messageMetadata = messageMetadata,
                    formatInteractionState = formatInteractionState,
                    senderId = event.sender,
                    senderProfile = event.senderProfile,
                    isOwn = event.isOwn,
                    timestamp = remember(event, timestampSettings) {
                        event.timestampOverlayContent(timestampSettings)
                    },
                    isSameAsPreviousSender = isSameAsPreviousSender,
                    inReplyTo = event.inReplyTo(),
                    threadInfo = threadInfo,
                    roomMembersById = roomMembersById,
                    timelineItemDebugInfoProvider = event.timelineItemDebugInfoProvider,
                    modifier = modifier,
                )
            }
            ReactionsRow(
                viewModel = viewModel,
                eventOrTransactionId = EventOrTransactionId.from(event.eventId, event.transactionId),
                reactions = event.reactions,
                roomMembersById = roomMembersById,
                messageIsOwn = event.isOwn,
            )
            val currentThreadId = viewModel.threadId.collectAsState().value
            if (threadInfo is EventThreadInfo.ThreadRoot && event.eventId?.value != currentThreadId?.value) {
                ThreadRootInfoRow(
                    threadInfo = threadInfo,
                    sessionId = viewModel.sessionId,
                    roomId = viewModel.roomId,
                    eventId = event.eventId,
                    messageIsOwn = event.isOwn,
                )
            }
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
    formatInteractionState: MatrixFormatInteractionState?,
): HierarchicalKeyboardActionProvider {
    val ownHandler = remember(viewModel, event) {
        viewModel.getKeyboardActionProviderForEvent(event, messageMetadata, formatInteractionState)
    }
    return ownHandler.hierarchicalKeyboardActionProvider()
}

@Composable
private fun eventClickAction(
    viewModel: ConversationViewModel,
    context: ActionContext,
    event: EventTimelineItem,
): InteractionAction? = remember(viewModel, context, event) {
    when (val content = event.content) {
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

@Composable
private fun eventCopyActions(
    event: EventTimelineItem,
    messageMetadata: MessageMetadata?,
) = remember(event, messageMetadata) {
    CopyActions(
        accessPlaintextSuspend = {
            if (event.content.isPlaintextCopyable()) {
                EventTextFormat.eventToTextSuspend(
                    content = event.content,
                    messageMetadata = messageMetadata,
                    senderProfile = event.senderProfile,
                    senderId = event.sender,
                )
            } else {
                null
            }
        },
        accessUserId = { event.sender.value },
    )
}

private fun EventContent.isPlaintextCopyable() = when (this) {
    // There's literally no content attached to UnknownContent right now, copying doesn't make much sense.
    UnknownContent -> false
    else -> true
}

@Composable
fun Modifier.drawJumpTargetDecoration(
    enabled: Boolean,
): Modifier = if (enabled) {
    val color = MaterialTheme.colorScheme.error
    val paddingPx = LocalDensity.current.run { Dimens.windowPadding.toPx() / 2 }
    val widthPx = LocalDensity.current.run { Dimens.Conversation.newMessagesLineHeight.toPx() }

    drawBehind {
        val x = min(size.width/2, paddingPx)
        drawLine(
            color = color,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = min(widthPx, size.width),
        )
    }
} else {
    this
}
