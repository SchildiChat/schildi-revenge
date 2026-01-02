package chat.schildi.revenge.compose.destination.conversation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.runtime.Composable
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.compose.components.TopNavigation
import chat.schildi.revenge.compose.components.TopNavigationCloseOrNavigateToInboxIcon
import chat.schildi.revenge.compose.components.TopNavigationIcon
import chat.schildi.revenge.compose.components.TopNavigationSearchOrTitle
import chat.schildi.revenge.compose.focus.LocalFocusParent
import chat.schildi.revenge.config.keybindings.Action
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.action_jump_to_unread
import shire.composeapp.generated.resources.action_mark_as_read

@Composable
fun ConversationTopNavigation(
    title: String,
) {
    val keyHandler = LocalKeyboardActionHandler.current
    val focusParent = LocalFocusParent.current
    TopNavigation {
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
