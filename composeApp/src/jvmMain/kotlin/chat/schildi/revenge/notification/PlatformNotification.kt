package chat.schildi.revenge.notification

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
