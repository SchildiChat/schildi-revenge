package chat.schildi.revenge.compose.destination.conversation.virtual

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import chat.schildi.revenge.Dimens
import chat.schildi.theme.scExposures

@Composable
fun NewMessagesLine(modifier: Modifier = Modifier) {
    ConversationDividerLine(
        MaterialTheme.scExposures.accentColor,
        modifier
            .padding(
                vertical = Dimens.Conversation.virtualItemPadding,
                horizontal = Dimens.windowPadding,
            )
    )
}

@Composable
fun ConversationDividerLine(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(Dimens.Conversation.newMessagesLineHeight)
            .background(
                color = color,
                shape = RoundedCornerShape(Dimens.Conversation.newMessagesLineHeight)
            )
    )
}
