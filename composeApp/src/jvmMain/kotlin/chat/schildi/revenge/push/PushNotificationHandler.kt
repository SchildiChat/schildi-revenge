package chat.schildi.revenge.push

import chat.schildi.lib.preferences.ScPrefs
import chat.schildi.revenge.RevengeAppGraph
import chat.schildi.revenge.ScCoroutines
import chat.schildi.revenge.database.push.PushNotificationEventEntity
import chat.schildi.revenge.database.revengeDatabase
import chat.schildi.revenge.notification.NotificationId
import chat.schildi.revenge.notification.platformNotify
import chat.schildi.revenge.notification.platformNotifyMessage
import chat.schildi.revenge.preferences.RevengePrefs
import co.touchlab.kermit.Logger
import io.element.android.libraries.core.coroutine.suspendLazy
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.exception.NotificationResolverException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class PushResolutionOutcome {
    Done,
    TransientFailure,
    PermanentFailure,
}

object PushNotificationHandler {
    private const val MAX_FAILURES = 4

    private val log = Logger.withTag("PushNotificationHandler")
    private val scope = ScCoroutines.scope(Dispatchers.IO, "PushNotificationHandler")

    private val pushDao = revengeDatabase.pushNotificationDao()

    fun onPushReceived(content: ByteArray, instance: String) {
        scope.launch {
            val sessionId = pushDao.getSessionId(instance)
            val payload = deserializeUnifiedPushPayload(content)
            if (sessionId == null) {
                log.w { "Ignoring push payload for unknown session via $instance" }
                return@launch
            }
            if (payload == null) {
                log.w { "Ignoring empty push payload for $instance / $sessionId" }
                return@launch
            }
            if (payload.notification == null) {
                log.w { "Ignoring missing push notification payload for $instance / $sessionId" }
                return@launch
            }
            val notification = payload.notification
            if (notification.roomId == null || notification.eventId == null) {
                // Commonly sent when unread count decreases.
                // TODO could we hack some notification auto-dismissing here, by iterating existing notifications
                //  and checking if the latest read receipt happens to be the latest notified message?
                //  (maybe only if `counts` is actually set in the payload)
                log.d {
                    "Ignoring event-less push notification for $instance / $sessionId"
                }
                return@launch
            }
            pushDao.insertPush(
                PushNotificationEventEntity(
                    sessionId = sessionId,
                    roomId = notification.roomId,
                    eventId = notification.eventId,
                    pushSource = instance,
                    timestamp = System.currentTimeMillis(),
                )
            )
            if (RevengePrefs.getSetting(ScPrefs.DEBUG_NOTIFICATIONS)) {
                val notificationId = NotificationId.Event(
                    SessionId(sessionId),
                    RoomId(notification.roomId),
                    EventId(notification.eventId)
                )
                platformNotify(
                    notificationId,
                    title = "$sessionId ${notification.roomId}",
                    message = notification.eventId,
                )
            }
            schedulePushResolutionWork(sessionId, notification.roomId)
        }
    }

    suspend fun resolvePendingPushes(sessionId: SessionId, roomId: RoomId): PushResolutionOutcome {
        val pushes = pushDao.getPendingPushes(sessionId.value, roomId.value, maxFailures = MAX_FAILURES)
        if (pushes.isEmpty()) {
            log.d { "No pending pushes for $sessionId/$roomId" }
            return PushResolutionOutcome.Done
        }
        log.d { "Resolving ${pushes.size} pending pushes for $sessionId/$roomId" }
        val isUnknownSession by suspendLazy { RevengeAppGraph.sessionStore.getSession(sessionId.value) == null }
        val client = RevengeAppGraph.sessionCache.getOrRestore(sessionId)
            .onFailure { e ->
                log.e("Failed to restore session for $sessionId", e)
                pushDao.updatePushes(
                    pushes.map {
                        it.copy(
                            failureCount = it.failureCount + 1,
                            lastFailure = "Session failed to restore: $e",
                            resolvedTimestamp = if (isUnknownSession.await()) System.currentTimeMillis() else null,
                        )
                    }
                )
            }.getOrNull() ?: run {
                return if (isUnknownSession.await()) {
                    log.e("Unknown session $sessionId, fail permanently")
                    PushResolutionOutcome.PermanentFailure
                } else {
                    PushResolutionOutcome.TransientFailure
                }
            }
        val result = client.notificationService.getNotifications(
            mapOf(roomId to pushes.map { EventId(it.eventId) })
        )
        if (result.isFailure) {
            val e = result.exceptionOrNull()
            log.e("Failed to get notifications for $sessionId/$roomId", e)
            pushDao.updatePushes(
                pushes.map {
                    it.copy(
                        failureCount = it.failureCount + 1,
                        lastFailure = e.toString(),
                    )
                }
            )
            return PushResolutionOutcome.TransientFailure
        }
        val results = result.getOrThrow()
        log.d { "Got ${results.size}/${pushes.size} notifications results for $sessionId/$roomId" }
        pushes.forEach { push ->
            val eventResult = results[EventId(push.eventId)]
            val data = eventResult?.getOrNull() ?: run {
                val typedException = eventResult?.exceptionOrNull() as? NotificationResolverException
                if (typedException != null && typedException !is NotificationResolverException.UnknownError) {
                    log.d("Got $typedException result for $sessionId/${push.roomId}/${push.eventId}")
                } else {
                    log.w(
                        "Got no notification result for $sessionId/${push.roomId}/${push.eventId}",
                        eventResult?.exceptionOrNull()
                    )
                }
                val countsAsResolved = when (typedException) {
                    null -> false
                    NotificationResolverException.EventFilteredOut,
                    NotificationResolverException.EventNotFound,
                        // TODO dismiss existing notif if exists for redactions?
                    NotificationResolverException.EventRedacted -> true
                    is NotificationResolverException.UnknownError -> false
                }
                pushDao.updatePushes(
                    listOf(
                        push.copy(
                            failureCount = push.failureCount + 1,
                            lastFailure = eventResult?.exceptionOrNull()?.toString() ?: "SDK ignored the event",
                            resolvedTimestamp = if (countsAsResolved) System.currentTimeMillis() else null,
                        )
                    )
                )
                return@forEach
            }
            log.d("Got notification result for $sessionId/${push.roomId}/${push.eventId}")
            platformNotifyMessage(
                id = NotificationId.Event(
                    sessionId = data.sessionId,
                    roomId = data.roomId,
                    eventId = data.eventId,
                ),
                data = data,
            )
            pushDao.markPushResolved(
                sessionId = data.sessionId.value,
                roomId = data.roomId.value,
                eventId = data.eventId.value,
            )
            // TODO clean up DB for pushes older than certain timestamp
        }
        return PushResolutionOutcome.Done
    }
}
