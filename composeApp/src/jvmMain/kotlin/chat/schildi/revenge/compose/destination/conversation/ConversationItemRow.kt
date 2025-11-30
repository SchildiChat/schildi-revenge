package chat.schildi.revenge.compose.destination.conversation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import chat.schildi.revenge.compose.destination.conversation.event.EventContentLayout
import chat.schildi.revenge.compose.destination.conversation.virtual.NewMessagesLine
import chat.schildi.revenge.compose.destination.conversation.virtual.PagingIndicator
import io.element.android.libraries.matrix.api.timeline.MatrixTimelineItem
import io.element.android.libraries.matrix.api.timeline.item.virtual.VirtualTimelineItem

@Composable
fun ConversationItemRow(item: MatrixTimelineItem, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        when (item) {
            is MatrixTimelineItem.Virtual -> {
                // TODO
                when (val virtualItem = item.virtual) {
                    is VirtualTimelineItem.DayDivider -> Text(virtualItem.timestamp.toString())
                    VirtualTimelineItem.LastForwardIndicator -> Text("FWD")
                    is VirtualTimelineItem.LoadingIndicator -> PagingIndicator()
                    VirtualTimelineItem.ReadMarker -> NewMessagesLine()
                    VirtualTimelineItem.RoomBeginning -> Text("BEGINNING")
                    VirtualTimelineItem.TypingNotification -> {
                        // TODO?
                    }
                }
            }

            is MatrixTimelineItem.Event -> {
                EventContentLayout(item.event)
            }

            MatrixTimelineItem.Other -> {
                // TODO what is this?
                Text("OTHER???")
            }
        }
    }
}
