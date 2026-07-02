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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chat.schildi.lib.preferences.ScPrefs
import chat.schildi.revenge.preferences.value
import chat.schildi.lib.util.formatUnreadCount
import chat.schildi.revenge.DateTimeFormat
import chat.schildi.revenge.Destination
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.UiState
import chat.schildi.revenge.actions.ActionResult
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.buildNavigationActionProvider
import chat.schildi.revenge.compose.components.AvatarImage
import chat.schildi.revenge.compose.components.ComposeSessionScope
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.revenge.model.ScopedRoomSummary
import chat.schildi.revenge.plaintext.EventTextFormat
import chat.schildi.revenge.actions.HierarchicalKeyboardActionProvider
import chat.schildi.revenge.actions.InteractionAction
import chat.schildi.revenge.actions.currentActionContext
import chat.schildi.revenge.actions.actionProvider
import chat.schildi.revenge.actions.hierarchicalKeyboardActionProvider
import chat.schildi.revenge.actions.plainTextCopyAction
import chat.schildi.revenge.compose.components.WithContextMenu
import chat.schildi.revenge.compose.components.WithTrackedAction
import chat.schildi.revenge.compose.components.thenIf
import chat.schildi.revenge.compose.focus.rememberFocusId
import chat.schildi.revenge.model.conversation.ConversationViewModel
import chat.schildi.revenge.model.InboxViewModel
import chat.schildi.revenge.model.PendingAction
import chat.schildi.revenge.model.ScopedRoomKey
import chat.schildi.theme.scExposures
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.api.room.CurrentUserMembership
import io.element.android.libraries.matrix.api.roomlist.LatestEventValue
import io.element.android.libraries.matrix.api.roomlist.RoomSummary
import io.element.android.libraries.matrix.api.timeline.item.event.getDisambiguatedDisplayName
import io.element.android.libraries.matrix.api.timeline.item.event.getDisplayName
import io.element.android.libraries.matrix.api.user.MatrixUser
import org.jetbrains.compose.resources.stringResource
import shire.res.generated.resources.Res
import shire.res.generated.resources.action_join
import shire.res.generated.resources.membership_change_invited
import shire.res.generated.resources.message_placeholder_invite
import shire.res.generated.resources.message_placeholder_invite_by
import shire.res.generated.resources.message_placeholder_invite_by_disambiguated
import shire.res.generated.resources.message_placeholder_tombstone
import shire.res.generated.resources.room_type_space
import kotlin.math.max

