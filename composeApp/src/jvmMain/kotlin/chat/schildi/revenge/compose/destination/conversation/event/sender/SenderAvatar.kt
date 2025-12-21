package chat.schildi.revenge.compose.destination.conversation.event.sender

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.compose.components.AvatarImage
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.api.timeline.item.event.ProfileDetails
import io.element.android.libraries.matrix.api.timeline.item.event.getDisplayName

@Composable
fun SenderAvatar(
    senderProfile: ProfileDetails,
    senderId: UserId,
    modifier: Modifier = Modifier,
    size: Dp = Dimens.Conversation.avatar,
    shape: Shape = Dimens.avatarShape,
) {
    val avatarUrl = when (senderProfile) {
        is ProfileDetails.Ready -> senderProfile.avatarUrl
        else -> null
    }
    AvatarImage(
        source = avatarUrl?.let { MediaSource(it) },
        size = size,
        shape = shape,
        displayName = senderProfile.getDisplayName() ?: senderId.value,
        modifier = modifier,
    )
}
