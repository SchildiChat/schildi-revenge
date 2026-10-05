package chat.schildi.revenge.model

import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId

expect fun platformOnMessageSent(sessionId: SessionId, roomId: RoomId)
