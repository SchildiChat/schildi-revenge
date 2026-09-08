package chat.schildi.revenge.notification

import chat.schildi.revenge.model.ScopedRawRoomId
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.api.notification.NotificationData

expect suspend fun platformNotify(
    id: NotificationId,
    title: String,
    message: String,
    largeImage: MediaSource? = null,
)

expect suspend fun platformNotifyMessage(
    id: NotificationId.Event,
    data: NotificationData,
)

/**
 * Get a list of rooms with current active notifications.
 */
expect fun platformActiveNotificationRooms(): List<ScopedRawRoomId>

/**
 * Heuristically cancel notifications based on the latest observed read receipt / marker, if it matches the
 * latest shown notification.
 */
expect fun platformAutoDismissNotification(sessionId: SessionId, roomId: RoomId, latestRead: List<EventId>): Boolean
