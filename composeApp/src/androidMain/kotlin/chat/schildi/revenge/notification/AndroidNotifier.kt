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
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import chat.schildi.revenge.Destination
import chat.schildi.revenge.MainActivity
import chat.schildi.revenge.RevengeAppGraph
import chat.schildi.revenge.RevengeApplication
import chat.schildi.revenge.compose.R
import chat.schildi.revenge.plaintext.NotificationEventTextFormat
import chat.schildi.revenge.serializedToString
import co.touchlab.kermit.Logger
import coil3.BitmapImage
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.size.Precision
import io.element.android.libraries.androidutils.hash.hash
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.api.notification.NotificationData
import io.element.android.libraries.matrix.api.room.CreateTimelineParams
import io.element.android.libraries.matrix.ui.media.MediaRequestData
import io.element.android.libraries.matrix.ui.media.animated.allowAnimatedImageDecoding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.getString
import shire.res.generated.resources.Res
import shire.res.generated.resources.notification_channel_app_notices
import shire.res.generated.resources.notification_channel_group_chat_messages
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTimedValue

object AndroidNotifier {

    private const val CHAT_MESSAGES_CHANNEL_ID_PREFIX = "session_"
    private const val CHAT_MESSAGES_CHANNEL_ID_GROUP = "chat_messages"
    private const val APP_NOTICES_CHANNEL_ID = "app_notices"
    private const val CONVERSATION_SHORTCUT_PREFIX = "room_"
    private const val MEDIA_TIMEOUT = 10_000L
    private const val LARGE_ICON_SIZE = 256

    private val log = Logger.withTag("AndroidNotifier")

    // Same as UiState, but we want the notification path to not rely on UiState to save resources
    val knownSessionIds: Flow<List<SessionId>> = RevengeAppGraph.sessionStore.sessionsFlow().map {
        it.map { SessionId(it.userId) }
    }.distinctUntilChanged()

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
            .setContentIntent(id.createPendingIntent(context, notificationId))
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

        val roomAvatar = data.roomAvatarUrl?.let { loadMediaBitmap(id.sessionId, MediaSource(it)) }
        val senderAvatar = data.senderAvatarUrl?.let { loadMediaBitmap(id.sessionId, MediaSource(it)) }

        // TODO look up
        val ownUser = Person.Builder()
            .setKey(id.sessionId.value)
            .setName(id.sessionId.value)
            .build()

        val senderName = data.getDisambiguatedDisplayName(data.senderId)
        val sender = Person.Builder()
            .setKey(data.senderId.value)
            .setName(senderName)
            .setIcon(senderAvatar?.let(IconCompat::createWithBitmap))
            .build()

        val message = NotificationEventTextFormat.notificationToText(data)

        val messagingStyle = existingNotification?.let {
            NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(it)
        } ?: NotificationCompat.MessagingStyle(ownUser)
        messagingStyle
            .setConversationTitle(data.roomDisplayName)
            .setGroupConversation(!data.isDirect)
            .addMessage(message, data.timestamp, sender)

        val shortcut = createConversationShortcut(
            sessionId = id.sessionId,
            roomId = id.roomId,
            label = data.roomDisplayName ?: senderName.takeIf { data.isDirect },
            icon = roomAvatar ?: senderAvatar?.takeIf { data.isDirect },
            person = sender.takeIf { data.isDirect },
            context = context,
        )
        shortcut?.let {
            runCatching {
                ShortcutManagerCompat.pushDynamicShortcut(context, it)
            }.onSuccess { published ->
                if (!published) {
                    log.w { "Conversation shortcut ${it.id} was rejected" }
                }
            }.onFailure { failure ->
                log.w("Failed to publish conversation shortcut ${it.id}", failure)
            }
        }

        val builder = existingNotification?.let {
            NotificationCompat.Builder(context, it)
        } ?: NotificationCompat.Builder(context, messageChannelId(id.sessionId))
            .setContentIntent(id.createPendingIntent(context, notificationId))

        val notification = builder
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(data.roomDisplayName)
            .setContentText(message)
            .setTicker(message)
            .setStyle(messagingStyle)
            .setNumber(messagingStyle.messages.size)
            .setLargeIcon(roomAvatar ?: senderAvatar)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .apply { shortcut?.let { setShortcutId(it.id) } }
            .build()

