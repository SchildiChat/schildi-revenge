package chat.schildi.revenge.compose.destination.conversation.event.message

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.ActionResult
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.InteractionAction
import chat.schildi.revenge.actions.actionProvider
import chat.schildi.revenge.actions.currentActionContext
import chat.schildi.revenge.compose.components.AvatarImage
import chat.schildi.revenge.compose.components.LocalSessionId
import chat.schildi.revenge.compose.components.thenIf
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.revenge.compose.focus.rememberFocusId
import chat.schildi.revenge.model.conversation.LocalEventActionViewModel
import chat.schildi.revenge.model.conversation.MessageMetadata
import chat.schildi.theme.scExposures
import com.beeper.android.messageformat.MatrixBodyParseResult
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.api.poll.PollAnswer
import io.element.android.libraries.matrix.api.poll.PollKind
import io.element.android.libraries.matrix.api.room.RoomMember
import io.element.android.libraries.matrix.api.timeline.item.EventThreadInfo
import io.element.android.libraries.matrix.api.timeline.item.event.InReplyTo
import io.element.android.libraries.matrix.api.timeline.item.event.PollContent
import kotlinx.collections.immutable.ImmutableMap
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import shire.res.generated.resources.Res
import shire.res.generated.resources.hint_checked
import shire.res.generated.resources.hint_poll_ended
import shire.res.generated.resources.hint_poll_max_selection
import shire.res.generated.resources.hint_poll_unlimited_selection

private const val MAX_VOTER_AVATARS = 7

@Composable
fun PollMessage(
    eventId: EventId?,
    content: PollContent,
    messageMetadata: MessageMetadata?,
    isOwn: Boolean,
    timestamp: TimestampOverlayContent?,
    inReplyTo: InReplyTo?,
    threadInfo: EventThreadInfo?,
    roomMembersById: ImmutableMap<UserId, RoomMember>,
    modifier: Modifier = Modifier,
) {
    val currentSelection = content.answers.filter { answer ->
        content.votes[answer.id]?.any { it == LocalSessionId.current } == true
    }.map { it.id }.toSet()
    val maxSelections = content.maxSelections.toLong()
    val atCap = currentSelection.size.toLong() >= maxSelections
    MessageBubble(
        isOwn = isOwn,
        timestamp = timestamp,
        allowTimestampOverlay = false,
        modifier = modifier.width(IntrinsicSize.Max),
    ) {
        inReplyTo?.let { ReplyContent(it, threadInfo) }

        TextLikeMessageContent(
            messageMetadata?.preFormattedContent ?: MatrixBodyParseResult(content.question),
            allowBigEmojiOnly = false,
        )

        val subtext = when {
            content.endTime != null -> stringResource(Res.string.hint_poll_ended)
            maxSelections > 0 && maxSelections < content.answers.size ->
                pluralStringResource(Res.plurals.hint_poll_max_selection, maxSelections.toInt(), maxSelections)
            else -> stringResource(Res.string.hint_poll_unlimited_selection)
        }

        Text(
            subtext,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Dimens.listPaddingSmall),
        )

        Column(
            verticalArrangement = Dimens.verticalArrangementSmall,
            modifier = Modifier.padding(top = Dimens.listPaddingSmall)
        ) {
            content.answers.forEach { answer ->
                key(answer.id) {
                    val isMyVote = answer.id in currentSelection
                    val enabled =
                        content.endTime == null && (isMyVote || maxSelections == 1L || !atCap) && LocalMessageRenderContext.current == MessageRenderContext.NORMAL
                    PollAnswerOption(
                        pollStartId = eventId,
                        poll = content,
                        answer = answer,
                        isMyVote = isMyVote,
                        enabled = enabled,
                        roomMembersById = roomMembersById,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun PollAnswerOption(
    pollStartId: EventId?,
    poll: PollContent,
    answer: PollAnswer,
    isMyVote: Boolean,
    enabled: Boolean,
    roomMembersById: ImmutableMap<UserId, RoomMember>,
    modifier: Modifier = Modifier,
) {
    val focusId = rememberFocusId()
    val eventActionViewModel = LocalEventActionViewModel.current
    val actionContext = currentActionContext()
    val shape = Dimens.Conversation.messageBubbleShape
    val showResults = poll.kind == PollKind.Disclosed || poll.endTime != null
    Column(
        modifier = modifier.thenIf(enabled) {
            keyFocusable(
                id = focusId,
                role = FocusRole.NESTED_AUX_ITEM,
                actionProvider = actionProvider(
                    primaryAction = if (pollStartId != null && eventActionViewModel != null) {
                        InteractionAction.Invoke {
                            eventActionViewModel.togglePollVote(
                                pollStartId = pollStartId,
                                context = actionContext,
                                poll = poll,
                                answerId = answer.id,
                            ) is ActionResult.Success
                        }
                    } else null,
                ),
                shape = shape,
            )
        }
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .padding(Dimens.Conversation.messageBubbleInnerPadding),
        verticalArrangement = Dimens.verticalArrangement,
    ) {
        val votes = poll.votes[answer.id]
        val maxVoted = poll.votes.values.maxOf { it.size }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Dimens.horizontalArrangement,
            modifier = Modifier.fillMaxWidth(),
        ) {
            AnimatedContent(
                isMyVote,
                transitionSpec = { fadeIn(Dimens.tween()) togetherWith fadeOut(Dimens.tween()) }
            ) { checked ->
                if (checked) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        stringResource(Res.string.hint_checked),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                } else {
                    Box(
                        Modifier
                            .size(16.dp)
                            .border(1.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                    )
                }
            }
            Text(
                answer.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (showResults) {
                votes?.let {
                    Row(
                        modifier = Modifier.padding(start = Dimens.horizontalItemPadding),
                        horizontalArrangement = Arrangement.spacedBy(
                            Dimens.Conversation.receiptPaddingHorizontal,
                            Alignment.Start,
                        ),
                    ) {
                        votes.take(MAX_VOTER_AVATARS).forEachIndexed { index, userId ->
                            key(userId) {
                                val member = roomMembersById[userId]
                                val senderName = member?.displayName ?: userId.value
                                AvatarImage(
                                    source = member?.avatarUrl?.let { MediaSource(it) },
                                    size = Dimens.Conversation.receiptSize,
                                    contentDescription = senderName,
                                    displayName = senderName,
                                    modifier = Modifier.zIndex(-index.toFloat())
                                )
                            }
                        }
                    }
                }
                Text(
                    (votes?.size ?: 0).toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (maxVoted > 0 && votes?.size == maxVoted)
                        MaterialTheme.scExposures.accentColor
                    else
                        MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        AnimatedVisibility(showResults && maxVoted > 0) {
            LinearProgressIndicator(
                modifier = Modifier
                    // LinearProgressIndicator sets width to 240.dp, which makes things wider than necessary.
                    // If we fill max width first to fill parent, then set to something small, the 2nd one becomes
                    // our "min width" to override the 240 one.
                    .fillMaxWidth()
                    .width(24.dp)
                    .padding(bottom = 2.dp)
                    .clip(RoundedCornerShape(50)),
                color = MaterialTheme.scExposures.accentColor,
                trackColor = MaterialTheme.colorScheme.outline,
                progress = { (votes?.size ?: 0).toFloat() / maxVoted },
                drawStopIndicator = {},
            )
        }
    }
}
