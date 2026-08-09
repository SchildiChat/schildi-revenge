package chat.schildi.revenge.compose.destination.conversation.event.sender

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import chat.schildi.revenge.compose.components.WithTooltip
import chat.schildi.theme.scExposures
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.timeline.item.event.PerMessageProfile
import io.element.android.libraries.matrix.api.timeline.item.event.ProfileDetails
import io.element.android.libraries.matrix.api.timeline.item.event.getDisplayName

@Composable
fun SenderName(
    senderId: UserId,
    senderProfile: ProfileDetails,
    perMessageProfile: PerMessageProfile?,
    modifier: Modifier = Modifier,
) {
    val nameOverride = perMessageProfile?.displayName?.takeIf { it != senderProfile.getDisplayName() }
    val renderedName = when (senderProfile) {
        is ProfileDetails.Ready -> buildAnnotatedString {
            if (senderProfile.displayName == null && nameOverride == null) {
                append(senderId.value)
            } else {
                append(nameOverride ?: senderProfile.displayName)
                if (senderProfile.displayNameAmbiguous || nameOverride != null) {
                    append(" ")
                    withStyle(
                        SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) {
                        when {
                            senderProfile.displayNameAmbiguous && nameOverride != null -> {
                                append(senderProfile.displayName)
                                append(" ")
                                append(senderId.value)
                            }
                            nameOverride != null -> append(senderProfile.displayName)
                            else -> append(senderId.value)
                        }
                    }
                }
            }
        }
        else -> AnnotatedString(senderId.value)
    }
    // TODO user coloring?
    SelectionContainer(modifier) {
        WithTooltip(senderId.value) {
            Text(
                text = renderedName,
                color = MaterialTheme.scExposures.accentColor,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
