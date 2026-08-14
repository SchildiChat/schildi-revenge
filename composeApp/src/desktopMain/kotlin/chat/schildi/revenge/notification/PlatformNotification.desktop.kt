package chat.schildi.revenge.notification

import chat.schildi.revenge.plaintext.NotificationEventTextFormat
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.api.notification.NotificationData

actual suspend fun platformNotify(
    id: NotificationId,
    title: String,
    message: String,
    largeImage: MediaSource?
) = DesktopNotifier.notify(
    id = id,
    title = title,
    message = message,
    largeImage = largeImage,
)

actual suspend fun platformNotifyMessage(
    id: NotificationId.Event,
    data: NotificationData,
) {
    val formatted = NotificationEventTextFormat.notificationToText(data)
    platformNotify(
        id = id,
        title = data.roomDisplayName ?: data.getDisambiguatedDisplayName(data.senderId),
        message = formatted,
        largeImage = data.roomAvatarUrl?.let { MediaSource(it) }
            ?: data.senderAvatarUrl?.let { MediaSource(it) },
    )
}
