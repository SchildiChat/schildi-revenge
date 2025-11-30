package chat.schildi.revenge.compose.destination.conversation.virtual

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import chat.schildi.revenge.Dimens

@Composable
fun PagingIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().padding(vertical = Dimens.Conversation.virtualItemPadding),
        Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
