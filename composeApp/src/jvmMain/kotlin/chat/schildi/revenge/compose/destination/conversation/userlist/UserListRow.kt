package chat.schildi.revenge.compose.destination.conversation.userlist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import chat.schildi.revenge.DateTimeFormat
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.ActionProvider
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.actionProvider
import chat.schildi.revenge.actions.hierarchicalKeyboardActionProvider
import chat.schildi.revenge.actions.plainTextCopyActionWithUserId
import chat.schildi.revenge.compose.components.AvatarImage
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.revenge.model.userlist.AbstractUserListViewModel
import chat.schildi.revenge.model.userlist.UserListItem
import io.element.android.libraries.matrix.api.media.MediaSource

@Composable
fun <T : UserListItem>UserListRow(
    item: T,
    viewModel: AbstractUserListViewModel<T>,
    actionProvider: ActionProvider = actionProvider(
        keyActions = viewModel.getItemActionHandler(item.userId).hierarchicalKeyboardActionProvider(),
        copyActions = plainTextCopyActionWithUserId(item.userId) { item.displayName },
    ),
    modifier: Modifier = Modifier,
    leadingContent: @Composable () -> Unit = {},
    trailingContent: @Composable () -> Unit = {},
) {
    Row(
        modifier
            .keyFocusable(
                FocusRole.LIST_ITEM,
                actionProvider = actionProvider,
            )
            .padding(horizontal = Dimens.windowPadding, vertical = Dimens.listPadding),
        horizontalArrangement = Dimens.horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leadingContent()
        AvatarImage(
            source = item.avatarUrl?.let { MediaSource(it) },
            size = Dimens.Conversation.avatar,
            shape = Dimens.avatarShape,
            displayName = item.displayName ?: item.userId.value,
            modifier = modifier,
        )
        SelectionContainer(Modifier.weight(1f)) {
            Column {
                Text(
                    item.displayName ?: item.userId.value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (item.displayName != null) {
                    Text(
                        item.userId.value,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Box {
            trailingContent()
        }
    }
}

@Composable
fun UserListTimestamp(
    timestamp: Long,
    modifier: Modifier = Modifier,
) {
    Text(
        DateTimeFormat.formatTimeOrDateTime(timestamp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