@Composable
fun InboxRow(
    viewModel: InboxViewModel,
    room: ScopedRoomSummary,
    hasDraft: Boolean,
    user: MatrixUser?,
    needsAccountDisambiguation: Boolean,
    modifier: Modifier = Modifier,
) {
    ComposeSessionScope(room.sessionId) {
        val focusId = rememberFocusId()
        WithContextMenu(
            focusId = focusId,
            entries = room.contextMenu(viewModel, focusId),
        ) { openContextMenu ->
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .heightIn(min = Dimens.Inbox.avatar + Dimens.listPadding * 2)
                    .keyFocusable(
                        FocusRole.LIST_ITEM,
                        focusId,
                        actionProvider = buildNavigationActionProvider(
                            initialTitle = {
                                ConversationViewModel.windowTitle(
                                    roomInfo = room.summary.info,
                                    accountUserDisplayName = user?.displayName,
                                    sessionId = room.sessionId,
                                )
                            },
                            keyActions = inboxRowKeyboardActionProvider(viewModel, room.key, isInvite = room.summary.isInvite()),
                            secondaryAction = openContextMenu,
                            copyActions = plainTextCopyAction { room.summary.info.name },
                        ) {
                            UiState.getConversationDestinationFromInbox(room.sessionId, room.summary.roomId)
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
                        displayName = room.summary.info.name ?: "",
                    )
                    user?.avatarUrl?.takeIf { needsAccountDisambiguation}?.let { userAvatar ->
                        AvatarImage(
                            source = MediaSource(userAvatar),
                            size = Dimens.Inbox.accountAvatar,
                            shape = Dimens.ownAccountAvatarShape,
                            displayName = user.displayName ?: user.userId.value,
                            modifier = Modifier.align(Alignment.BottomStart),
                        )
                    }
                    room.summary.info.bridgeState.firstOrNull { it.protocol?.avatarUrl != null }?.protocol?.let { protocol ->
                        protocol.avatarUrl?.let { bridgeAvatar ->
                            AvatarImage(
                                source = MediaSource(bridgeAvatar),
                                size = Dimens.Inbox.accountAvatar,
                                shape = CircleShape,
                                displayName = protocol.displayName ?: protocol.id ?: "",
                                modifier = Modifier.align(Alignment.BottomEnd),
                            )
                        }
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
                        ScLastMessageAndIndicatorRow(viewModel, room)
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.ScNameAndTimestampRow(room: RoomSummary, hasDraft: Boolean) {
    // Name
    val primaryName = room.info.privateRoomName ?: room.info.name ?: room.roomId.value
    val secondaryName = if (room.info.privateRoomName != null && room.info.name != null) {
        room.info.name
    } else {
        null
    }
    Text(
        modifier = Modifier
            .thenIf(secondaryName == null && !room.info.isSpace) {
                weight(1f)
            },
        style = MaterialTheme.typography.titleMedium,
        text = primaryName,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
    if (room.info.isSpace) {
        Text(
            modifier = Modifier
                .padding(start = Dimens.horizontalItemPadding)
                .thenIf(secondaryName == null) {
                    weight(1f)
                },
            style = MaterialTheme.typography.titleMedium,
            text = stringResource(Res.string.room_type_space),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
    if (secondaryName != null) {
        Text(
            modifier = Modifier
                .padding(start = Dimens.horizontalItemPadding)
                .weight(1f),
            style = MaterialTheme.typography.titleMedium,
            text = secondaryName,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(start = Dimens.horizontalItemPaddingBig),
    ) {
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
private fun RowScope.ScLastMessageAndIndicatorRow(
    viewModel: InboxViewModel,
    scopedRoom: ScopedRoomSummary,
) {
    val room = scopedRoom.summary
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
            is LatestEventValue.Local -> EventTextFormat.eventToText(
                event.content,
                scopedRoom.latestEventMessageMetadata,
                event.senderProfile,
                event.senderId
            )
            LatestEventValue.None -> null
            is LatestEventValue.Remote -> {
                val eventText = EventTextFormat.eventToText(
                    event.content,
                    scopedRoom.latestEventMessageMetadata,
                    event.senderProfile,
                    event.senderId,
                )
                if (event.isOwn || room.isDm) {
                    eventText
                } else {
                    "${event.senderProfile.getDisambiguatedDisplayName(event.senderId)}: $eventText"
                }
            }
            is LatestEventValue.RoomInvite -> event.inviterId?.let { inviterId ->
                event.invitedProfile.getDisplayName()?.let {
                    stringResource(
                        Res.string.membership_change_invited,
                        inviterId,
                        it
                    )
                }
            } ?: stringResource(Res.string.message_placeholder_invite)
        } ?: ""
    }
    Row(
        modifier = Modifier
            .weight(1f)
            .padding(end = Dimens.horizontalItemPaddingBig),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (room.isInvite()) {
            val inviter = room.info.inviter
            inviter?.avatarUrl?.let {
                AvatarImage(
                    source = MediaSource(it),
                    size = 12.dp,
                    displayName = inviter.displayName ?: inviter.userId.value,
                    modifier = Modifier.padding(end = Dimens.horizontalItemPadding),
                )
            }
        }
        Text(
            text = messagePreview,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Content),
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
        if (room.isInvite()) {
            val actionContext = currentActionContext()
            val destinationStateHolder = LocalDestinationState.current
            fun join(): Boolean {
                val result = viewModel.joinRoom(actionContext, scopedRoom.sessionId, room.roomId)
                if (result is ActionResult.Success) {
                    destinationStateHolder?.navigate(Destination.Conversation(scopedRoom.sessionId, scopedRoom.summary.roomId))
                }
                return result is ActionResult.Actioned
            }
            WithTrackedAction(PendingAction.RoomJoin(room.roomId)) { enabled ->
                Button(
                    modifier = Modifier.keyFocusable(
                        actionProvider = actionProvider(
                            primaryAction = if (enabled) InteractionAction.Invoke(::join) else null,
                        ),
                        addMouseFocusable = false,
                        addClickListener = false,
                    ),
                    enabled = enabled,
                    onClick = { join() },
                ) {
                    Text(stringResource(Res.string.action_join))
                }
            }
        }
        ScUnreadCounter(room)
    }
}

data class RoomUnreadCounts(
    val highlightCount: Long,
    val notificationCount: Long,
    val unreadCount: Long,
    val markedUnread: Boolean,
    val unreadUnderestimate: Boolean,
) {
    fun hasUnread() = markedUnread || notificationCount > 0L || highlightCount > 0L || unreadCount > 0L || unreadUnderestimate
    fun canMarkUnread() = !markedUnread && notificationCount == 0L && highlightCount == 0L
}

@Composable
fun RoomSummary.unreadCounts(): RoomUnreadCounts {
    val allowSilentUnreadCount = ScPrefs.RENDER_SILENT_UNREAD.value()
    return if (ScPrefs.CLIENT_GENERATED_UNREAD_COUNTS.value()) {
        val renderUnderestimates = ScPrefs.INDICATE_UNREAD_COUNT_UNDERESTIMATES.value()
        RoomUnreadCounts(
            highlightCount = info.numUnreadMentions,
            notificationCount = info.numUnreadNotifications,
            unreadCount = if (allowSilentUnreadCount) info.numUnreadMessages else 0,
            markedUnread = info.isMarkedUnread,
            unreadUnderestimate = renderUnderestimates && info.unreadCountUnderestimate,
        )
    } else {
        RoomUnreadCounts(
            highlightCount = info.highlightCount,
            notificationCount = info.notificationCount,
            unreadCount = if (allowSilentUnreadCount) info.unreadCount else 0,
            markedUnread = info.isMarkedUnread,
            unreadUnderestimate = false,
        )
    }
}

@Composable
private fun ScUnreadCounter(room: RoomSummary) {
    val (highlightCount, notificationCount, unreadCount, markedUnread, underestimate) = room.unreadCounts()
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
            count = "${formatUnreadCount(highlightCount)}/${formatUnreadCount(fullUnreadToUse, underestimate)}"
            badgeColor = MaterialTheme.scExposures.mentionBadgeColor
        }
        notificationCount > 0 -> {
            count = formatUnreadCount(notificationCount, underestimate)
            badgeColor = if (highlightCount > 0)
                MaterialTheme.scExposures.mentionBadgeColor
            else
                MaterialTheme.scExposures.notificationBadgeColor
        }
        highlightCount > 0 -> {
            count = formatUnreadCount(highlightCount, underestimate)
            badgeColor = MaterialTheme.scExposures.mentionBadgeColor
        }
        markedUnread -> {
            count = "!"
            badgeColor = MaterialTheme.scExposures.notificationBadgeColor
            outlinedBadge = true
        }
        unreadCount > 0 -> {
            count = formatUnreadCount(unreadCount, underestimate)
            badgeColor = MaterialTheme.scExposures.unreadBadgeColor
        }
        underestimate -> {
            count = "?"
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

@Composable
private fun inboxRowKeyboardActionProvider(
    viewModel: InboxViewModel,
    room: ScopedRoomKey,
    isInvite: Boolean,
): HierarchicalKeyboardActionProvider {
    val ownHandler = remember(viewModel, room, isInvite) {
        viewModel.getKeyboardActionProviderForRoom(room.sessionId, room.roomId, isInvite)
    }
    return ownHandler.hierarchicalKeyboardActionProvider()
}
