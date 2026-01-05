package chat.schildi.revenge.compose.destination.conversation.event

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.InteractionAction
import chat.schildi.revenge.actions.actionProvider
import chat.schildi.revenge.compose.components.AvatarImage
import chat.schildi.revenge.compose.components.UserTimestampItem
import chat.schildi.revenge.compose.components.UserTimestampList
import chat.schildi.revenge.compose.components.WithContextMenu
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.revenge.compose.focus.rememberFocusId
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.api.room.RoomMember
import io.element.android.libraries.matrix.api.timeline.item.event.Receipt
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap

@Composable
fun ColumnScope.ReadReceiptsRow(
    receipts: ImmutableList<Receipt>,
    roomMembersById: ImmutableMap<UserId, RoomMember>,
    modifier: Modifier = Modifier,
) {
    if (receipts.isEmpty()) return
    val focusId = rememberFocusId()
    WithContextMenu(
        focusId = focusId,
        popupContent = {
            val userTimestamps = remember(receipts, roomMembersById) {
                receipts.map { receipt ->
                    val member = roomMembersById[receipt.userId]
                    UserTimestampItem<Unit>(
                        userId = receipt.userId,
                        displayName = member?.displayName,
                        avatarUrl = member?.avatarUrl,
                        timestamp = receipt.timestamp,
                        extra = null,
                    )
                }
            }
            UserTimestampList(userTimestamps)
        },
        modifier = modifier,
    ) {
        FlowRow(
            modifier = Modifier
                .keyFocusable(
                    role = FocusRole.NESTED_AUX_ITEM,
                    id = focusId,
                    actionProvider = actionProvider(
                        primaryAction = InteractionAction.ContextMenu(focusId, null),
                        secondaryAction = InteractionAction.ContextMenu(focusId, null),
                    ),
                )
                .padding(
                    top = Dimens.Conversation.receiptPaddingVertical,
                    start = 0.dp,
                    end = Dimens.Conversation.otherSidePadding,
                )
                .align(Alignment.Start),
            verticalArrangement = Arrangement.spacedBy(Dimens.Conversation.receiptPaddingVertical),
            horizontalArrangement = Arrangement.spacedBy(
                Dimens.Conversation.receiptPaddingHorizontal,
                Alignment.Start,
            ),
        ) {
            receipts.forEachIndexed { index, receipt ->
                ReadReceiptItem(
                    receipt = receipt,
                    member = roomMembersById[receipt.userId],
                    modifier = Modifier.zIndex(-index.toFloat())
                )
            }
        }
    }
}

@Composable
fun ReadReceiptItem(
    receipt: Receipt,
    member: RoomMember?,
    modifier: Modifier = Modifier,
) {
    val senderName = member?.displayName ?: receipt.userId.value
    AvatarImage(
        source = member?.avatarUrl?.let { MediaSource(it) },
        size = Dimens.Conversation.receiptSize,
        contentDescription = senderName,
        displayName = senderName,
        modifier = modifier,
    )
}
