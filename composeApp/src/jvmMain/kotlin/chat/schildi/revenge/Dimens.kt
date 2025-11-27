package chat.schildi.revenge

import androidx.compose.foundation.layout.Arrangement
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
}
