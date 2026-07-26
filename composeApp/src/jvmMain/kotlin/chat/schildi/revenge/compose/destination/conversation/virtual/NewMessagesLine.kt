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
import chat.schildi.lib.preferences.ScPrefs
import chat.schildi.revenge.preferences.value
import chat.schildi.revenge.Dimens
import chat.schildi.theme.scExposures

@Composable
fun NewMessagesLine(
    isReal: Boolean = true,
    isHint: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val debugUnreadLine = ScPrefs.SHOW_DEV_INFOS.value()
    if (!debugUnreadLine && isHint) {
        return
    }
    ConversationDividerLine(
        if (isReal || !debugUnreadLine) // fully read event ID matches where the SDK inserted it
            MaterialTheme.scExposures.accentColor
        else if (isHint) // this is where the fully read event ID was last time we checked, but Rust SDK didn't insert an unread line here
            MaterialTheme.scExposures.linkColor
        else // Rust SDK inserted it here but it may not be accurate
            MaterialTheme.scExposures.accentColor.copy(alpha = 0.3f),
        modifier
            .padding(
                vertical = Dimens.Conversation.unreadLinePadding,
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
