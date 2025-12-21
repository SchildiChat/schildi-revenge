package chat.schildi.revenge.compose.destination.conversation.event

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import chat.schildi.revenge.EventTextFormat
import chat.schildi.revenge.compose.destination.conversation.event.message.TimestampOverlayContent
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.timeline.item.event.ProfileDetails
import io.element.android.libraries.matrix.api.timeline.item.event.RoomMembershipContent

@Composable
fun RoomMembershipRow(
    content: RoomMembershipContent,
    senderId: UserId,
    senderProfile: ProfileDetails,
    timestamp: TimestampOverlayContent?,
    modifier: Modifier = Modifier,
) {
    val text = EventTextFormat.roomMembershipToText(content, senderProfile, senderId)
    StateUpdateRow(
        text = AnnotatedString(text),
        senderProfile = senderProfile,
        senderId = senderId,
        timestamp = timestamp,
        modifier = modifier,
    )
}
