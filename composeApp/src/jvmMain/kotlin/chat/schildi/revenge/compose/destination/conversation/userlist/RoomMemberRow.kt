package chat.schildi.revenge.compose.destination.conversation.userlist

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import chat.schildi.revenge.model.userlist.RoomMemberItem
import chat.schildi.revenge.model.userlist.RoomMemberListViewModel
import chat.schildi.revenge.plaintext.UserRoleFormat
import io.element.android.libraries.matrix.api.room.RoomMember.Role

@Composable
fun RoomMemberRow(
    roomMember: RoomMemberItem,
    viewModel: RoomMemberListViewModel,
    modifier: Modifier = Modifier,
) {
    UserListRow(
        item = roomMember,
        viewModel = viewModel,
        modifier = modifier,
    ) {
        if (roomMember.value.role != Role.User) {
            Text(
                text = UserRoleFormat.formatUserRoleWithPowerLevel(
                    roomMember.value.role,
                    roomMember.value.powerLevel.takeIf { it != Long.MAX_VALUE },
                ).render(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
