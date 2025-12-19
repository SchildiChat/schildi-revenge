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
import androidx.compose.material.icons.filled.Edit
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
import chat.schildi.preferences.ScPrefs
import chat.schildi.preferences.value
import chat.schildi.lib.util.formatUnreadCount
import chat.schildi.revenge.DateTimeFormat
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.buildNavigationActionProvider
import chat.schildi.revenge.compose.components.AvatarImage
import chat.schildi.revenge.compose.components.ComposeSessionScope
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.revenge.model.ScopedRoomSummary
import chat.schildi.revenge.Destination
import chat.schildi.revenge.EventTextFormat
import chat.schildi.revenge.compose.util.toStringHolder
import chat.schildi.theme.scExposures
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.api.room.CurrentUserMembership
import io.element.android.libraries.matrix.api.roomlist.LatestEventValue
import io.element.android.libraries.matrix.api.roomlist.RoomSummary
import io.element.android.libraries.matrix.api.timeline.item.event.getDisambiguatedDisplayName
import io.element.android.libraries.matrix.api.user.MatrixUser
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.message_placeholder_invite
import shire.composeapp.generated.resources.message_placeholder_invite_by
import shire.composeapp.generated.resources.message_placeholder_invite_by_disambiguated
import shire.composeapp.generated.resources.message_placeholder_tombstone
import kotlin.math.max

@Composable
fun InboxRow(
    room: ScopedRoomSummary,
    hasDraft: Boolean,
    user: MatrixUser?,
    modifier: Modifier = Modifier,
) {
    ComposeSessionScope(room.sessionId) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.Inbox.avatar + Dimens.listPadding * 2)
                .keyFocusable(
                    FocusRole.LIST_ITEM,
                    buildNavigationActionProvider(
                        initialTitle = room.summary.info.name?.toStringHolder()
                    ) {
                        Destination.Conversation(room.sessionId, room.summary.roomId)
                    },
                )
                .padding(
                    horizontal = Dimens.windowPadding,
                    vertical = Dimens.listPadding,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                AvatarImage(
                    source = room.summary.info.avatarUrl?.let { MediaSource(it) }
                        ?: room.summary.info.heroes.takeIf { it.size == 1 }?.firstOrNull()?.avatarUrl?.let {
                            MediaSource(it)
                        },
                    size = Dimens.Inbox.avatar,
                )
                user?.avatarUrl?.let { userAvatar ->
                    AvatarImage(
                        source = MediaSource(userAvatar),
                        size = Dimens.Inbox.accountAvatar,
                        shape = Dimens.ownAccountAvatarShape,
                        modifier = Modifier.align(Alignment.BottomStart),
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Dimens.horizontalItemPadding)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    ScNameAndTimestampRow(room.summary, hasDraft)
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    ScLastMessageAndIndicatorRow(room.summary)
                }
            }
        }
    }
}

@Composable
private fun RowScope.ScNameAndTimestampRow(room: RoomSummary, hasDraft: Boolean) {
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
        if (hasDraft) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                modifier = Modifier.size(Dimens.Inbox.smallIcon),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Favorite
        if (room.info.isFavorite && ScPrefs.PIN_FAVORITES.value()) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(Dimens.Inbox.smallIcon),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Low prio
        if (room.info.isLowPriority && ScPrefs.BURY_LOW_PRIORITY.value()) {
            Icon(
                imageVector = Icons.Default.Archive,
                contentDescription = null,
                modifier = Modifier.size(Dimens.Inbox.smallIcon),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Timestamp
        room.latestEventTimestamp?.let { timestamp ->
            Text(
                text = DateTimeFormat.formatTimestampAsDateOrTime(timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RowScope.ScLastMessageAndIndicatorRow(room: RoomSummary) {
    // Last Message
    val messagePreview = if (room.isInvite()) {
        room.info.inviter?.let { inviter ->
            val displayName = inviter.displayName
            if (displayName != null && room.info.name != displayName) {
                stringResource(Res.string.message_placeholder_invite_by_disambiguated, displayName, inviter.userId.value)
            } else {
                stringResource(Res.string.message_placeholder_invite_by, inviter.userId.value)
            }
        } ?: stringResource(Res.string.message_placeholder_invite)
    } else if (room.info.successorRoom != null) {
        stringResource(Res.string.message_placeholder_tombstone)
    } else {
        when (val event = room.latestEvent) {
            is LatestEventValue.Local -> EventTextFormat.eventToText(event.content, event.senderProfile, event.senderId)
            LatestEventValue.None -> null
            is LatestEventValue.Remote -> {
                val eventText = EventTextFormat.eventToText(event.content, event.senderProfile, event.senderId)
                if (event.isOwn || room.isOneToOne) {
                    eventText
                } else {
                    "${event.senderProfile.getDisambiguatedDisplayName(event.senderId)}: $eventText"
                }
            }
        } ?: ""
    }
    Row(
        modifier = Modifier
            .weight(1f)
            .padding(end = Dimens.horizontalItemPaddingBig),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (room.isInvite()) {
            room.info.inviter?.avatarUrl?.let {
                AvatarImage(
                    source = MediaSource(it),
                    size = 12.dp,
                    modifier = Modifier.padding(end = Dimens.horizontalItemPadding),
                )
            }
        }
        Text(
            text = messagePreview,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }

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
        room.isInvite() -> {
            count = "!"
            badgeColor = MaterialTheme.scExposures.notificationBadgeColor
        }
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

fun RoomSummary.isInvite() = info.currentUserMembership == CurrentUserMembership.INVITED
