package chat.schildi.revenge.compose.destination.conversation.event

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.AddReaction
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiPeople
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.key.Key
import chat.schildi.revenge.compose.components.ContextMenuEntry
import chat.schildi.revenge.compose.util.toStringHolder
import chat.schildi.revenge.config.keybindings.Action
import chat.schildi.revenge.config.keybindings.DestinationEnum
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.timeline.item.event.EventTimelineItem
import io.element.android.libraries.matrix.api.timeline.item.event.MessageContent
import io.element.android.libraries.matrix.api.timeline.item.event.MessageTypeWithAttachment
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.action_copy_body
import shire.composeapp.generated.resources.action_download
import shire.composeapp.generated.resources.action_download_and_open
import shire.composeapp.generated.resources.action_edit
import shire.composeapp.generated.resources.action_jump_to_replied_to_message
import shire.composeapp.generated.resources.action_react
import shire.composeapp.generated.resources.action_redact
import shire.composeapp.generated.resources.action_reply
import shire.composeapp.generated.resources.action_thread
import shire.composeapp.generated.resources.action_view_reactions
import shire.composeapp.generated.resources.action_view_read_receipts

@Composable
fun EventTimelineItem.contextMenu(
    sessionId: SessionId,
    roomId: RoomId,
): ImmutableList<ContextMenuEntry> {
    val messageContent = content as? MessageContent ?: return persistentListOf()
    val canSendMessages = true // TODO from permissions
    val canSendReactions = true // TODO from permissions
    val canRedact = isOwn // TODO from power levels
    return listOfNotNull(
        ContextMenuEntry(
            Res.string.action_download_and_open.toStringHolder(),
            rememberVectorPainter(Icons.Default.OpenWith),
            Action.Event.DownloadFileAndOpen,
            keyboardShortcut = Key.O,
        ).takeIf { messageContent.type is MessageTypeWithAttachment },
        ContextMenuEntry(
            Res.string.action_download.toStringHolder(),
            rememberVectorPainter(Icons.Default.Download),
            Action.Event.DownloadFile,
            keyboardShortcut = Key.L,
        ).takeIf { messageContent.type is MessageTypeWithAttachment },
        ContextMenuEntry(
            Res.string.action_jump_to_replied_to_message.toStringHolder(),
            rememberVectorPainter(Icons.Default.Navigation),
            Action.Event.JumpToRepliedTo,
            keyboardShortcut = Key.J,
        ).takeIf { messageContent.inReplyTo != null },
        ContextMenuEntry(
            Res.string.action_reply.toStringHolder(),
            rememberVectorPainter(Icons.AutoMirrored.Default.Reply),
            Action.Event.ComposeReply,
            keyboardShortcut = Key.R,
        ).takeIf { canSendMessages },
        ContextMenuEntry(
            Res.string.action_thread.toStringHolder(),
            rememberVectorPainter(Icons.Default.Gesture),
            Action.Navigation.NavigateAuto,
            actionArgs = persistentListOf(DestinationEnum.ConversationThread.destName, sessionId.value, roomId.value, eventId?.value ?: ""),
            keyboardShortcut = Key.T,
        ).takeIf { eventId != null },
        ContextMenuEntry(
            Res.string.action_edit.toStringHolder(),
            rememberVectorPainter(Icons.Default.Edit),
            Action.Event.ComposeEdit,
            keyboardShortcut = Key.E,
        ).takeIf { isOwn && canSendMessages },
        ContextMenuEntry(
            Res.string.action_react.toStringHolder(),
            rememberVectorPainter(Icons.Default.AddReaction),
            Action.Event.ComposeReaction,
            keyboardShortcut = Key.C,
        ).takeIf { canSendReactions },
        ContextMenuEntry(
            Res.string.action_view_reactions.toStringHolder(),
            rememberVectorPainter(Icons.Default.EmojiPeople),
            Action.Navigation.NavigateAuto,
            actionArgs = persistentListOf(DestinationEnum.MessageReactions.destName, sessionId.value, roomId.value, eventId?.value ?: ""),
            keyboardShortcut = Key.V,
        ).takeIf { reactions.isNotEmpty() && eventId != null },
        ContextMenuEntry(
            Res.string.action_view_read_receipts.toStringHolder(),
            rememberVectorPainter(Icons.Default.Visibility),
            Action.Navigation.NavigateAuto,
            actionArgs = persistentListOf(DestinationEnum.MessageReadReceipts.destName, sessionId.value, roomId.value, eventId?.value ?: ""),
            keyboardShortcut = Key.P,
        ).takeIf { receipts.isNotEmpty() && eventId != null },
        ContextMenuEntry(
            Res.string.action_copy_body.toStringHolder(),
            rememberVectorPainter(Icons.Default.ContentCopy),
            Action.Event.CopyContent,
            keyboardShortcut = Key.Y,
        ).takeIf { messageContent.body.isNotBlank() },
        ContextMenuEntry(
            Res.string.action_redact.toStringHolder(),
            rememberVectorPainter(Icons.Default.Delete),
            Action.Event.Redact,
            critical = true,
            keyboardShortcut = Key.D,
        ).takeIf { canRedact },
    ).toPersistentList()
}
