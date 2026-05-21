package chat.schildi.revenge.compose.destination.inbox

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LowPriority
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.key.Key
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.compose.components.ContextMenuActionEntry
import chat.schildi.revenge.compose.components.ContextMenuDecoration
import chat.schildi.revenge.compose.components.ContextMenuEntry
import chat.schildi.revenge.compose.components.ContextMenuSubmenuEntry
import chat.schildi.revenge.compose.components.isAllowed
import chat.schildi.revenge.compose.focus.rememberFocusId
import chat.schildi.revenge.compose.util.toStringHolder
import chat.schildi.revenge.config.keybindings.Action
import chat.schildi.revenge.config.keybindings.ActionRoomNotificationSetting
import chat.schildi.revenge.model.InboxViewModel
import chat.schildi.revenge.model.PendingAction
import chat.schildi.revenge.model.ScopedRoomSummary
import io.element.android.libraries.matrix.api.room.RoomNotificationMode
import io.element.android.libraries.matrix.api.room.RoomNotificationSettings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.flowOf
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.action_context_favorite_toggle
import shire.composeapp.generated.resources.action_context_low_priority_toggle
import shire.composeapp.generated.resources.action_join
import shire.composeapp.generated.resources.action_leave
import shire.composeapp.generated.resources.action_mark_as_read
import shire.composeapp.generated.resources.action_mark_as_unread
import shire.composeapp.generated.resources.action_navigate_in_current
import shire.composeapp.generated.resources.action_navigate_in_new_window
import shire.composeapp.generated.resources.action_notifications
import shire.composeapp.generated.resources.action_notifications_all
import shire.composeapp.generated.resources.action_notifications_default
import shire.composeapp.generated.resources.action_notifications_mentions
import shire.composeapp.generated.resources.action_notifications_none
import shire.composeapp.generated.resources.action_reject_invite
import java.util.UUID

