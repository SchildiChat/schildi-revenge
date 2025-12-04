package chat.schildi.revenge.compose.destination.conversation.event.sender

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.compose.components.AvatarImage
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.api.timeline.item.event.ProfileDetails

@Composable
fun SenderAvatar(senderProfile: ProfileDetails, modifier: Modifier = Modifier) {
    val avatarUrl = when (senderProfile) {
        is ProfileDetails.Ready -> senderProfile.avatarUrl
        else -> null
    }
    AvatarImage(
        source = avatarUrl?.let { MediaSource(it) },
        size = Dimens.Conversation.avatar,
        modifier = modifier,
    )
}
