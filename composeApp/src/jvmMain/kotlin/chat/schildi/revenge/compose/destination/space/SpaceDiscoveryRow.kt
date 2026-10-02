package chat.schildi.revenge.compose.destination.space

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import chat.schildi.revenge.Destination
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.actions.ActionResult
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.InteractionAction
import chat.schildi.revenge.actions.actionProvider
import chat.schildi.revenge.actions.buildNavigationActionProvider
import chat.schildi.revenge.actions.currentActionContext
import chat.schildi.revenge.actions.hierarchicalKeyboardActionProvider
import chat.schildi.revenge.compose.components.AvatarImage
import chat.schildi.revenge.compose.components.WithTrackedAction
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.revenge.compose.focus.rememberFocusId
import chat.schildi.revenge.model.PendingAction
import chat.schildi.revenge.model.SpaceDiscoveryViewModel
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.api.room.CurrentUserMembership
import io.element.android.libraries.matrix.api.spaces.SpaceRoom
import org.jetbrains.compose.resources.stringResource
import shire.res.generated.resources.Res
import shire.res.generated.resources.action_join
import shire.res.generated.resources.room_type_space

@Composable
fun SpaceDiscoveryRow(
    viewModel: SpaceDiscoveryViewModel,
    room: SpaceRoom,
    modifier: Modifier = Modifier,
) {
    val focusId = rememberFocusId()
    val isJoined = room.state == CurrentUserMembership.JOINED
    val isInvite = room.state == CurrentUserMembership.INVITED
    val roomActionProvider = remember(viewModel, room) {
        viewModel.getKeyboardActionProviderForRoom(
            viewModel.sessionId,
            room.roomId,
            isInvite,
            isJoined,
            room.canonicalAlias,
            room.via,
        )
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.Inbox.avatar + Dimens.listPadding * 2)
            .keyFocusable(
                FocusRole.LIST_ITEM,
                focusId,
                actionProvider = buildNavigationActionProvider(
                    keyActions = roomActionProvider.hierarchicalKeyboardActionProvider(),
                ) {
                    if (room.isSpace && isJoined) {
                        Destination.SpaceDiscovery(
                            viewModel.sessionId,
                            room.roomId,
                        )
                    } else {
                        Destination.Conversation(
                            viewModel.sessionId,
                            room.roomId,
                            alias = room.canonicalAlias,
                            joinServerNames = room.via
                        )
                    }
                },
            )
            .padding(
                horizontal = Dimens.windowPadding,
                vertical = Dimens.listPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarImage(
            source = room.avatarUrl?.let(::MediaSource),
            size = Dimens.Inbox.avatar,
            displayName = room.displayName,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(
                    start = Dimens.Inbox.avatarItemPadding,
                    end = Dimens.horizontalItemPaddingBig,
                ),
            verticalArrangement = Arrangement.spacedBy(Dimens.listPaddingSmall, Alignment.CenterVertically),
        ) {
            Text(
                style = MaterialTheme.typography.titleMedium,
                text = room.displayName,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val text = buildAnnotatedString {
                if (room.isSpace) {
                    val spaceText = stringResource(Res.string.room_type_space)
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                        append(spaceText)
                        if (!room.topic.isNullOrBlank()) {
                            append(": ")
                        }
                    }
                }
                if (!room.topic.isNullOrBlank()) {
                    append(room.topic)
                }
            }
            if (text.isNotEmpty()) {
                Text(
                    style = MaterialTheme.typography.bodyMedium,
                    text = text,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (!isJoined) {
            val actionContext = currentActionContext()
            val destinationStateHolder = LocalDestinationState.current
            fun join(): Boolean {
                val result = viewModel.joinRoom(actionContext, room)
                if (result is ActionResult.Success) {
                    destinationStateHolder?.navigate(
                        Destination.Conversation(
                            viewModel.sessionId,
                            room.roomId,
                            alias = room.canonicalAlias,
                            joinServerNames = room.via,
                        )
                    )
                }
                return result is ActionResult.Actioned
            }
            WithTrackedAction(PendingAction.RoomJoin(room.roomId)) { enabled ->
                Button(
                    modifier = Modifier
                        .padding(start = Dimens.horizontalItemPadding)
                        .keyFocusable(
                            actionProvider = actionProvider(
                                primaryAction = if (enabled) InteractionAction.Invoke(::join) else null,
                            ),
                            addMouseFocusable = false,
                            addClickListener = false,
                        ),
                    enabled = enabled,
                    onClick = { join() },
                ) {
                    Text(stringResource(Res.string.action_join))
                }
            }
        }
    }
}
