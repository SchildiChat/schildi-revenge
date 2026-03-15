package chat.schildi.revenge.compose.destination.conversation.userlist

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import chat.schildi.revenge.model.userlist.ReadReceiptListItem
import chat.schildi.revenge.model.userlist.MessageReadReceiptListViewModel

@Composable
fun ReadReceiptListRow(
    receiptItem: ReadReceiptListItem,
    viewModel: MessageReadReceiptListViewModel,
    modifier: Modifier = Modifier,
) {
    UserListRow(
        item = receiptItem,
        viewModel = viewModel,
        modifier = modifier,
    ) {
        UserListTimestamp(receiptItem.receipt.timestamp)
    }
}
