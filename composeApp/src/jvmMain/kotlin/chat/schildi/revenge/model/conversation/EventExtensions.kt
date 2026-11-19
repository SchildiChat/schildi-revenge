package chat.schildi.revenge.model.conversation

import io.element.android.libraries.matrix.api.timeline.item.event.EventContent
import io.element.android.libraries.matrix.api.timeline.item.event.MessageContent
import io.element.android.libraries.matrix.api.timeline.item.event.PollContent
import io.element.android.libraries.matrix.api.timeline.item.event.StickerContent

fun EventContent.matchesSearch(query: String) = when (this) {
    is MessageContent -> body.contains(query, ignoreCase = true)
    is StickerContent -> bestDescription.contains(query, ignoreCase = true)
    is PollContent -> question.contains(query, ignoreCase = true)
    else -> false
}
