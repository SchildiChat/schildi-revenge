package chat.schildi.revenge.compose.destination.conversation.event

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.AddReaction
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiPeople
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.PublicOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.key.Key
import chat.schildi.revenge.compose.components.ContextMenuActionEntry
import chat.schildi.revenge.compose.components.ContextMenuEntry
import chat.schildi.resources.toStringHolder
import chat.schildi.revenge.compose.components.ContextMenuSubmenuEntry
import chat.schildi.revenge.compose.components.enterCommandModeContextMenuAction
import chat.schildi.revenge.compose.focus.rememberFocusId
import chat.schildi.revenge.config.keybindings.Action
import chat.schildi.revenge.config.keybindings.DestinationEnum
import chat.schildi.revenge.model.conversation.ConversationPermissions
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.timeline.item.event.EventTimelineItem
import io.element.android.libraries.matrix.api.timeline.item.event.MessageContent
import io.element.android.libraries.matrix.api.timeline.item.event.MessageTypeWithAttachment
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import shire.res.generated.resources.Res
import shire.res.generated.resources.action_copy_body
import shire.res.generated.resources.action_download
import shire.res.generated.resources.action_download_and_open
import shire.res.generated.resources.action_edit
import shire.res.generated.resources.action_jump_to_replied_to_message
import shire.res.generated.resources.action_mark_as_read
import shire.res.generated.resources.action_mark_as_read_type_read_marker
import shire.res.generated.resources.action_mark_as_read_type_read_receipt_private
import shire.res.generated.resources.action_mark_as_read_type_read_receipt_public
import shire.res.generated.resources.action_react
import shire.res.generated.resources.action_redact
import shire.res.generated.resources.action_reply
import shire.res.generated.resources.action_thread
import shire.res.generated.resources.action_view_reactions
import shire.res.generated.resources.action_view_read_receipts

@Composable
fun EventTimelineItem.contextMenu(
    sessionId: SessionId,
    roomId: RoomId,
    permissions: ConversationPermissions?,
): ImmutableList<ContextMenuEntry> {
    val messageContent = content as? MessageContent ?: return persistentListOf()
    val canRedact = if (isOwn) {
        permissions?.canRedactOwn ?: true
    } else {
        permissions?.canRedactOther ?: false
    }
    return listOfNotNull(
        ContextMenuActionEntry(
            Res.string.action_download_and_open.toStringHolder(),
            rememberVectorPainter(Icons.Default.OpenWith),
            Action.Event.DownloadFileAndOpen,
            keyboardShortcut = Key.O,
        ).takeIf { messageContent.type is MessageTypeWithAttachment },
        ContextMenuActionEntry(
            Res.string.action_download.toStringHolder(),
            rememberVectorPainter(Icons.Default.Download),
            Action.Event.DownloadFile,
            keyboardShortcut = Key.L,
        ).takeIf { messageContent.type is MessageTypeWithAttachment },
        ContextMenuActionEntry(
            Res.string.action_jump_to_replied_to_message.toStringHolder(),
            rememberVectorPainter(Icons.Default.Navigation),
            Action.Event.JumpToRepliedTo,
            keyboardShortcut = Key.J,
        ).takeIf { messageContent.inReplyTo != null },
        ContextMenuActionEntry(
            Res.string.action_reply.toStringHolder(),
            rememberVectorPainter(Icons.AutoMirrored.Default.Reply),
            Action.Event.ComposeReply,
            keyboardShortcut = Key.R,
        ).takeIf { permissions?.canSendMessages ?: false },
        ContextMenuActionEntry(
            Res.string.action_thread.toStringHolder(),
            rememberVectorPainter(Icons.Default.Gesture),
            Action.Navigation.NavigateAuto,
            actionArgs = persistentListOf(DestinationEnum.ConversationThread.destName, sessionId.value, roomId.value, eventId?.value ?: ""),
            keyboardShortcut = Key.T,
        ).takeIf { eventId != null },
        ContextMenuActionEntry(
            Res.string.action_edit.toStringHolder(),
            rememberVectorPainter(Icons.Default.Edit),
            Action.Event.ComposeEdit,
            keyboardShortcut = Key.E,
        ).takeIf { isOwn && (permissions?.canSendMessages ?: true) },
        ContextMenuActionEntry(
            Res.string.action_react.toStringHolder(),
            rememberVectorPainter(Icons.Default.AddReaction),
            Action.Event.ComposeReaction,
            keyboardShortcut = Key.C,
        ).takeIf { permissions?.canSendReactions ?: false },
        ContextMenuActionEntry(
            Res.string.action_view_reactions.toStringHolder(),
            rememberVectorPainter(Icons.Default.EmojiPeople),
            Action.Navigation.NavigateAuto,
            actionArgs = persistentListOf(DestinationEnum.MessageReactions.destName, sessionId.value, roomId.value, eventId?.value ?: ""),
            keyboardShortcut = Key.V,
        ).takeIf { reactions.isNotEmpty() && eventId != null },
        ContextMenuActionEntry(
            Res.string.action_view_read_receipts.toStringHolder(),
            rememberVectorPainter(Icons.Default.Visibility),
            Action.Navigation.NavigateAuto,
            actionArgs = persistentListOf(DestinationEnum.MessageReadReceipts.destName, sessionId.value, roomId.value, eventId?.value ?: ""),
            keyboardShortcut = Key.P,
        ).takeIf { receipts.isNotEmpty() && eventId != null },
        ContextMenuActionEntry(
            Res.string.action_copy_body.toStringHolder(),
            rememberVectorPainter(Icons.Default.ContentCopy),
            Action.Event.CopyContent,
            keyboardShortcut = Key.Y,
        ).takeIf { messageContent.body.isNotBlank() },
        ContextMenuSubmenuEntry(
            Res.string.action_mark_as_read.toStringHolder(),
            rememberVectorPainter(Icons.Default.Visibility),
            rememberFocusId(),
            persistentListOf(
                ContextMenuActionEntry(
                    Res.string.action_mark_as_read_type_read_receipt_public.toStringHolder(),
                    rememberVectorPainter(Icons.Default.Public),
                    Action.Event.MarkEventRead,
                    keyboardShortcut = Key.R,
                    dismissParentsOnAutoClose = true,
                ),
                ContextMenuActionEntry(
                    Res.string.action_mark_as_read_type_read_receipt_private.toStringHolder(),
                    rememberVectorPainter(Icons.Default.PublicOff),
                    Action.Event.MarkEventReadPrivate,
                    keyboardShortcut = Key.P,
                    dismissParentsOnAutoClose = true,
                ),
                ContextMenuActionEntry(
                    Res.string.action_mark_as_read_type_read_marker.toStringHolder(),
                    rememberVectorPainter(Icons.Default.Checklist),
                    Action.Event.MarkEventFullyRead,
                    keyboardShortcut = Key.M,
                    dismissParentsOnAutoClose = true,
                ),
            ),
            keyboardShortcut = Key.M,
        ),
        ContextMenuActionEntry(
            Res.string.action_redact.toStringHolder(),
            rememberVectorPainter(Icons.Default.Delete),
            Action.Event.Redact,
            critical = true,
            keyboardShortcut = Key.D,
        ).takeIf { canRedact },
        enterCommandModeContextMenuAction(),
    ).toPersistentList()
}
