package chat.schildi.revenge.notification

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
