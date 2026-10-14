package chat.schildi.revenge.model

import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId

data class ScopedRoomId(
    val sessionId: SessionId,
    val roomId: RoomId,
) {
    fun toRaw() = ScopedRawRoomId(
        sessionId = sessionId.value,
        roomId = roomId.value,
    )
}

typealias ScopedRoomKey = ScopedRoomId

data class ScopedRawRoomId(
    val sessionId: String,
    val roomId: String,
) {
    fun toWrapped() = ScopedRoomId(
        sessionId = SessionId(sessionId),
        roomId = RoomId(roomId),
    )
}
