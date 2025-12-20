package chat.schildi.revenge.compose.destination.conversation.event

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import chat.schildi.revenge.EventTextFormat
import chat.schildi.revenge.compose.destination.conversation.event.message.TimestampOverlayContent
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.timeline.item.event.ProfileDetails
import io.element.android.libraries.matrix.api.timeline.item.event.StateContent
import io.element.android.libraries.matrix.api.timeline.item.event.getDisambiguatedDisplayName
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.message_placeholder_state_event

@Composable
fun StateEventRow(
    content: StateContent,
    senderId: UserId,
    senderProfile: ProfileDetails,
    timestamp: TimestampOverlayContent?,
    modifier: Modifier = Modifier,
) {
    val text = EventTextFormat.stateEventToText(content, senderProfile, senderId)
    StateUpdateRow(
        text = AnnotatedString(text),
        senderProfile = senderProfile,
        timestamp = timestamp,
        modifier = modifier,
    )
}

@Composable
fun StateEventFallbackRow(
    eventType: String,
    senderId: UserId,
    senderProfile: ProfileDetails,
    timestamp: TimestampOverlayContent?,
    modifier: Modifier = Modifier,
) {
    val senderName = senderProfile.getDisambiguatedDisplayName(senderId)
    StateUpdateRow(
        text = AnnotatedString(stringResource(Res.string.message_placeholder_state_event, senderName, eventType)),
        senderProfile = senderProfile,
        timestamp = timestamp,
        modifier = modifier,
    )
}
