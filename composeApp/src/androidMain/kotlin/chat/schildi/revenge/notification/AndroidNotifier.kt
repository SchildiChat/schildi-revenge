package chat.schildi.revenge.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import chat.schildi.revenge.MainActivity
import chat.schildi.revenge.RevengeAppGraph
import chat.schildi.revenge.RevengeApplication
import chat.schildi.revenge.UiState
import chat.schildi.revenge.compose.R
import chat.schildi.revenge.plaintext.NotificationEventTextFormat
import chat.schildi.revenge.serializedToString
import co.touchlab.kermit.Logger
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.api.notification.NotificationData
import io.element.android.libraries.matrix.api.room.CreateTimelineParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.getString
import shire.res.generated.resources.Res
import shire.res.generated.resources.notification_channel_app_notices
import shire.res.generated.resources.notification_channel_group_chat_messages
import kotlin.time.Duration.Companion.milliseconds

object AndroidNotifier {

    private const val CHAT_MESSAGES_CHANNEL_ID_PREFIX = "session_"
    private const val CHAT_MESSAGES_CHANNEL_ID_GROUP = "chat_messages"
    private const val APP_NOTICES_CHANNEL_ID = "app_notices"
    private const val MEDIA_TIMEOUT = 3_000L

    private val log = Logger.withTag("AndroidNotifier")

    fun messageChannelId(sessionId: SessionId) = CHAT_MESSAGES_CHANNEL_ID_PREFIX + sessionId.value

    suspend fun notify(
        id: NotificationId,
        title: String,
        message: String,
        largeImage: MediaSource?,
        context: Context = RevengeApplication.instance,
    ) {
        val notificationManager = NotificationManagerCompat.from(context)
        if (!context.canPostNotifications() || !notificationManager.areNotificationsEnabled()) return

        val notificationId = id.androidNotificationId()
        val largeIcon = largeImage?.let { loadMediaBitmap(id.sessionId, it) }

        val channelId = when (id) {
            is NotificationId.VerificationRequest -> {
                // While we don't have push for these, and don't auto-dismiss either,
                // no huge benefit in showing these
                return
            }
            is NotificationId.Event -> messageChannelId(id.sessionId)
            // is NotificationId.Room -> // What is this for without event ID?
            else -> APP_NOTICES_CHANNEL_ID
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentIntent(id.createContentIntent(context, notificationId))
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(largeIcon)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()

        try {
            notificationManager.notify(notificationId, notification)
        } catch (failure: SecurityException) {
            log.w("Notification permission was revoked before the notification could be posted", failure)
        }
    }

    suspend fun notifyMessage(
        id: NotificationId.Event,
        data: NotificationData,
        context: Context = RevengeApplication.instance,
    ) {
        val notificationManager = NotificationManagerCompat.from(context)
        if (!context.canPostNotifications() || !notificationManager.areNotificationsEnabled()) return

        val notificationId = id.androidNotificationId()

        val existingNotification = runCatching {
            notificationManager.activeNotifications
                .firstOrNull { it.id == notificationId }
                ?.notification
        }.onFailure {
            log.w("Failed to inspect active notifications", it)
        }.getOrNull()

        val roomAvatar = (data.roomAvatarUrl ?: data.senderAvatarUrl?.takeIf { data.isDirect })?.let {
            loadMediaBitmap(id.sessionId, MediaSource(it))
        }

        // TODO look up
        val ownUser = Person.Builder()
            .setKey(id.sessionId.value)
            .setName(id.sessionId.value)
            .build()

        val sender = Person.Builder()
            .setName(data.getDisambiguatedDisplayName(data.senderId))
            .apply {
                data.senderAvatarUrl?.let {
                    loadMediaBitmap(data.sessionId, MediaSource(it))?.let {
                        setIcon(IconCompat.createWithBitmap(it))
                    }
                }
            }
            .build()

        val message = NotificationEventTextFormat.notificationToText(data)

        val messagingStyle = existingNotification?.let {
            NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(it)
        } ?: NotificationCompat.MessagingStyle(ownUser)
        messagingStyle.addMessage(message, data.timestamp, sender)

        val builder = existingNotification?.let {
            NotificationCompat.Builder(context, it)
        } ?: NotificationCompat.Builder(context, messageChannelId(id.sessionId))
            .setContentIntent(id.createContentIntent(context, notificationId))

        // TODO chat shortcuts

        val notification = builder
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(data.roomDisplayName)
            .setContentText(message)
            .setStyle(messagingStyle)
            .setNumber(messagingStyle.messages.size)
            .setLargeIcon(roomAvatar)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()

        try {
            notificationManager.notify(notificationId, notification)
        } catch (failure: SecurityException) {
            log.w("Notification permission was revoked before the notification could be posted", failure)
        }
    }

    private fun NotificationId.conversationDestination() = when (this) {
        is NotificationId.Event -> UiState.getConversationDestinationFromInbox(
            sessionId,
            roomId,
            CreateTimelineParams.Focused(eventId),
        )
        is NotificationId.Room -> UiState.getConversationDestinationFromInbox(sessionId, roomId)
        NotificationId.DebugMessage,
        is NotificationId.VerificationRequest -> null
    }

    private fun NotificationId.androidNotificationId(): Int = when (this) {
        is NotificationId.Event -> NotificationId.Room(sessionId, roomId).hashCode()
        else -> hashCode()
    }

    private fun NotificationId.createContentIntent(context: Context, notificationId: Int): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        conversationDestination()?.let { destination ->
            intent.putExtra(MainActivity.EXTRA_DESTINATION, destination.serializedToString())
        }
        return PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createSessionNotificationChannels(
        context: Context = RevengeApplication.instance,
        sessionIds: List<SessionId>,
    ) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        log.d { "Create ${sessionIds.size} session notification channels" }
        notificationManager.createNotificationChannels(
            sessionIds.map { sessionId ->
                NotificationChannel(
                    messageChannelId(sessionId),
                    sessionId.value,
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    enableVibration(true)
                    enableLights(true)
                    group = CHAT_MESSAGES_CHANNEL_ID_GROUP
                }
            }
        )
    }

