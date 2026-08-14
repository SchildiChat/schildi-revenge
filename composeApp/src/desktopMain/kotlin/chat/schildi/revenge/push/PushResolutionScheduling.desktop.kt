package chat.schildi.revenge.push

import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId

actual suspend fun schedulePushResolutionWork(sessionId: String, roomId: String) {
    // TODO support network-connectivity-based retries once we support push on desktop?
    PushNotificationHandler.resolvePendingPushes(SessionId(sessionId), RoomId(roomId))
}
