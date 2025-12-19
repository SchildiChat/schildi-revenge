package chat.schildi.revenge.compose.destination.inbox

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.InteractionAction
import chat.schildi.revenge.actions.defaultActionProvider
import chat.schildi.revenge.compose.components.AvatarImage
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.revenge.model.InboxAccount
import chat.schildi.revenge.model.InboxViewModel
import chat.schildi.revenge.model.spaces.SpaceAggregationDataSource
import chat.schildi.theme.scExposures
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.api.sync.SyncState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.hint_sync_state_error
import shire.composeapp.generated.resources.hint_sync_state_idle
import shire.composeapp.generated.resources.hint_sync_state_offline
import shire.composeapp.generated.resources.hint_sync_state_running
import shire.composeapp.generated.resources.hint_sync_state_terminated

@Composable
fun AccountSelectorRow(
    viewModel: InboxViewModel,
    accounts: ImmutableList<InboxAccount>,
    unreadCounts: ImmutableMap<SessionId, SpaceAggregationDataSource.SpaceUnreadCounts>,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier,
        horizontalArrangement = Dimens.horizontalArrangement,
        contentPadding = PaddingValues(horizontal = Dimens.windowPadding),
    ) {
        items(accounts) { account ->
            AccountButton(viewModel, account, unreadCounts[account.user.userId])
        }
    }
}

@Composable
fun AccountButton(
    viewModel: InboxViewModel,
    account: InboxAccount,
    unreadCounts: SpaceAggregationDataSource.SpaceUnreadCounts?,
    modifier: Modifier = Modifier,
) {
    val outlineColor = animateColorAsState(
        if (account.isSelected)
            MaterialTheme.scExposures.accentColor
        else
            Color.Transparent
    ).value
    val backgroundColor = animateColorAsState(
        if (account.isCurrentlyVisible)
            MaterialTheme.scExposures.bubbleBgIncoming // TODO
        else
            MaterialTheme.colorScheme.surfaceDim
    ).value
    Row(modifier
        .keyFocusable(
            actionProvider = defaultActionProvider(
                primaryAction = InteractionAction.Invoke {
                    viewModel.setAccountSelected(account.user.userId, !account.isSelected)
                    true
                },
                secondaryAction = InteractionAction.Invoke {
                    if (account.isSelected) {
                        viewModel.setAccountSelected(account.user.userId, false)
                    }
                    viewModel.setAccountHidden(account.user.userId, !account.isHidden)
                    true
                },
            )
        )
        .background(backgroundColor, RoundedCornerShape(50))
        .border(1.dp, outlineColor, RoundedCornerShape(50))
        .padding(8.dp),
        horizontalArrangement = Dimens.horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Only care about actual notifications here, not silent unreads
        val renderedUnreadCounts = unreadCounts?.copy(
            unreadChats = 0,
            unreadMessages = 0,
        )
        SpaceUnreadCountBox(renderedUnreadCounts, 4.dp) {
            AvatarImage(
                source = account.user.avatarUrl?.let { MediaSource(it) },
                size = 24.dp,
                sessionId = account.user.userId,
                shape = Dimens.ownAccountAvatarShape,
                contentDescription = account.user.userId.value,
            )
        }
        val icon = if (account.isHidden && !account.isSelected)
            Icons.Default.VisibilityOff
        else when (account.syncState) {
            SyncState.Error -> Icons.Default.Error
            SyncState.Idle -> Icons.Default.CheckCircleOutline
            SyncState.Running -> Icons.Default.Sync
            SyncState.Terminated -> Icons.Default.Stop
            SyncState.Offline -> Icons.Default.CloudOff
        }
        AnimatedContent(icon) { icon ->
            val hint = when (account.syncState) {
                SyncState.Error -> stringResource(Res.string.hint_sync_state_error)
                SyncState.Idle -> stringResource(Res.string.hint_sync_state_idle)
                SyncState.Running -> stringResource(Res.string.hint_sync_state_running)
                SyncState.Terminated -> stringResource(Res.string.hint_sync_state_terminated)
                SyncState.Offline -> stringResource(Res.string.hint_sync_state_offline)
            }
            Icon(
                icon,
                hint,
                Modifier.size(16.dp),
                tint = if (account.syncState == SyncState.Error)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
