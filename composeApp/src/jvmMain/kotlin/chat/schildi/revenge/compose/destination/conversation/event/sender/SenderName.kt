package chat.schildi.revenge.compose.destination.conversation.event.sender

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import chat.schildi.theme.scExposures
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.timeline.item.event.ProfileTimelineDetails

@Composable
fun SenderName(senderId: UserId, senderProfile: ProfileTimelineDetails, modifier: Modifier = Modifier) {
    val renderedName = when (senderProfile) {
        is ProfileTimelineDetails.Ready -> buildAnnotatedString {
            if (senderProfile.displayName == null) {
                append(senderId.value)
            } else {
                append(senderProfile.displayName)
                if (senderProfile.displayNameAmbiguous) {
                    append(" ")
                    withStyle(
                        SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) {
                        append(senderId.value)
                    }
                }
            }
        }
        else -> AnnotatedString(senderId.value)
    }
    // TODO user coloring
    Text(
        text = renderedName,
        color = MaterialTheme.scExposures.accentColor,
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier,
    )
}
