package chat.schildi.revenge.compose.destination.conversation.event

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chat.schildi.revenge.DateTimeFormat
import chat.schildi.revenge.Destination
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.buildNavigationActionProvider
import chat.schildi.revenge.compose.components.AvatarImage
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.theme.scExposures
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.core.ThreadId
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.api.room.CreateTimelineParams
import io.element.android.libraries.matrix.api.timeline.item.EventThreadInfo
import io.element.android.libraries.matrix.api.timeline.item.event.getAvatarUrl
import io.element.android.libraries.matrix.api.timeline.item.event.getDisambiguatedDisplayName
import io.element.android.libraries.matrix.api.timeline.item.event.getDisplayName
import org.jetbrains.compose.resources.pluralStringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.n_threaded_replies

@Composable
fun ColumnScope.ThreadRootInfoRow(
    threadInfo: EventThreadInfo.ThreadRoot,
    sessionId: SessionId,
    roomId: RoomId,
    eventId: EventId?,
    messageIsOwn: Boolean,
    modifier: Modifier = Modifier,
) {
    val alignment = if (messageIsOwn) Alignment.End else Alignment.Start
    Row(
        modifier = modifier
            .padding(
                top = Dimens.Conversation.threadInfoPaddingVertical,
                start = if (messageIsOwn)
                    Dimens.Conversation.otherSidePadding
                else
                    Dimens.Conversation.avatarReservation,
                end = if (messageIsOwn)
                    0.dp
                else
                    Dimens.Conversation.otherSidePadding,
            )
            .align(alignment)
            .keyFocusable(
                role = FocusRole.NESTED_AUX_ITEM,
                actionProvider = buildNavigationActionProvider {
                    Destination.Conversation(
                        sessionId = sessionId,
                        roomId = roomId,
                        timelineParams = eventId?.value?.let {
                            CreateTimelineParams.Threaded(ThreadId(it))
                        },
                    )
                },
            )
            .padding(horizontal = Dimens.Conversation.threadInfoPaddingHorizontal),
        horizontalArrangement = Dimens.horizontalArrangementSmall,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val numReplies = threadInfo.summary.numberOfReplies
        Text(
            pluralStringResource(
                Res.plurals.n_threaded_replies,
                numReplies.coerceAtMost(Integer.MAX_VALUE.toLong()).toInt(),
                numReplies
            ),
            color = MaterialTheme.scExposures.threadHint,
            style = MaterialTheme.typography.labelMedium,
        )
        threadInfo.summary.latestEvent.dataOrNull()?.let { latestEvent ->
            latestEvent.senderProfile.getAvatarUrl()?.let { latestSenderAvatar ->
                AvatarImage(
                    source = MediaSource(latestSenderAvatar),
                    size = Dimens.Conversation.receiptSize,
                    contentDescription = latestEvent.senderProfile.getDisambiguatedDisplayName(latestEvent.senderId),
                    displayName = latestEvent.senderProfile.getDisplayName() ?: "",
                    modifier = modifier,
                )
            }
            Text(
                DateTimeFormat.formatTimestampAsDateOrTime(latestEvent.timestamp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
