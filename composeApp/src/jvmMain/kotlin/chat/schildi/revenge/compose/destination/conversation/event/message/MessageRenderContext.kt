package chat.schildi.revenge.compose.destination.conversation.event.message

import androidx.compose.runtime.compositionLocalOf

enum class MessageRenderContext {
    NORMAL,
    IN_REPLY_TO,
}

val LocalMessageRenderContext = compositionLocalOf { MessageRenderContext.NORMAL }
