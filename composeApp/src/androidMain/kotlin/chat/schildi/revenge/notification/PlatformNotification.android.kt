package chat.schildi.revenge.notification

import chat.schildi.revenge.actions.AppMessage
import chat.schildi.revenge.model.ScopedRawRoomId
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.api.notification.NotificationData

actual suspend fun platformNotify(
    id: NotificationId,
    title: String,
    message: String,
    largeImage: MediaSource?
) = AndroidNotifier.notify(
    id = id,
    title = title,
    message = message,
    largeImage = largeImage,
)

actual suspend fun platformNotifyMessage(
    id: NotificationId.Event,
    data: NotificationData,
) = AndroidNotifier.notifyMessage(
    id = id,
    data = data,
)

actual suspend fun platformNotifyAppMessage(
    id: NotificationId.AppMessage,
    data: AppMessage,
    transient: Boolean,
) = AndroidNotifier.notifyAppMessage(
    id = id,
    data = data,
    transient = transient,
)

actual fun platformActiveNotificationRooms(): List<ScopedRawRoomId> =
    AndroidNotifier.activeNotificationRooms()

actual fun platformAutoDismissNotification(
    sessionId: SessionId,
    roomId: RoomId,
    latestRead: List<EventId>,
) = AndroidNotifier.maybeAutoDismiss(
    sessionId = sessionId.value,
    roomId = roomId.value,
    latestRead = latestRead.map(EventId::value),
)

actual fun platformRedactNotificationMessage(
    sessionId: SessionId,
    roomId: RoomId,
    eventId: EventId,
) = AndroidNotifier.redactMessage(
    sessionId = sessionId.value,
    roomId = roomId.value,
    eventId = eventId.value,
)
