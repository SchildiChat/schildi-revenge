package chat.schildi.revenge.compose.destination.conversation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chat.schildi.revenge.Destination
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.compose.components.AvatarImage
import chat.schildi.revenge.compose.components.TopNavigation
import chat.schildi.revenge.compose.components.TopNavigationCloseOrNavigateToInboxIcon
import chat.schildi.revenge.compose.components.TopNavigationIcon
import chat.schildi.revenge.compose.components.TopNavigationSearchOrTitle
import chat.schildi.revenge.compose.components.TopNavigationTitle
import chat.schildi.revenge.compose.focus.LocalFocusParent
import chat.schildi.revenge.config.keybindings.Action
import chat.schildi.revenge.model.conversation.RoomPreviewViewModel
import io.element.android.libraries.matrix.api.media.MediaSource
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.action_jump_to_unread
import shire.composeapp.generated.resources.action_mark_as_read
import shire.composeapp.generated.resources.action_show_room_members
import shire.composeapp.generated.resources.room_details_title
import shire.composeapp.generated.resources.thread

@Composable
fun ConversationTopNavigation(
    viewModel: RoomPreviewViewModel,
    hasTimeline: Boolean,
) {
    val roomInfo = viewModel.roomInfo.collectAsState(null).value
    val title = roomInfo?.name ?: ""
    val avatar = roomInfo?.avatarUrl?.let { MediaSource(it) }
    val keyHandler = LocalKeyboardActionHandler.current
    val focusParent = LocalFocusParent.current
    val destinationState = LocalDestinationState.current
    TopNavigation {
        if (viewModel.threadId == null) {
            if (avatar != null) {
                AvatarImage(
                    source = avatar,
                    size = 36.dp,
                    displayName = title,
                    modifier = Modifier.padding(
                        start = Dimens.windowPadding,
                        top = Dimens.listPadding,
                        bottom = Dimens.listPadding,
                    ),
                )
            }
            if (hasTimeline) {
                TopNavigationSearchOrTitle(title)
            } else {
                TopNavigationTitle(title)
            }
            if (focusParent != null) {
                TopNavigationIcon(
                    Icons.Default.Info,
                    stringResource(Res.string.room_details_title),
                ) {
                    destinationState?.navigate(Destination.RoomDetails(viewModel.sessionId, viewModel.roomId))
                }
                TopNavigationIcon(
                    Icons.Default.Group,
                    stringResource(Res.string.action_show_room_members),
                ) {
                    destinationState?.navigate(Destination.RoomMembers(viewModel.sessionId, viewModel.roomId))
                }
                if (hasTimeline) {
                    TopNavigationIcon(
                        Icons.Default.Update,
                        stringResource(Res.string.action_jump_to_unread),
                    ) {
                        keyHandler.handleAction(
                            focusItem = focusParent.uuid,
                            action = Action.Conversation.JumpToFullyRead,
                        )
                    }
                    TopNavigationIcon(
                        Icons.Default.Visibility,
                        stringResource(Res.string.action_mark_as_read),
                    ) {
                        keyHandler.handleAction(
                            focusItem = focusParent.uuid,
                            action = Action.Room.MarkRoomRead,
                        )
                        keyHandler.handleAction(
                            focusItem = focusParent.uuid,
                            action = Action.Room.MarkRoomFullyRead,
                        )
                    }
                }
            }
        } else {
            TopNavigationSearchOrTitle(stringResource(Res.string.thread))
        }
        TopNavigationCloseOrNavigateToInboxIcon()
    }
}
