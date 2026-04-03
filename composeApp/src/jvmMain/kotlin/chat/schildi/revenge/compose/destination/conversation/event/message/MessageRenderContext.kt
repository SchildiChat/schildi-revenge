package chat.schildi.revenge.compose.destination.conversation.event.message

import androidx.compose.runtime.compositionLocalOf
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.core.ThreadId

enum class MessageRenderContext {
    NORMAL,
    IN_REPLY_TO,
}

data class ThreadReplyContext(
    val sessionId: SessionId,
    val roomId: RoomId,
    val threadId: ThreadId,
)

val LocalMessageRenderContext = compositionLocalOf { MessageRenderContext.NORMAL }
val LocalThreadReplyContext = compositionLocalOf<ThreadReplyContext?> { null }
