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

enum class NewMessageLineInstance {
    ReadMarker,
    Sdk,
}

@Composable
fun NewMessagesLine(
    instance: NewMessageLineInstance,
    isThreadedTimeline: Boolean,
    modifier: Modifier = Modifier,
) {
    val debugUnreadLine = ScPrefs.SHOW_DEV_INFOS.value()
    if (!debugUnreadLine) {
        val shouldRender = when (instance) {
            // Fully read event says this is unread, this is to be trusted as truth in non-threaded timelines.
            NewMessageLineInstance.ReadMarker -> !isThreadedTimeline
            // SDK says this is unread, but doesn't match what the m.fully_read marker says.
            // Only trust in threaded timelines.
            NewMessageLineInstance.Sdk -> isThreadedTimeline
        }
        if (!shouldRender) {
            return
        }
    }
    val color = if (debugUnreadLine) {
        when (instance) {
            NewMessageLineInstance.ReadMarker -> if (isThreadedTimeline) {
                MaterialTheme.scExposures.accentColor.copy(alpha = 0.3f)
            } else {
                MaterialTheme.scExposures.accentColor
            }
            NewMessageLineInstance.Sdk -> if (isThreadedTimeline) {
                MaterialTheme.scExposures.accentColor
            } else {
                MaterialTheme.scExposures.accentColor.copy(alpha = 0.3f)
            }
        }
    } else {
        MaterialTheme.scExposures.accentColor
    }
    ConversationDividerLine(
        color,
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
