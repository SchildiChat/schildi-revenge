package chat.schildi.revenge.compose.destination.conversation.virtual

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import chat.schildi.revenge.Dimens
import chat.schildi.theme.scExposures

@Composable
fun NewMessagesLine(modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth()
            .padding(
                horizontal = Dimens.listPadding,
                vertical = Dimens.Conversation.virtualItemPadding,
            )
            .background(
                color = MaterialTheme.scExposures.colorOnAccent
            )
    )
}
