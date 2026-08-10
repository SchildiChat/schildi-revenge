package chat.schildi.revenge.compose.destination.conversation.event.sender

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.min
import chat.schildi.revenge.Destination
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.InteractionAction
import chat.schildi.revenge.actions.LocalRoomContextSuggestionsProvider
import chat.schildi.revenge.actions.actionProvider
import chat.schildi.revenge.compose.components.AvatarImage
import chat.schildi.revenge.compose.components.LocalSessionId
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.resources.HardcodedStringHolder
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.api.timeline.item.event.PerMessageProfile
import io.element.android.libraries.matrix.api.timeline.item.event.ProfileDetails
import io.element.android.libraries.matrix.api.timeline.item.event.getDisplayName

@Composable
fun SenderAvatar(
    senderProfile: ProfileDetails,
    senderId: UserId,
    perMessageProfile: PerMessageProfile?,
    size: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = Dimens.avatarShape,
) {
    val avatarUrl = when (senderProfile) {
        is ProfileDetails.Ready -> senderProfile.avatarUrl
        else -> null
    }
    val avatarSource = when {
        perMessageProfile?.avatarFile != null -> perMessageProfile.avatarFile
        perMessageProfile?.avatarUrl != null -> perMessageProfile.avatarUrl?.takeIf(String::isNotEmpty)?.let(::MediaSource)
        else -> avatarUrl?.let(::MediaSource)
    }
    val sessionId = LocalSessionId.current
    val roomId = LocalRoomContextSuggestionsProvider.current?.roomId
    Box(
        modifier.let {
            if (sessionId == null) {
                it
            } else {
                it.keyFocusable(
                    actionProvider = actionProvider(
                        primaryAction = InteractionAction.Navigate(
                            initialTitle = {
                                HardcodedStringHolder(
                                    senderProfile.getDisplayName() ?: senderId.value
                                )
                            },
                            buildDestination = { Destination.UserDetails(sessionId, senderId, roomId) }
                        )
                    )
                )
            }
        }
    ) {
        AvatarImage(
            source = avatarSource,
            size = size,
            shape = shape,
            displayName = perMessageProfile?.displayName ?: senderProfile.getDisplayName() ?: senderId.value,
        )
        if (avatarUrl != null && (perMessageProfile?.avatarFile != null ||
            perMessageProfile?.avatarUrl != null && perMessageProfile.avatarUrl != avatarUrl)) {
            AvatarImage(
                source = MediaSource(avatarUrl),
                size = min(size/2, Dimens.Inbox.accountAvatar),
                shape = shape,
                displayName = senderProfile.getDisplayName() ?: senderId.value,
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
    }
}
