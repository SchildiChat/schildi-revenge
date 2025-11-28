package chat.schildi.revenge.compose.destination.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chat.schildi.lib.preferences.ScPrefs
import chat.schildi.lib.preferences.value
import chat.schildi.lib.util.formatUnreadCount
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.compose.components.AvatarImage
import chat.schildi.revenge.compose.components.ComposeSessionScope
import chat.schildi.revenge.compose.model.ScopedRoomSummary
import chat.schildi.theme.scExposures
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.api.roomlist.RoomSummary
import kotlin.math.max

@Composable
fun InboxRow(
    room: ScopedRoomSummary,
    modifier: Modifier = Modifier,
) {
    ComposeSessionScope(room.sessionId) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.Inbox.avatar + Dimens.listPadding * 2)
                .padding(
                    horizontal = Dimens.windowPadding,
                    vertical = Dimens.listPadding,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AvatarImage(
                source = room.summary.info.avatarUrl?.let { MediaSource(it) },
                size = Dimens.Inbox.avatar,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Dimens.horizontalItemPadding)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    ScNameAndTimestampRow(room.summary)
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    ScLastMessageAndIndicatorRow(room.summary)
                }
            }
        }
    }
}

@Composable
private fun RowScope.ScNameAndTimestampRow(room: RoomSummary) {
    // Name
    Text(
        modifier = Modifier
            .weight(1f)
            .padding(end = Dimens.horizontalItemPaddingBig),
        style = MaterialTheme.typography.titleMedium,
        text = room.info.name ?: room.roomId.value,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // Favorite
        if (room.info.isFavorite) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(Dimens.Inbox.smallIcon),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Low prio
        if (room.info.isLowPriority) {
            Icon(
                imageVector = Icons.Default.Archive,
                contentDescription = null,
                modifier = Modifier.size(Dimens.Inbox.smallIcon),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Timestamp
        if (room.lastMessageTimestamp != null) {
            Text(
                text = room.lastMessageTimestamp.toString(), // TODO format
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RowScope.ScLastMessageAndIndicatorRow(room: RoomSummary) {
    // Last Message - TODO format
    val messagePreview = room.lastMessage?.eventId?.value ?: ""
    Text(
        modifier = Modifier
            .weight(1f)
            .padding(end = Dimens.horizontalItemPaddingBig),
        text = messagePreview,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )

    // Unread
    Row(
        modifier = Modifier.heightIn(min = Dimens.Inbox.smallIcon),
        horizontalArrangement = Dimens.horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ScUnreadCounter(room)
    }
}

@Composable
private fun ScUnreadCounter(room: RoomSummary) {
    val highlightCount: Long
    val notificationCount: Long
    val unreadCount: Long
    val allowSilentUnreadCount = ScPrefs.RENDER_SILENT_UNREAD.value()
    if (ScPrefs.CLIENT_GENERATED_UNREAD_COUNTS.value()) {
        highlightCount = room.info.numUnreadMentions
        notificationCount = room.info.numUnreadNotifications
        unreadCount = if (allowSilentUnreadCount) room.info.numUnreadMessages else 0
    } else {
        highlightCount = room.info.highlightCount
        notificationCount = room.info.notificationCount
        unreadCount = if (allowSilentUnreadCount) room.info.unreadCount else 0
    }
    val count: String
    val badgeColor: Color
    var outlinedBadge = false
    when {
        ScPrefs.DUAL_MENTION_UNREAD_COUNTS.value() && highlightCount > 0 && (notificationCount > highlightCount || unreadCount > highlightCount) -> {
            val fullUnreadToUse = max(unreadCount, notificationCount)
            count = "${formatUnreadCount(highlightCount)}/${formatUnreadCount(fullUnreadToUse)}"
            badgeColor = MaterialTheme.scExposures.mentionBadgeColor
        }
        notificationCount > 0 -> {
            count = formatUnreadCount(notificationCount)
            badgeColor = if (highlightCount > 0)
                MaterialTheme.scExposures.mentionBadgeColor
            else
                MaterialTheme.scExposures.notificationBadgeColor
        }
        highlightCount > 0 -> {
            count = formatUnreadCount(highlightCount)
            badgeColor = MaterialTheme.scExposures.mentionBadgeColor
        }
        room.info.isMarkedUnread -> {
            count = "!"
            badgeColor = MaterialTheme.scExposures.notificationBadgeColor
            outlinedBadge = true
        }
        unreadCount > 0 -> {
            count = formatUnreadCount(unreadCount)
            badgeColor = MaterialTheme.scExposures.unreadBadgeColor
        }
        else -> {
            // No badge to show
            return
        }
    }
    Box (
        modifier = Modifier
            .let {
                if (outlinedBadge)
                    it.border(2.dp, badgeColor, RoundedCornerShape(30.dp))
                else
                    it.background(badgeColor, RoundedCornerShape(30.dp))
            }
            .sizeIn(minWidth = 24.dp, minHeight = 24.dp)
    ) {
        Text(
            text = count,
            color = if (outlinedBadge) badgeColor else MaterialTheme.scExposures.colorOnAccent,
            style = MaterialTheme.typography.bodySmall.let { if (outlinedBadge) it.copy(fontWeight = FontWeight.Bold) else it },
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 4.dp)
        )
    }
}
