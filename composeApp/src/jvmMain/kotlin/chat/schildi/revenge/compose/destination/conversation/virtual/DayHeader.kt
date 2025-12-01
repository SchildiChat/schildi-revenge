package chat.schildi.revenge.compose.destination.conversation.virtual

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import chat.schildi.revenge.DateTimeFormat
import chat.schildi.revenge.Dimens
import io.element.android.libraries.matrix.api.timeline.item.virtual.VirtualTimelineItem

@Composable
fun DayHeader(item: VirtualTimelineItem.DayDivider, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth()
            .padding(
                vertical = Dimens.Conversation.virtualItemPadding,
                horizontal = Dimens.windowPadding,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(DateTimeFormat.formatTimestampAsDate(item.timestamp))
    }
}
