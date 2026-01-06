package chat.schildi.revenge.compose.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chat.schildi.revenge.DateTimeFormat
import chat.schildi.revenge.Dimens
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.media.MediaSource
import kotlinx.collections.immutable.ImmutableList
import java.util.UUID

data class UserTimestampItem<T>(
    val userId: UserId,
    val displayName: String?,
    val avatarUrl: String?,
    val timestamp: Long,
    val extra: T? = null,
)

private fun <T>List<UserTimestampItem<T>>.tooltipTextFunction(): @Composable () -> String = {
    remember(this) {
        joinToString { it.displayName ?: it.userId.value }
    }
}

@Composable
fun <T>WithUserTimestampListPopup(
    focusId: UUID,
    users: ImmutableList<UserTimestampItem<T>>,
    modifier: Modifier = Modifier,
    leadingItemContent: @Composable (T) -> Unit = {},
    trailingItemContent: @Composable (T) -> Unit = {},
    content: @Composable () -> Unit,
) {
    WithTooltip(
        users.tooltipTextFunction(),
        modifier,
        isPersistent = true,
    ) {
        WithContextMenu(
            focusId = focusId,
            popupContent = {
                UserTimestampList(
                    users = users,
                    leadingItemContent = leadingItemContent,
                    trailingItemContent = trailingItemContent,
                )
            },
            content = content,
        )
    }
}

@Composable
fun <T>UserTimestampList(
    users: ImmutableList<UserTimestampItem<T>>,
    leadingItemContent: @Composable (T) -> Unit = {},
    trailingItemContent: @Composable (T) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // TODO popups don't like lazy columns, but the fixed size is ugly too. Normal column could be expensive in huge rooms.
    Column(modifier) {
        val sortedUsers = remember(users) { users.sortedBy { it.timestamp } }
        sortedUsers.forEach { user ->
            key(user.userId) {
                UserTimestampListItem(
                    user = user,
                    leadingItemContent = leadingItemContent,
                    trailingItemContent = trailingItemContent,
                )
            }
        }
    }
}

@Composable
private fun <T> UserTimestampListItem(
    user: UserTimestampItem<T>,
    leadingItemContent: @Composable (T) -> Unit = {},
    trailingItemContent: @Composable (T) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.padding(horizontal = Dimens.horizontalItemPadding, vertical = Dimens.listPadding),
        horizontalArrangement = Dimens.horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        user.extra?.let {
            leadingItemContent(it)
        }
        AvatarImage(
            source = user.avatarUrl?.let { MediaSource(it) },
            size = 36.dp,
            displayName = user.displayName ?: user.userId.value,
        )
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                user.displayName ?: user.userId.value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            if (user.displayName != null && user.displayName != user.userId.value) {
                Text(
                    user.userId.value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
        Text(
            DateTimeFormat.formatTimeOrDateTime(user.timestamp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1,
        )
        user.extra?.let {
            trailingItemContent(it)
        }
    }
}
