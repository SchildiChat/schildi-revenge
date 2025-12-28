package chat.schildi.revenge.compose.destination.inbox

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LowPriority
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Window
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.key.Key
import chat.schildi.revenge.compose.components.ContextMenuEntry
import chat.schildi.revenge.compose.util.toStringHolder
import chat.schildi.revenge.config.keybindings.Action
import chat.schildi.revenge.model.ScopedRoomSummary
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.action_context_favorite_toggle
import shire.composeapp.generated.resources.action_context_low_priority_toggle
import shire.composeapp.generated.resources.action_leave
import shire.composeapp.generated.resources.action_mark_as_read
import shire.composeapp.generated.resources.action_mark_as_unread
import shire.composeapp.generated.resources.action_navigate_in_current
import shire.composeapp.generated.resources.action_navigate_in_new_window

@Composable
fun ScopedRoomSummary.contextMenu(): ImmutableList<ContextMenuEntry> {
    // TODO invite-specific context menu too
    return if (summary.isInvite()) {
        persistentListOf()
    } else {
        val unreadCounts = summary.unreadCounts()
        listOfNotNull(
            ContextMenuEntry(
                Res.string.action_mark_as_read.toStringHolder(),
                rememberVectorPainter(Icons.Default.Visibility),
                Action.Room.MarkRoomRead,
                keyboardShortcut = Key.R,
            ).takeIf { unreadCounts.hasUnread() },
            ContextMenuEntry(
                Res.string.action_mark_as_unread.toStringHolder(),
                rememberVectorPainter(Icons.Default.Visibility),
                Action.Room.MarkRoomUnread,
                keyboardShortcut = Key.U,
            ).takeIf { unreadCounts.canMarkUnread() },
            ContextMenuEntry(
                Res.string.action_context_favorite_toggle.toStringHolder(),
                rememberVectorPainter(Icons.Default.Favorite),
                Action.Room.MarkFavorite,
                persistentListOf((!summary.info.isFavorite).toString()),
                toggleState = summary.info.isFavorite,
                keyboardShortcut = Key.F,
            ),
            ContextMenuEntry(
                Res.string.action_context_low_priority_toggle.toStringHolder(),
                rememberVectorPainter(Icons.Default.LowPriority),
                Action.Room.MarkLowPriority,
                persistentListOf((!summary.info.isLowPriority).toString()),
                toggleState = summary.info.isLowPriority,
                keyboardShortcut = Key.L,
            ),
            ContextMenuEntry(
                Res.string.action_navigate_in_current.toStringHolder(),
                rememberVectorPainter(Icons.Default.Navigation),
                Action.NavigationItem.NavigateCurrent,
                keyboardShortcut = Key.O,
            ),
            ContextMenuEntry(
                Res.string.action_navigate_in_new_window.toStringHolder(),
                rememberVectorPainter(Icons.Default.Window),
                Action.NavigationItem.NavigateInNewWindow,
                keyboardShortcut = Key.W,
            ),
            ContextMenuEntry(
                Res.string.action_leave.toStringHolder(),
                rememberVectorPainter(Icons.Default.MeetingRoom),
                Action.Room.Leave,
                critical = true,
                keyboardShortcut = Key.V,
            ),
        ).toPersistentList()
    }
}
