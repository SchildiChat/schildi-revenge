package chat.schildi.revenge.compose.destination.conversation.event.message

import androidx.compose.runtime.compositionLocalOf

enum class MessageRenderContext {
    NORMAL,
    IN_REPLY_TO,
    THREADED_IN_REPLY_TO,
}

fun MessageRenderContext.isReply() =
    this == MessageRenderContext.IN_REPLY_TO || this == MessageRenderContext.THREADED_IN_REPLY_TO

val LocalMessageRenderContext = compositionLocalOf { MessageRenderContext.NORMAL }
