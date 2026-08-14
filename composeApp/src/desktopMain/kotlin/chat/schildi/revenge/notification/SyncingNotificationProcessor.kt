package chat.schildi.revenge.notification

import chat.schildi.notifications.SyncNotification
import chat.schildi.revenge.preferences.RevengePrefs
import chat.schildi.lib.preferences.ScPrefs
import chat.schildi.revenge.ScCoroutines
import chat.schildi.revenge.UiState
import chat.schildi.revenge.plaintext.NotificationEventTextFormat
import chat.schildi.revenge.util.tryOrNull
import co.touchlab.kermit.Logger
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.api.notification.NotificationData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

object SyncingNotificationProcessor {
    private val log = Logger.withTag("NotificationProcessor")
    private val scope = ScCoroutines.scope(Dispatchers.IO, "NotificationProcessor")
    private val launchTimestamp = System.currentTimeMillis()

    @OptIn(ExperimentalCoroutinesApi::class)
    val notifications = combine(
        UiState.combinedSessions,
        UiState.mutedAccounts,
        RevengePrefs.settingFlow(ScPrefs.DESKTOP_NOTIFICATIONS),
    ) { sessions, muted, allowNotifications ->
        if (allowNotifications && muted != null) {
            val filteredSessions = sessions.filter { it.client.sessionId !in muted }
            log.d { "Initializing notification listener for [${filteredSessions.joinToString()}]" }
            filteredSessions.map {
                it.client.syncNotifications()
            }
        } else {
            emptyList()
        }
    }.flatMapLatest { flows ->
        merge(*flows.toTypedArray())
    }.shareIn(
        scope = scope,
        started = SharingStarted.Lazily,
        replay = 20,
    )

    fun observeNotifications() {
        notifications.onEach { notification ->
            val id = notification.toNotificationId()
            if (!notification.actions.contains(SyncNotification.Action.Notify)) {
                log.i { "Ignore notification $id with actions [${notification.actions.joinToString()}]" }
                return@onEach
            }

            val parsedNotification = tryOrNull {
                Json.parseToJsonElement(notification.rawEvent).jsonObject
            }
            val notificationServerTs = tryOrNull {
                parsedNotification?.get("origin_server_ts")?.jsonPrimitive?.longOrNull
            }

            if (notificationServerTs != null && notificationServerTs < launchTimestamp) {
                log.w { "Skipping past notification by timestamp for $id" }
                return@onEach
            }

            val mapped = notification.toNotificationData(notificationServerTs ?: System.currentTimeMillis())

            log.d { "Notifying for $id, mapped=${mapped != null}" }
            if (mapped == null || id !is NotificationId.Event) {
                val formatted = NotificationEventTextFormat.notificationToText(
                    content = notification.content,
                    senderId = notification.senderId,
                    senderName = notification.senderInfo.displayName ?: notification.senderId.value,
                    roomDisplayName = notification.roomInfo.displayName,
                    isDirect = notification.roomInfo.isDirect,
                )
                platformNotify(
                    id = id,
                    title = notification.roomInfo.displayName,
                    message = formatted,
                    largeImage = notification.roomInfo.avatarUrl?.let { MediaSource(it) }
                        ?: notification.senderInfo.avatarUrl?.let { MediaSource(it) },
                )
            } else {
                platformNotifyMessage(
                    id = id,
                    data = mapped,
                )
            }
        }.launchIn(scope)
    }
}

fun SyncNotification.toNotificationId(): NotificationId = eventId?.let { eventId ->
    NotificationId.Event(
        sessionId = sessionId,
        roomId = roomId,
        eventId = eventId,
    )
} ?: NotificationId.Room(
    sessionId = sessionId,
    roomId = roomId,
)

fun SyncNotification.toNotificationData(timestamp: Long) = eventId?.let { eventId ->
    NotificationData(
        sessionId = sessionId,
        eventId = eventId,
        threadId = threadId,
        roomId = roomId,
        senderId = senderId,
        senderAvatarUrl = senderInfo.avatarUrl,
        senderDisplayName = senderInfo.displayName,
        senderIsNameAmbiguous = senderInfo.isNameAmbiguous,
        roomAvatarUrl = roomInfo.avatarUrl ?: senderInfo.avatarUrl.takeIf { roomInfo.isDirect },
        roomDisplayName = roomInfo.displayName,
        isDirect = roomInfo.isDirect,
        isDm = roomInfo.isDirect,
        isSpace = roomInfo.isSpace,
        isEncrypted = roomInfo.isEncrypted,
        isNoisy = isNoisy,
        timestamp = timestamp,
        content = content,
        hasMention = hasMention,
        roomJoinRule = roomInfo.joinRule,
    )
}