@Composable
fun ScopedRoomSummary.contextMenu(inboxViewModel: InboxViewModel, focusId: UUID): ImmutableList<ContextMenuEntry> {
    return if (summary.isInvite()) {
        // TODO reject invite, reject and ignore
        persistentListOf(
            ContextMenuActionEntry(
                Res.string.action_join.toStringHolder(),
                rememberVectorPainter(Icons.Default.MeetingRoom),
                Action.Room.Join,
                enabled = PendingAction.RoomJoin(summary.roomId).isAllowed(),
                keyboardShortcut = Key.J,
            ),
            ContextMenuActionEntry(
                Res.string.action_reject_invite.toStringHolder(),
                rememberVectorPainter(Icons.Default.MeetingRoom),
                Action.Room.Leave,
                critical = true,
                enabled = PendingAction.RoomLeave(summary.roomId).isAllowed(),
                keyboardShortcut = Key.R,
            ),
        )
    } else {
        val keyHandler = LocalKeyboardActionHandler.current
        val isMenuVisible = keyHandler.currentOpenContextMenu.collectAsState().value?.hasMenu(focusId) == true
        val notificationSettings = remember(inboxViewModel, this, isMenuVisible) {
            if (isMenuVisible) {
                inboxViewModel.followNotificationSettings(this)
            } else {
                flowOf(null)
            }
        }.collectAsState(null).value

        val unreadCounts = summary.unreadCounts()
        listOfNotNull(
            ContextMenuActionEntry(
                Res.string.action_mark_as_read.toStringHolder(),
                rememberVectorPainter(Icons.Default.Visibility),
                Action.Room.MarkRoomRead,
                keyboardShortcut = Key.R,
            ).takeIf { unreadCounts.hasUnread() },
            ContextMenuActionEntry(
                Res.string.action_mark_as_unread.toStringHolder(),
                rememberVectorPainter(Icons.Default.Visibility),
                Action.Room.MarkRoomUnread,
                keyboardShortcut = Key.U,
            ).takeIf { unreadCounts.canMarkUnread() },
            ContextMenuActionEntry(
                Res.string.action_context_favorite_toggle.toStringHolder(),
                rememberVectorPainter(Icons.Default.Favorite),
                Action.Room.MarkFavorite,
                persistentListOf((!summary.info.isFavorite).toString()),
                decoration = ContextMenuDecoration.Toggle(summary.info.isFavorite),
                keyboardShortcut = Key.F,
            ),
            ContextMenuActionEntry(
                Res.string.action_context_low_priority_toggle.toStringHolder(),
                rememberVectorPainter(Icons.Default.LowPriority),
                Action.Room.MarkLowPriority,
                persistentListOf((!summary.info.isLowPriority).toString()),
                decoration = ContextMenuDecoration.Toggle(summary.info.isLowPriority),
                keyboardShortcut = Key.L,
            ),
            ContextMenuActionEntry(
                Res.string.action_navigate_in_current.toStringHolder(),
                rememberVectorPainter(Icons.Default.Navigation),
                Action.NavigationItem.NavigateCurrent,
                keyboardShortcut = Key.O,
            ),
            ContextMenuActionEntry(
                Res.string.action_navigate_in_new_window.toStringHolder(),
                rememberVectorPainter(Icons.Default.Window),
                Action.NavigationItem.NavigateInNewWindow,
                keyboardShortcut = Key.W,
            ),
            ContextMenuSubmenuEntry(
                Res.string.action_notifications.toStringHolder(),
                rememberVectorPainter(Icons.Default.Notifications),
                rememberFocusId(),
                persistentListOf(
                    ContextMenuActionEntry(
                        Res.string.action_notifications_default.toStringHolder(),
                        null,
                        Action.Room.SetRoomNotifications,
                        actionArgs = persistentListOf(ActionRoomNotificationSetting.Default.name),
                        keyboardShortcut = Key.D,
                        decoration = ContextMenuDecoration.CheckMark.takeIf { notificationSettings?.isDefault == true },
                        autoCloseMenu = false,
                    ),
                    ContextMenuActionEntry(
                        Res.string.action_notifications_all.toStringHolder(),
                        null,
                        Action.Room.SetRoomNotifications,
                        actionArgs = persistentListOf(ActionRoomNotificationSetting.All.name),
                        keyboardShortcut = Key.A,
                        decoration = notificationModeCheckMark(RoomNotificationMode.ALL_MESSAGES, notificationSettings),
                        autoCloseMenu = false,
                    ),
                    ContextMenuActionEntry(
                        Res.string.action_notifications_mentions.toStringHolder(),
                        null,
                        Action.Room.SetRoomNotifications,
                        actionArgs = persistentListOf(ActionRoomNotificationSetting.Mentions.name),
                        keyboardShortcut = Key.M,
                        decoration = notificationModeCheckMark(RoomNotificationMode.MENTIONS_AND_KEYWORDS_ONLY, notificationSettings),
                        autoCloseMenu = false,
                    ),
                    ContextMenuActionEntry(
                        Res.string.action_notifications_none.toStringHolder(),
                        null,
                        Action.Room.SetRoomNotifications,
                        actionArgs = persistentListOf(ActionRoomNotificationSetting.Mute.name),
                        keyboardShortcut = Key.N,
                        decoration = notificationModeCheckMark(RoomNotificationMode.MUTE, notificationSettings),
                        autoCloseMenu = false,
                    ),
                ),
                keyboardShortcut = Key.N,
            ),
            ContextMenuActionEntry(
                Res.string.action_leave.toStringHolder(),
                rememberVectorPainter(Icons.Default.MeetingRoom),
                Action.Room.Leave,
                critical = true,
                enabled = PendingAction.RoomLeave(summary.roomId).isAllowed(),
                keyboardShortcut = Key.V,
            ),
        ).toPersistentList()
    }
}

private fun ScopedRoomSummary.notificationModeCheckMark(
    item: RoomNotificationMode,
    settings: RoomNotificationSettings?
) = when {
    settings == null -> if (summary.info.userDefinedNotificationMode == item) {
        ContextMenuDecoration.DisabledCheckMark
    } else {
        null
    }
    settings.mode == item -> if (settings.isDefault) {
        ContextMenuDecoration.DisabledCheckMark
    } else {
        ContextMenuDecoration.CheckMark
    }
    else -> null
}
