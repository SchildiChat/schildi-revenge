package chat.schildi.revenge.compose.destination.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.ActionResult
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.InteractionAction
import chat.schildi.revenge.actions.ListActions
import chat.schildi.revenge.actions.LocalKeyboardActionProvider
import chat.schildi.revenge.actions.LocalListActionProvider
import chat.schildi.revenge.actions.LocalRoomContextSuggestionsProvider
import chat.schildi.revenge.actions.actionProvider
import chat.schildi.revenge.actions.currentActionContext
import chat.schildi.revenge.actions.hierarchicalKeyboardActionProvider
import chat.schildi.revenge.actions.plainTextCopyAction
import chat.schildi.revenge.actions.plainTextCopyActionWithMxcUrl
import chat.schildi.revenge.actions.plainTextCopyActionWithUserId
import chat.schildi.revenge.compose.components.AvatarImage
import chat.schildi.revenge.compose.focus.FocusContainer
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.revenge.config.keybindings.Action
import chat.schildi.revenge.model.conversation.RoomPreviewViewModel
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.api.room.CurrentUserMembership
import io.element.android.libraries.matrix.api.room.RoomInfo
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.action_join
import shire.composeapp.generated.resources.action_reject_invite
import shire.composeapp.generated.resources.message_placeholder_invite_by
import shire.composeapp.generated.resources.message_placeholder_invite_by_disambiguated
import shire.composeapp.generated.resources.room_preview_membership_banned
import shire.composeapp.generated.resources.room_preview_membership_invited
import shire.composeapp.generated.resources.room_preview_membership_joined
import shire.composeapp.generated.resources.room_preview_membership_knocked
import shire.composeapp.generated.resources.room_preview_membership_left

