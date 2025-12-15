package chat.schildi.revenge.compose.destination.conversation.event

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.compose.destination.conversation.event.message.TimestampOverlayContent
import chat.schildi.revenge.compose.destination.conversation.event.sender.SenderAvatar
import io.element.android.libraries.matrix.api.timeline.item.event.ProfileDetails

@Composable
fun StateUpdateRow(
    text: AnnotatedString,
    senderProfile: ProfileDetails,
    timestamp: TimestampOverlayContent?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(Dimens.Inbox.avatar), contentAlignment = Alignment.CenterEnd) {
            SenderAvatar(
                senderProfile = senderProfile,
                size = Dimens.Conversation.avatarForState,
            )
        }
        Spacer(Modifier.width(Dimens.Conversation.avatarItemPadding))
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        timestamp?.let {
            Spacer(Modifier.width(Dimens.horizontalItemPadding))
            Text(
                it.timestamp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
