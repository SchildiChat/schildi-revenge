package chat.schildi.revenge.compose.destination.conversation.event

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.compose.components.AvatarImage
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
    FlowRow(
        modifier = modifier
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
        receipts.forEach { receipt ->
            ReadReceiptItem(
                receipt = receipt,
                member = roomMembersById[receipt.userId],
            )
        }
    }
}

@Composable
fun ReadReceiptItem(
    receipt: Receipt,
    member: RoomMember?,
    modifier: Modifier = Modifier,
) {
    // TODO really need to work on display-name based avatar fallbacks
    AvatarImage(
        source = member?.avatarUrl?.let { MediaSource(it) },
        size = Dimens.Conversation.receiptSize,
        contentDescription = member?.displayName ?: receipt.userId.value,
        modifier = modifier,
    )
}
