package chat.schildi.revenge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object Dimens {
    val windowPadding = 16.dp
    val listPadding = 8.dp
    val horizontalItemPadding = 8.dp
    val horizontalItemPaddingBig = 16.dp
    val horizontalArrangement = Arrangement.spacedBy(horizontalItemPadding)
    val verticalArrangement = Arrangement.spacedBy(listPadding)

    object Inbox {
        val avatar = 48.dp
        val smallIcon = 16.dp
        val maxWidth = 1024.dp
    }

    object Conversation {
        val avatar = 48.dp
        val avatarItemPadding = 16.dp
        val virtualItemPadding = 8.dp
        val otherSidePadding = 80.dp
        val messageBubbleCornerRadius = 10.dp
        val messageBubbleShape = RoundedCornerShape(messageBubbleCornerRadius)
        val replyContentShape = RoundedCornerShape(6.dp)
        val replyItemPadding = 8.dp
        val messageBubbleInnerPadding = 8.dp
        val imageBubbleInnerPadding = 2.dp
        val messageSameSenderPadding = 4.dp
        val messageOtherSenderPadding = 8.dp
        val captionPadding = 8.dp

        val imageMinWidth = 48.dp
        val imageMinHeight = 48.dp
        val imageMaxWidth = 500.dp
        val imageMaxHeight = 500.dp
        val imageRepliedToMaxHeight = 200.dp
    }
}
