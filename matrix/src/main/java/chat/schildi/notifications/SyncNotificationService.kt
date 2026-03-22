package chat.schildi.notifications

import io.element.android.libraries.matrix.api.core.SessionId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.matrix.rustcomponents.sdk.Client
import org.matrix.rustcomponents.sdk.NotificationItem
import org.matrix.rustcomponents.sdk.SyncNotificationListener
import timber.log.Timber

class SyncNotificationService internal constructor(
    sessionId: SessionId,
    client: Client,
    sessionCoroutineScope: CoroutineScope,
) {
    private val logger = Timber.tag("SyncNotifications")
    private val mapper = SyncNotificationMapper(sessionId)
    private val _notifications =
        MutableSharedFlow<SyncNotification>(
            extraBufferCapacity = 64,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    val notifications: SharedFlow<SyncNotification> = _notifications.asSharedFlow()

    init {
        sessionCoroutineScope.launch {
            runCatching {
                client.registerNotificationHandler(
                    object : SyncNotificationListener {
                        override fun onNotification(
                            notification: NotificationItem,
                            roomId: String,
                        ) {
                            mapper.map(notification, roomId)
                                .onSuccess {
                                    if (!_notifications.tryEmit(it)) {
                                        logger.w(
                                            "Dropping sync notification for roomId=$roomId because the buffer is full",
                                        )
                                    }
                                }
                                .onFailure {
                                    logger.e(it, "Failed to map sync notification for roomId=$roomId")
                                }
                        }
                    },
                )
            }.onFailure {
                logger.e(it, "Failed to register sync notification handler")
            }
        }
    }
}