@Composable
fun RoomPreviewScreen(
    roomInfo: RoomInfo,
    viewModel: RoomPreviewViewModel,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
) {

    val listState = rememberLazyListState()
    val listAction = remember(listState) { ListActions(listState) }
    FocusContainer(
        LocalListActionProvider provides listAction,
        LocalRoomContextSuggestionsProvider provides viewModel.roomContextSuggestionsProvider,
        LocalKeyboardActionProvider provides viewModel.roomActionProvider.hierarchicalKeyboardActionProvider(),
        role = FocusRole.DESTINATION_ROOT_CONTAINER,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(contentModifier.fillMaxSize()) {
            ConversationTopNavigation(viewModel, hasTimeline = false)
            Box(
                Modifier.fillMaxWidth().padding(Dimens.windowPadding).weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Dimens.verticalArrangement,
                ) {
                    item {
                        Box(
                            Modifier.fillMaxWidth().keyFocusable(
                                role = FocusRole.LIST_ITEM,
                                actionProvider = actionProvider(
                                    copyActions = plainTextCopyActionWithMxcUrl(roomInfo.avatarUrl),
                                )
                            ),
                            contentAlignment = Alignment.Center,
                        ) {
                            AvatarImage(
                                source = roomInfo.avatarUrl?.let { MediaSource(it) },
                                size = 128.dp,
                                displayName = roomInfo.name ?: roomInfo.id.value,
                                modifier = Modifier.keyFocusable(
                                    role = FocusRole.NESTED_AUX_ITEM,
                                    actionProvider = actionProvider(
                                        copyActions = plainTextCopyActionWithMxcUrl(roomInfo.avatarUrl),
                                    )
                                ),
                                allowAnimated = true,
                            )
                        }
                    }
                    if (roomInfo.name != null || roomInfo.privateRoomName != null) {
                        val text = buildString {
                            append(roomInfo.privateRoomName ?: roomInfo.name)
                            if (roomInfo.name != null && roomInfo.privateRoomName != null && roomInfo.name != roomInfo.privateRoomName) {
                                append(" (")
                                append(roomInfo.name)
                                append(")")
                            }
                        }
                        item {
                            Row(
                                Modifier.fillMaxWidth().keyFocusable(
                                    role = FocusRole.LIST_ITEM,
                                    actionProvider = actionProvider(
                                        copyActions = plainTextCopyAction { text },
                                    )
                                ),
                                horizontalArrangement = Arrangement.spacedBy(
                                    Dimens.horizontalItemPadding,
                                    Alignment.CenterHorizontally,
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                SelectionContainer {
                                    Text(
                                        text,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                }
                            }
                        }
                    }
                    if (roomInfo.currentUserMembership != CurrentUserMembership.INVITED || roomInfo.inviter == null) {
                        item {
                            val membershipText = when (roomInfo.currentUserMembership) {
                                CurrentUserMembership.INVITED -> stringResource(Res.string.room_preview_membership_invited)
                                CurrentUserMembership.JOINED -> stringResource(Res.string.room_preview_membership_joined)
                                CurrentUserMembership.LEFT -> stringResource(Res.string.room_preview_membership_left)
                                CurrentUserMembership.KNOCKED -> stringResource(Res.string.room_preview_membership_knocked)
                                CurrentUserMembership.BANNED -> stringResource(Res.string.room_preview_membership_banned)
                            }
                            Box(
                                Modifier.fillMaxWidth().keyFocusable(
                                    role = FocusRole.LIST_ITEM,
                                    actionProvider = actionProvider(
                                        copyActions = plainTextCopyAction { membershipText },
                                    )
                                ),
                                contentAlignment = Alignment.Center,
                            ) {
                                SelectionContainer {
                                    Text(
                                        membershipText,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                }
                            }
                        }
                    }
                    roomInfo.inviter?.let { inviter ->
                        item {
                            val invitedBy = if (inviter.displayName != null) {
                                stringResource(Res.string.message_placeholder_invite_by_disambiguated, inviter.displayName ?: "", inviter.userId.value)
                            } else {
                                stringResource(Res.string.message_placeholder_invite_by, inviter.userId.value)
                            }
                            Row(
                                Modifier.fillMaxWidth().keyFocusable(
                                    role = FocusRole.LIST_ITEM,
                                    actionProvider = actionProvider(
                                        copyActions = plainTextCopyActionWithUserId(inviter.userId) { invitedBy },
                                    )
                                ),
                                horizontalArrangement = Arrangement.spacedBy(Dimens.horizontalItemPadding, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                inviter.avatarUrl?.let { userAvatar ->
                                    AvatarImage(
                                        source = MediaSource(userAvatar),
                                        size = 24.dp,
                                        displayName = inviter.disambiguatedDisplayName,
                                        modifier = Modifier.keyFocusable(
                                            role = FocusRole.NESTED_AUX_ITEM,
                                            actionProvider = actionProvider(
                                                copyActions = plainTextCopyActionWithMxcUrl(userAvatar),
                                            )
                                        ),
                                        allowAnimated = true,
                                    )
                                }
                                SelectionContainer {
                                    Text(
                                        invitedBy,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                }
                            }
                        }
                    }
                    item {
                        val actionContext = currentActionContext()
                        val joinClicked = remember { mutableStateOf(false) }
                        fun join(): Boolean {
                            val actioned = viewModel.roomActionProvider.handleAction(
                                actionContext,
                                Action.Room.Join,
                                emptyList(),
                            ) is ActionResult.Actioned
                            if (actioned) {
                                joinClicked.value = true
                            }
                            return actioned
                        }
                        if (joinClicked.value) return@item
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Button(
                                modifier = Modifier.keyFocusable(
                                    role = FocusRole.LIST_ITEM,
                                    actionProvider = actionProvider(
                                        primaryAction = InteractionAction.Invoke(::join),
                                    ),
                                    addMouseFocusable = false,
                                    addClickListener = false,
                                ),
                                onClick = { join() },
                            ) {
                                Text(stringResource(Res.string.action_join))
                            }
                        }
                    }
                    if (roomInfo.currentUserMembership == CurrentUserMembership.INVITED) {
                        item {
                            val actionContext = currentActionContext()
                            fun leave(): Boolean {
                                return viewModel.roomActionProvider.handleAction(
                                    actionContext,
                                    Action.Room.Leave,
                                    emptyList(),
                                ) is ActionResult.Actioned
                            }
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Button(
                                    modifier = Modifier.keyFocusable(
                                        role = FocusRole.LIST_ITEM,
                                        actionProvider = actionProvider(
                                            primaryAction = InteractionAction.Invoke(::leave),
                                        ),
                                        addMouseFocusable = false,
                                        addClickListener = false,
                                    ),
                                    onClick = { leave() },
                                ) {
                                    Text(stringResource(Res.string.action_reject_invite))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
