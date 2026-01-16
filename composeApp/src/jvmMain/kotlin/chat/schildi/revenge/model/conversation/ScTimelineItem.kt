package chat.schildi.revenge.model.conversation

import com.beeper.android.messageformat.MatrixBodyParseResult
import com.beeper.android.messageformat.MatrixBodyPreFormatStyle
import com.beeper.android.messageformat.MatrixHtmlParser
import io.element.android.libraries.matrix.api.timeline.MatrixTimelineItem
import io.element.android.libraries.matrix.api.timeline.item.event.EventContent
import io.element.android.libraries.matrix.api.timeline.item.event.MessageContent
import io.element.android.libraries.matrix.api.timeline.item.event.MessageFormat
import io.element.android.libraries.matrix.api.timeline.item.event.MessageTypeWithAttachment
import io.element.android.libraries.matrix.api.timeline.item.event.TextLikeMessageType

data class MessageMetadata(
    val preFormattedContent: MatrixBodyParseResult? = null,
)

data class ScTimelineItem(
    val item: MatrixTimelineItem,
    val messageMetadata: MessageMetadata? = null,
)

/**
 * Potentially expensive message parsing included!
 * Avoid in UI thread if possible (but exceptions are probably ok).
 */
fun MatrixTimelineItem.toScTimelineItem(
    parser: MatrixHtmlParser,
    style: MatrixBodyPreFormatStyle,
): ScTimelineItem {
    return when (this) {
        is MatrixTimelineItem.Event -> {
            ScTimelineItem(this, event.content.messageMetadata(parser, style))
        }
        is MatrixTimelineItem.Virtual,
        MatrixTimelineItem.Other -> ScTimelineItem(this)
    }
}

/**
 * Potentially expensive message parsing included!
 * Avoid in UI thread if possible (but exceptions are probably ok).
 */
fun EventContent.messageMetadata(
    parser: MatrixHtmlParser,
    style: MatrixBodyPreFormatStyle,
): MessageMetadata {
    val (formattedBody, plaintext) = when (val type = (this as? MessageContent)?.type) {
        is TextLikeMessageType -> Pair(type.formatted, type.body)
        is MessageTypeWithAttachment -> Pair(type.formattedCaption, type.caption)
        else -> Pair(null, null)
    }
    // TODO pass through intentional mentions, and don't render @room if unintentional
    val allowRoomMention = true
    val preFormattedContent = formattedBody?.takeIf { it.format == MessageFormat.HTML }?.let {
        parser.parseHtml(it.body, style, allowRoomMention)
    } ?: plaintext?.let {
        parser.parsePlaintext(it, style, allowRoomMention)
    }
    return MessageMetadata(preFormattedContent)
}
