package chat.schildi.revenge.compose.destination.inbox

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.key.Key
import chat.schildi.resources.toStringHolder
import chat.schildi.revenge.Destination
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.actions.actionProvider
import chat.schildi.revenge.compose.components.ContextMenuActionEntry
import chat.schildi.revenge.compose.components.TopNavigation
import chat.schildi.revenge.compose.components.TopNavigationIcon
import chat.schildi.revenge.compose.components.TopNavigationSearchOrTitle
import chat.schildi.revenge.compose.components.WithContextMenu
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.revenge.compose.focus.rememberFocusId
import chat.schildi.revenge.config.keybindings.Action
import chat.schildi.revenge.config.keybindings.DestinationEnum
import io.element.android.libraries.matrix.api.core.SessionId
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.jetbrains.compose.resources.stringResource
import shire.res.generated.resources.Res
import shire.res.generated.resources.action_new
import shire.res.generated.resources.app_title_short
import shire.res.generated.resources.create_room
import shire.res.generated.resources.hint_settings
import shire.res.generated.resources.start_chat

@Composable
fun InboxTopNavigation(
    title: String?,
    preferredSessionId: SessionId? = null,
) {
    TopNavigation {
        val destinationState = LocalDestinationState.current
        val keyHandler = LocalKeyboardActionHandler.current
        TopNavigationSearchOrTitle(title ?: stringResource(Res.string.app_title_short))
        if (destinationState != null) {
            val focusId = rememberFocusId()
            WithContextMenu(
                focusId = focusId,
                entries = persistentListOf(
                    ContextMenuActionEntry(
                        Res.string.start_chat.toStringHolder(),
                        rememberVectorPainter(Icons.Default.PersonAdd),
                        Action.Navigation.NavigateAuto,
                        listOfNotNull(
                            DestinationEnum.StartChat.destName,
                            preferredSessionId?.value,
                        ).toPersistentList(),
                        keyboardShortcut = Key.S,
                    ),
                    ContextMenuActionEntry(
                        Res.string.create_room.toStringHolder(),
                        rememberVectorPainter(Icons.Default.GroupAdd),
                        Action.Navigation.NavigateAuto,
                        listOfNotNull(
                            DestinationEnum.CreateRoom.destName,
                            preferredSessionId?.value,
                        ).toPersistentList(),
                        keyboardShortcut = Key.C,
                    ),
                ),
            ) { openContextMenu ->
                TopNavigationIcon(
                    Icons.Default.Add,
                    stringResource(Res.string.action_new),
                    modifier = Modifier.keyFocusable(
                        FocusRole.SHADOW_ITEM,
                        id = focusId,
                        actionProvider = actionProvider(
                            primaryAction = openContextMenu,
                        ),
                        addClickListener = false,
                    ),
                ) {
                    openContextMenu ?: return@TopNavigationIcon
                    keyHandler.executeAction(openContextMenu, destinationState)
                }
            }
            TopNavigationIcon(
                Icons.Default.Settings,
                stringResource(Res.string.hint_settings)
            ) {
                destinationState.navigate(Destination.Settings())
            }
        }
    }
}
