package chat.schildi.revenge.notification

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import chat.schildi.revenge.UiState
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.media.MediaSource
import io.github.kdroidfilter.knotify.builder.AppConfig
import io.github.kdroidfilter.knotify.builder.ExperimentalNotificationsApi
import io.github.kdroidfilter.knotify.builder.NotificationInitializer
import io.github.kdroidfilter.knotify.compose.builder.sendComposeNotification
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.getString
import org.jetbrains.skia.Image
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.app_title_short

sealed interface NotificationId {
    val sessionId: SessionId?
    data class Event(
        override val sessionId: SessionId,
        val roomId: RoomId,
        val eventId: EventId,
    ) : NotificationId
    data class Room(
        override val sessionId: SessionId,
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

    @OptIn(ExperimentalNotificationsApi::class)
    suspend fun notify(
        id: NotificationId,
        title: String,
        message: String,
        largeImage: MediaSource? = null,
    ) {
        val image = largeImage?.let {
            val client = id.sessionId?.let { UiState.currentClientFor(it) } ?: return@let null
            withTimeoutOrNull(3000) {
                client.matrixMediaLoader.loadMediaContent(largeImage).getOrNull()?.let {
                    Image.makeFromEncoded(it).toComposeImageBitmap()
                }
            }
        }
        sendComposeNotification(
            title = title,
            message = message,
            largeIcon = image?.let {{
                Image(
                    bitmap = image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }}
        )
    }
}
