package chat.schildi.revenge.compose.destination.conversation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PestControlRodent
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import chat.schildi.lib.preferences.ScPrefs
import chat.schildi.resources.toStringHolder
import chat.schildi.revenge.Destination
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.actions.actionProvider
import chat.schildi.revenge.compose.components.AvatarImage
import chat.schildi.revenge.compose.components.ContextMenuActionEntry
import chat.schildi.revenge.compose.components.ContextMenuDecoration
import chat.schildi.revenge.compose.components.TopNavigation
import chat.schildi.revenge.compose.components.TopNavigationCloseOrNavigateToInboxIcon
import chat.schildi.revenge.compose.components.TopNavigationIcon
import chat.schildi.revenge.compose.components.TopNavigationSearchOrTitle
import chat.schildi.revenge.compose.components.TopNavigationTitle
import chat.schildi.revenge.compose.components.WithContextMenu
import chat.schildi.revenge.compose.focus.LocalFocusParent
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.revenge.compose.focus.rememberFocusId
import chat.schildi.revenge.config.keybindings.Action
import chat.schildi.revenge.config.keybindings.DestinationEnum
import chat.schildi.revenge.model.conversation.RoomPreviewViewModel
import chat.schildi.revenge.preferences.isEnabled
import chat.schildi.revenge.preferences.value
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.api.room.CreateTimelineParams
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import shire.res.generated.resources.Res
import shire.res.generated.resources.action_jump_to_unread
import shire.res.generated.resources.action_mark_as_read
import shire.res.generated.resources.action_show_room_members
import shire.res.generated.resources.dev_tools_title
import shire.res.generated.resources.keep_24px
import shire.res.generated.resources.pinned_messages
import shire.res.generated.resources.pref_view_hidden_events_title_short
import shire.res.generated.resources.pref_view_redactions_title_short
import shire.res.generated.resources.room_details_title
import shire.res.generated.resources.thread

@Composable
fun ConversationTopNavigation(
    viewModel: RoomPreviewViewModel,
    hasTimeline: Boolean,
    compact: Boolean,
) {
    val roomInfo = viewModel.roomInfo.collectAsState(null).value
    val title = roomInfo?.name ?: ""
    val avatar = roomInfo?.avatarUrl?.let { MediaSource(it) }
    val keyHandler = LocalKeyboardActionHandler.current
    val focusParent = LocalFocusParent.current
    val destinationState = LocalDestinationState.current
    TopNavigation {
        when (viewModel.timelineParams) {
            is CreateTimelineParams.Threaded -> TopNavigationSearchOrTitle(stringResource(Res.string.thread))
            is CreateTimelineParams.PinnedOnly -> TopNavigationSearchOrTitle(stringResource(Res.string.pinned_messages))
            else -> {
                if (avatar != null) {
                    AvatarImage(
                        source = avatar,
                        size = Dimens.topAppBarIconSize,
                        displayName = title,
                        modifier = Modifier.padding(
                            start = Dimens.windowPadding,
                            top = Dimens.listPadding,
                            bottom = Dimens.listPadding,
                        ),
                    )
                }
                if (hasTimeline) {
                    TopNavigationSearchOrTitle(title) {
                        destinationState?.navigate(Destination.RoomDetails(viewModel.sessionId, viewModel.roomId))
                    }
                } else {
                    TopNavigationTitle(title) {
                        destinationState?.navigate(Destination.RoomDetails(viewModel.sessionId, viewModel.roomId))
                    }
                }
                if (focusParent != null) {
                    // In compact mode, hide what can be reached via room click or room details anyway
                    if (!compact) {
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
                    }
                    if (ScPrefs.DEV_QUICK_OPTIONS.value()) {
                        val focusId = rememberFocusId()
                        WithContextMenu(
                            focusId = focusId,
                            entries = persistentListOf(
                                ContextMenuActionEntry(
                                    Res.string.dev_tools_title.toStringHolder(),
                                    null,
                                    Action.Navigation.NavigateAuto,
                                    persistentListOf(
                                        DestinationEnum.RoomDevTools.destName,
                                        viewModel.sessionId.value,
                                        viewModel.roomId.value,
                                    ),
                                    keyboardShortcut = Key.D,
                                ),
                                ContextMenuActionEntry(
                                    Res.string.pref_view_redactions_title_short.toStringHolder(),
                                    null,
                                    Action.Global.ToggleSetting,
                                    persistentListOf(
                                        ScPrefs.VIEW_REDACTIONS.sKey,
                                    ),
                                    keyboardShortcut = Key.R,
                                    decoration = ContextMenuDecoration.Toggle(ScPrefs.VIEW_REDACTIONS.value()),
                                    enabled = ScPrefs.VIEW_REDACTIONS.isEnabled(),
                                ),
                                ContextMenuActionEntry(
                                    Res.string.pref_view_hidden_events_title_short.toStringHolder(),
                                    null,
                                    Action.Global.ToggleSetting,
                                    persistentListOf(
                                        ScPrefs.VIEW_HIDDEN_EVENTS.sKey,
                                    ),
                                    keyboardShortcut = Key.H,
                                    decoration = ContextMenuDecoration.Toggle(ScPrefs.VIEW_HIDDEN_EVENTS.value()),
                                    enabled = ScPrefs.VIEW_HIDDEN_EVENTS.isEnabled(),
                                ),
                            ),
                        ) { openContextMenu ->
                            TopNavigationIcon(
                                Icons.Default.PestControlRodent,
                                stringResource(Res.string.dev_tools_title),
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
                    }
                    if (hasTimeline) {
                        if (!roomInfo?.pinnedEventIds.isNullOrEmpty()) {
                            TopNavigationIcon(
                                painterResource(Res.drawable.keep_24px),
                                stringResource(Res.string.pinned_messages),
                            ) {
                                destinationState?.navigate(Destination.Conversation(viewModel.sessionId, viewModel.roomId, CreateTimelineParams.PinnedOnly))
                            }
                        }
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
            }
        }
        TopNavigationCloseOrNavigateToInboxIcon()
    }
}
