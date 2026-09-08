package chat.schildi.revenge.model

import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId

data class ScopedRoomId(
    val sessionId: SessionId,
    val roomId: RoomId,
)

typealias ScopedRoomKey = ScopedRoomId

data class ScopedRawRoomId(
    val sessionId: String,
    val roomId: String,
)