     suspend fun launchCreateNotificationChannelsLoop(
         scope: CoroutineScope,
         context: Context = RevengeApplication.instance,
    ) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannels(
            listOf(
                NotificationChannel(
                    APP_NOTICES_CHANNEL_ID,
                    getString(Res.string.notification_channel_app_notices),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        )
         notificationManager.createNotificationChannelGroup(
             NotificationChannelGroup(
                 CHAT_MESSAGES_CHANNEL_ID_GROUP,
                 getString(Res.string.notification_channel_group_chat_messages),
             )
         )
         UiState.knownSessionIds.onEach { sessionIds ->
             val expectSessionChannelIds = sessionIds.map(::messageChannelId).toSet()
             // Clean up old channels
             notificationManager.notificationChannels.forEach { channel ->
                 if (channel.id != APP_NOTICES_CHANNEL_ID && channel.id !in expectSessionChannelIds) {
                     log.d { "Delete old notification channel ${channel.id}" }
                     notificationManager.deleteNotificationChannel(channel.id)
                 }
             }
             // Create/update channels for current sessions
             createSessionNotificationChannels(context, sessionIds)
         }.launchIn(scope)
    }

    private fun Context.canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

    private suspend fun loadMediaBitmap(sessionId: SessionId?, source: MediaSource): Bitmap? {
        sessionId ?: return null
        return withTimeoutOrNull(MEDIA_TIMEOUT.milliseconds) {
            // TODO hook up our media cache?
            val client = RevengeAppGraph.sessionCache.getOrRestore(sessionId).getOrNull()
                ?: return@withTimeoutOrNull null
            client.matrixMediaLoader
                .loadMediaThumbnail(source, 1024, 1024)
                .getOrNull()
                ?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
        }
    }
}
