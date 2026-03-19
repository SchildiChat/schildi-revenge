package chat.schildi.revenge.compose.destination.conversation.event.reaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.InteractionAction
import chat.schildi.revenge.actions.actionProvider
import chat.schildi.revenge.compose.components.LocalSessionId
import chat.schildi.revenge.compose.components.UserTimestampItem
import chat.schildi.revenge.compose.components.WithUserTimestampListPopup
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.revenge.compose.focus.rememberFocusId
import chat.schildi.revenge.compose.media.imageLoader
import chat.schildi.revenge.compose.util.containsOnlyEmojis
import chat.schildi.revenge.model.conversation.ConversationViewModel
import chat.schildi.theme.rememberEmojiFontFamily
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.api.room.RoomMember
import io.element.android.libraries.matrix.api.timeline.item.event.EventOrTransactionId
import io.element.android.libraries.matrix.api.timeline.item.event.EventReaction
import io.element.android.libraries.matrix.ui.media.MediaRequestData
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableList

@Composable
fun ReactionsBubble(
    viewModel: ConversationViewModel,
    eventOrTransactionId: EventOrTransactionId,
    reaction: EventReaction,
    roomMembersById: ImmutableMap<UserId, RoomMember>,
    modifier: Modifier = Modifier
) {
    val focusId = rememberFocusId()
    val userTimestamps = remember(reaction, roomMembersById) {
        reaction.senders.map { sender ->
            val member = roomMembersById[sender.senderId]
            UserTimestampItem(
                userId = sender.senderId,
                displayName = member?.displayName,
                avatarUrl = member?.avatarUrl,
                timestamp = sender.timestamp,
                extra = reaction.key,
            )
        }.toImmutableList()
    }
    WithUserTimestampListPopup(
        focusId = focusId,
        users = userTimestamps,
        modifier = modifier,
        tooltipPrefix = reaction.shortcode?.plus(" ") ?: "",
        leadingItemContent = {
            ReactionContent(reaction.key, reaction.shortcode, MaterialTheme.typography.headlineSmall)
        }
    ) {
        val includesSelf = reaction.senders.any { it.senderId == LocalSessionId.current }
        Row(
            modifier.let {
                if (includesSelf) {
                    it.background(MaterialTheme.colorScheme.surfaceVariant, Dimens.Conversation.reactionShape)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.onSurface,
                            Dimens.Conversation.reactionShape
                        )
                } else {
                    it.background(MaterialTheme.colorScheme.surfaceContainerHigh, Dimens.Conversation.reactionShape)
                }
            }
                .clip(Dimens.Conversation.reactionShape)
                .keyFocusable(
                    id = focusId,
                    role = FocusRole.NESTED_AUX_ITEM,
                    actionProvider = actionProvider(
                        primaryAction = InteractionAction.Invoke {
                            viewModel.toggleReaction(eventOrTransactionId, reaction.key)
                        },
                        secondaryAction = InteractionAction.ContextMenu(focusId, null),
                    ),
                )
                .padding(
                    horizontal = Dimens.Conversation.reactionInnerPaddingHorizontal,
                    vertical = Dimens.Conversation.reactionInnerPaddingVertical,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ReactionContent(reaction.key, reaction.shortcode)
            Text(
                reaction.senders.size.toString(),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
