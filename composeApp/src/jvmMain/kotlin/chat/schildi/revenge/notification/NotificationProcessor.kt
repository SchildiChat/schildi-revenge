package chat.schildi.revenge.notification

import chat.schildi.notifications.SyncNotification
import chat.schildi.preferences.RevengePrefs
import chat.schildi.preferences.ScPrefs
import chat.schildi.revenge.UiState
import chat.schildi.revenge.plaintext.NotificationEventTextFormat
import co.touchlab.kermit.Logger
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

object NotificationProcessor {
    private val log = Logger.withTag("NotificationProcessor")
    private val scope = CoroutineScope(Dispatchers.IO)

    @OptIn(ExperimentalCoroutinesApi::class)
    val notifications = combine(
        UiState.combinedSessions,
        UiState.mutedAccounts,
        RevengePrefs.settingFlow(ScPrefs.DESKTOP_NOTIFICATIONS),
    ) { sessions, muted, allowNotifications ->
        if (allowNotifications) {
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
            val formatted = NotificationEventTextFormat.notificationToText(notification)
            val id = notification.toNotificationId()
            // TODO pass room avatar as largeImage
            log.d { "Notifying for $id" }
            Notifier.notify(
                id = id,
                title = notification.roomInfo.displayName,
                formatted
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