        try {
            notificationManager.notify(notificationId, notification)
        } catch (failure: SecurityException) {
            log.w("Notification permission was revoked before the notification could be posted", failure)
        }
    }

    private fun NotificationId.conversationDestination() = when (this) {
        is NotificationId.Event -> Destination.Conversation(
            sessionId,
            roomId,
            CreateTimelineParams.Focused(eventId),
        )
        is NotificationId.Room -> Destination.Conversation(sessionId, roomId)
        NotificationId.DebugMessage,
        is NotificationId.VerificationRequest -> null
    }

    private fun NotificationId.androidNotificationId(): Int = when (this) {
        is NotificationId.Event -> NotificationId.Room(sessionId, roomId).hashCode()
        else -> hashCode()
    }

    private fun NotificationId.createIntent(context: Context): Intent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        conversationDestination()?.let { destination ->
            intent.putExtra(MainActivity.EXTRA_DESTINATION, destination.serializedToString())
        }
        return intent
    }

    private fun NotificationId.createPendingIntent(context: Context, notificationId: Int): PendingIntent? {
        val intent = createIntent(context) ?: return null
        return PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createConversationShortcut(
        sessionId: SessionId,
        roomId: RoomId,
        label: String?,
        icon: Bitmap?,
        person: Person?,
        context: Context,
    ): ShortcutInfoCompat? {
        val intent = NotificationId.Room(sessionId, roomId).createIntent(context) ?: return null
        return ShortcutInfoCompat.Builder(context, conversationShortcutId(sessionId, roomId))
            .setShortLabel(label?.takeIf(String::isNotBlank) ?: roomId.value)
            .setIntent(intent)
            .setIsConversation()
            .apply {
                icon?.let { setIcon(IconCompat.createWithAdaptiveBitmap(it)) }
                person?.let { setPerson(person) }
            }
            .build()
    }

    private fun conversationShortcutSessionPrefix(sessionId: SessionId): String {
        return "$CONVERSATION_SHORTCUT_PREFIX${sessionId.value.hash().take(32)}_"
    }

    private fun conversationShortcutId(sessionId: SessionId, roomId: RoomId): String {
        return conversationShortcutSessionPrefix(sessionId) + roomId.value.hash().take(32)
    }

    private fun removeConversationShortcutsForUnknownSessions(context: Context, sessionIds: List<SessionId>) {
        val knownSessionPrefixes = sessionIds.map(::conversationShortcutSessionPrefix)
        val shortcutIds = runCatching {
            ShortcutManagerCompat.getShortcuts(
                context,
                ShortcutManagerCompat.FLAG_MATCH_DYNAMIC or ShortcutManagerCompat.FLAG_MATCH_CACHED,
            ).mapNotNull { shortcut ->
                shortcut.id.takeIf { id ->
                    id.startsWith(CONVERSATION_SHORTCUT_PREFIX) &&
                        knownSessionPrefixes.none { prefix -> id.startsWith(prefix) }
                }
            }.distinct()
        }.onFailure { failure ->
            log.w("Failed to fetch conversation shortcuts", failure)
        }.getOrNull() ?: return

        if (shortcutIds.isEmpty()) return
        runCatching {
            ShortcutManagerCompat.removeLongLivedShortcuts(context, shortcutIds)
        }.onSuccess {
            log.d { "Removed ${shortcutIds.size} conversation shortcuts for deleted sessions" }
        }.onFailure { failure ->
            log.w("Failed to remove conversation shortcuts for deleted sessions", failure)
        }
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
        knownSessionIds.onEach { sessionIds ->
            removeConversationShortcutsForUnknownSessions(context, sessionIds)
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
        return measureTimedValue {
            withTimeoutOrNull(MEDIA_TIMEOUT.milliseconds) {
                val imageLoaderHolder = RevengeAppGraph.imageLoaderHolder
                val imageLoader = imageLoaderHolder.getIfExists(sessionId) ?: run {
                    val client = RevengeAppGraph.sessionCache.getOrRestore(sessionId).getOrNull()
                        ?: return@withTimeoutOrNull null
                    imageLoaderHolder.get(client)
                }
                val requestData = MediaRequestData(source, MediaRequestData.Kind.Content)
                val context = RevengeApplication.instance
                val request = ImageRequest.Builder(context)
                    .data(requestData)
                    .size(LARGE_ICON_SIZE)
                    .precision(Precision.INEXACT)
                    .allowAnimatedImageDecoding(false)
                    .build()
                ((imageLoader.execute(request) as? SuccessResult)?.image as? BitmapImage)?.bitmap
            }
        }.let {
            log.d { "Bitmap load for $sessionId, ${source.safeUrl} took ${it.duration}, success=${it.value != null}" }
            it.value
        }
    }
}
