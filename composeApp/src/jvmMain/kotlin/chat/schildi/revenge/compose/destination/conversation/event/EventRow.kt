package chat.schildi.revenge.compose.destination.conversation.event

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.compose.destination.conversation.event.reaction.ReactionsRow
import chat.schildi.revenge.compose.focus.keyFocusable
import io.element.android.libraries.matrix.api.timeline.item.event.EventTimelineItem

@Composable
fun EventRow(
    event: EventTimelineItem,
    isSameAsPreviousSender: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .keyFocusable(FocusRole.SEARCHABLE_ITEM)
            .padding(horizontal = Dimens.windowPadding)
    ) {
        EventContentLayout(
            content = event.content,
            senderId = event.sender,
            senderProfile = event.senderProfile,
            isOwn = event.isOwn,
            isSameAsPreviousSender = isSameAsPreviousSender,
            inReplyTo = event.inReplyTo(),
        )
        ReactionsRow(
            reactions = event.reactions,
            messageIsOwn = event.isOwn,
        )
    }
}
