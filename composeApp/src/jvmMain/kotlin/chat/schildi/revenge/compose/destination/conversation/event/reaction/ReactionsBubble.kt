package chat.schildi.revenge.compose.destination.conversation.event.reaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.InteractionAction
import chat.schildi.revenge.actions.actionProvider
import chat.schildi.revenge.compose.components.LocalSessionId
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.revenge.compose.media.imageLoader
import chat.schildi.revenge.model.ConversationViewModel
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.api.timeline.item.event.EventOrTransactionId
import io.element.android.libraries.matrix.api.timeline.item.event.EventReaction
import io.element.android.libraries.matrix.ui.media.MediaRequestData

@Composable
fun ReactionsBubble(
    viewModel: ConversationViewModel,
    eventOrTransactionId: EventOrTransactionId,
    reaction: EventReaction,
    modifier: Modifier = Modifier
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
                role = FocusRole.NESTED_AUX_ITEM,
                actionProvider = actionProvider(
                    primaryAction = InteractionAction.Invoke {
                        viewModel.toggleReaction(eventOrTransactionId, reaction.key)
                    },
                ),
            )
            .padding(
                horizontal = Dimens.Conversation.reactionInnerPaddingHorizontal,
                vertical = Dimens.Conversation.reactionInnerPaddingVertical,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (reaction.key.startsWith("mxc://")) {
            SubcomposeAsyncImage(
                model = MediaRequestData(MediaSource(reaction.key), MediaRequestData.Kind.Content),
                modifier = Modifier.height(
                    LocalDensity.current.run {
                        MaterialTheme.typography.bodyMedium.lineHeight.toDp()
                    }
                ),
                imageLoader = imageLoader(),
                contentScale = ContentScale.Fit,
                alignment = Alignment.Center,
                contentDescription = reaction.key,
            ) {
                when (painter.state.collectAsState().value) {
                    is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                    else -> {
                        Text(
                            reaction.key.take(Dimens.Conversation.reactionMaxLength),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        } else {
            Text(
                reaction.key.take(Dimens.Conversation.reactionMaxLength),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(
            reaction.senders.size.toString(),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
