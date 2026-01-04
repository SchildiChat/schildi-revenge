package chat.schildi.revenge.compose.destination.conversation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.compose.components.AvatarImage
import chat.schildi.revenge.compose.components.TopNavigation
import chat.schildi.revenge.compose.components.TopNavigationCloseOrNavigateToInboxIcon
import chat.schildi.revenge.compose.components.TopNavigationIcon
import chat.schildi.revenge.compose.components.TopNavigationSearchOrTitle
import chat.schildi.revenge.compose.focus.LocalFocusParent
import chat.schildi.revenge.config.keybindings.Action
import io.element.android.libraries.matrix.api.media.MediaSource
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.action_jump_to_unread
import shire.composeapp.generated.resources.action_mark_as_read

@Composable
fun ConversationTopNavigation(
    title: String,
    avatar: MediaSource?,
) {
    val keyHandler = LocalKeyboardActionHandler.current
    val focusParent = LocalFocusParent.current
    TopNavigation {
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
        TopNavigationSearchOrTitle(title)
        if (focusParent != null) {
            TopNavigationIcon(
                Icons.Default.Update,
                stringResource(Res.string.action_jump_to_unread),
            ) {
                keyHandler.handleAction(
                    focusItem = focusParent.uuid,
                    action = Action.Conversation.JumpToFullyRead,
                    args = emptyList(),
                )
            }
            TopNavigationIcon(
                Icons.Default.Visibility,
                stringResource(Res.string.action_mark_as_read),
            ) {
                keyHandler.handleAction(
                    focusItem = focusParent.uuid,
                    action = Action.Room.MarkRoomRead,
                    args = emptyList(),
                )
                keyHandler.handleAction(
                    focusItem = focusParent.uuid,
                    action = Action.Room.MarkRoomFullyRead,
                    args = emptyList(),
                )
            }
        }
        TopNavigationCloseOrNavigateToInboxIcon()
    }
}
