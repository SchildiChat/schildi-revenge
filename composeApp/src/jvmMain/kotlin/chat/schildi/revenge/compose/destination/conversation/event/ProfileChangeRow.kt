package chat.schildi.revenge.compose.destination.conversation.event

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import chat.schildi.revenge.compose.destination.conversation.event.message.TimestampOverlayContent
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.timeline.item.event.ProfileChangeContent
import io.element.android.libraries.matrix.api.timeline.item.event.ProfileDetails
import io.element.android.libraries.matrix.api.timeline.item.event.getDisambiguatedDisplayName
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.profile_update_avatar
import shire.composeapp.generated.resources.profile_update_cleared_name
import shire.composeapp.generated.resources.profile_update_name
import shire.composeapp.generated.resources.profile_update_name_and_avatar
import shire.composeapp.generated.resources.profile_update_none
import shire.composeapp.generated.resources.profile_update_set_name
import shire.composeapp.generated.resources.profile_update_set_name_and_avatar

@Composable
fun ProfileChangeRow(
    content: ProfileChangeContent,
    senderId: UserId,
    senderProfile: ProfileDetails,
    timestamp: TimestampOverlayContent?,
    modifier: Modifier = Modifier,
) {
    val senderName = senderProfile.getDisambiguatedDisplayName(senderId)
    val text = if (content.prevDisplayName == content.displayName) {
        if (content.prevAvatarUrl == content.avatarUrl) {
            stringResource(Res.string.profile_update_none, senderName)
        } else {
            stringResource(Res.string.profile_update_avatar, senderName)
        }
    } else if (content.prevAvatarUrl != content.avatarUrl) {
        if (content.prevDisplayName == null) {
            stringResource(Res.string.profile_update_set_name_and_avatar, senderName)
        } else {
            stringResource(Res.string.profile_update_name_and_avatar, senderName, content.prevDisplayName ?: "")
        }
    } else {
        when {
            content.prevDisplayName == null -> stringResource(Res.string.profile_update_set_name, senderName)
            content.displayName == null -> stringResource(Res.string.profile_update_cleared_name, senderName)
            else -> stringResource(Res.string.profile_update_name, senderName, content.prevDisplayName ?: "")
        }
    }
    StateUpdateRow(
        text = AnnotatedString(text),
        senderProfile = senderProfile,
        timestamp = timestamp,
        modifier = modifier,
    )
}
