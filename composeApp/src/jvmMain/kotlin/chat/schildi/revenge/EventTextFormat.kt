package chat.schildi.revenge

import io.element.android.libraries.matrix.api.timeline.item.event.AudioMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.EmoteMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.EventContent
import io.element.android.libraries.matrix.api.timeline.item.event.FileMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.ImageMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.LocationMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.MessageContent
import io.element.android.libraries.matrix.api.timeline.item.event.NoticeMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.OtherMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.StickerContent
import io.element.android.libraries.matrix.api.timeline.item.event.StickerMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.TextMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.VideoMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.VoiceMessageType

object EventTextFormat {
    fun eventToText(content: EventContent): String? {
        return when (content) {
            is MessageContent -> {
                when (val type = content.type) {
                    is EmoteMessageType -> type.body
                    is LocationMessageType -> type.body
                    is AudioMessageType -> type.bestDescription
                    is FileMessageType -> type.bestDescription
                    is ImageMessageType -> type.bestDescription
                    is StickerMessageType -> type.bestDescription
                    is VideoMessageType -> type.bestDescription
                    is VoiceMessageType -> type.bestDescription
                    is OtherMessageType -> type.body
                    is NoticeMessageType -> type.body
                    is TextMessageType -> type.body
                }
            }
            is StickerContent -> content.bestDescription
            else -> null
        }
    }
}
