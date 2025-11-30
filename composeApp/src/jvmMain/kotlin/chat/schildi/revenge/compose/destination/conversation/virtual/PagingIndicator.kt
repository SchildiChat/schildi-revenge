package chat.schildi.revenge.compose.destination.conversation.virtual

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun PagingIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth(),
        Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
