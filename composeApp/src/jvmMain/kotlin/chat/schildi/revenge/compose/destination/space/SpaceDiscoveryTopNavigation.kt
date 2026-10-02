package chat.schildi.revenge.compose.destination.space

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import chat.schildi.revenge.Destination
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.compose.components.AvatarImage
import chat.schildi.revenge.compose.components.TopNavigation
import chat.schildi.revenge.compose.components.TopNavigationCloseOrNavigateToInboxIcon
import chat.schildi.revenge.compose.components.TopNavigationIcon
import chat.schildi.revenge.compose.components.TopNavigationSearchOrTitle
import chat.schildi.revenge.model.SpaceDiscoveryViewModel
import io.element.android.libraries.matrix.api.media.MediaSource
import org.jetbrains.compose.resources.stringResource
import shire.res.generated.resources.Res
import shire.res.generated.resources.action_show_room_members
import shire.res.generated.resources.room_details_title
import shire.res.generated.resources.room_type_space
import kotlin.jvm.optionals.getOrNull

@Composable
fun SpaceDiscoveryTopNavigation(
    viewModel: SpaceDiscoveryViewModel,
    compact: Boolean,
) {
    val space = viewModel.currentSpace.collectAsState().value?.getOrNull()
    val title = space?.displayName
    val avatar = space?.avatarUrl?.let { MediaSource(it) }
    val destinationState = LocalDestinationState.current
    TopNavigation {
        if (avatar != null) {
            AvatarImage(
                source = avatar,
                size = Dimens.topAppBarIconSize,
                displayName = title ?: viewModel.roomId.value,
                modifier = Modifier.padding(
                    start = Dimens.windowPadding,
                    top = Dimens.listPadding,
                    bottom = Dimens.listPadding,
                ),
            )
        }
        TopNavigationSearchOrTitle(title ?: stringResource(Res.string.room_type_space))
        TopNavigationIcon(
            Icons.Default.Info,
            stringResource(Res.string.room_details_title),
        ) {
            destinationState?.navigate(Destination.RoomDetails(viewModel.sessionId, viewModel.roomId))
        }
        if (!compact) {
            TopNavigationIcon(
                Icons.Default.Group,
                stringResource(Res.string.action_show_room_members),
            ) {
                destinationState?.navigate(
                    Destination.RoomMembers(
                        viewModel.sessionId,
                        viewModel.roomId,
                    )
                )
            }
        }
        TopNavigationCloseOrNavigateToInboxIcon()
    }
}
