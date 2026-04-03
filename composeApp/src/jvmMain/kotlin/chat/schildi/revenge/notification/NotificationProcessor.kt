package chat.schildi.revenge.notification

import chat.schildi.notifications.SyncNotification
import chat.schildi.preferences.RevengePrefs
import chat.schildi.preferences.ScPrefs
import chat.schildi.revenge.UiState
import chat.schildi.revenge.plaintext.NotificationEventTextFormat
import chat.schildi.revenge.util.tryOrNull
import co.touchlab.kermit.Logger
import io.element.android.libraries.matrix.api.media.MediaSource
import kotlinx.coroutines.CoroutineScope
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

object NotificationProcessor {
    private val log = Logger.withTag("NotificationProcessor")
    private val scope = CoroutineScope(Dispatchers.IO)
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

            if (notificationServerTs == null) {
                log.e { "Failed to parse notification timestamp for $id" }
                return@onEach
            }
            if (notificationServerTs < launchTimestamp) {
                log.w { "Skipping past notification by timestamp for $id" }
                return@onEach
            }
            val formatted = NotificationEventTextFormat.notificationToText(notification)
            log.d { "Notifying for $id" }
            Notifier.notify(
                id = id,
                title = notification.roomInfo.displayName,
                message = formatted,
                largeImage = notification.roomInfo.avatarUrl?.let { MediaSource(it) }
                    ?: notification.senderInfo.avatarUrl?.let { MediaSource(it) },
            )
        }.launchIn(scope)
    }
}

fun SyncNotification.toNotificationId() = eventId?.let { eventId ->
    NotificationId.Event(
        sessionId = sessionId,
        roomId = roomId,
        eventId = eventId,
    )
} ?: NotificationId.Room(
    sessionId = sessionId,
    roomId = roomId,
)
