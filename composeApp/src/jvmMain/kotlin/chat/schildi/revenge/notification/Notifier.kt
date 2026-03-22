package chat.schildi.revenge.notification

import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.github.kdroidfilter.knotify.builder.AppConfig
import io.github.kdroidfilter.knotify.builder.ExperimentalNotificationsApi
import io.github.kdroidfilter.knotify.builder.Notification
import io.github.kdroidfilter.knotify.builder.NotificationInitializer
import io.github.kdroidfilter.knotify.builder.sendNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import org.jetbrains.compose.resources.getString
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.app_title_short

sealed interface NotificationId {
    data class Event(
        val sessionId: SessionId,
        val roomId: RoomId,
        val eventId: EventId,
    ) : NotificationId
    data class Room(
        val sessionId: SessionId,
        val roomId: RoomId,
    ) : NotificationId
}

object Notifier {

    suspend fun initialize() {
        NotificationInitializer.configure(
            AppConfig(
                appName = getString(Res.string.app_title_short),
                smallIcon = Res.getUri("drawable-xhdpi/ic_launcher.png")
            )
        )
    }

    val activeNotifications = MutableStateFlow<Map<NotificationId, Notification>>(emptyMap())

    // TODO cancel notifications, render images and stuff?
    @OptIn(ExperimentalNotificationsApi::class)
    suspend fun notify(
        id: NotificationId,
        title: String,
        message: String,
        largeImage: String? = null,
    ) {
        // TODO onActivated?
        val previous = activeNotifications.getAndUpdate { it - id }[id]
        previous?.hide()
        val notification = sendNotification(
            title = title,
            message = message,
            largeImage = largeImage,
        )
        activeNotifications.update {
            it.toMutableMap().apply {
                this[id] = notification
            }
        }
    }
}
