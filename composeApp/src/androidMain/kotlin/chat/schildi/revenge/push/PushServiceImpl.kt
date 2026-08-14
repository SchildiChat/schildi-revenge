package chat.schildi.revenge.push

import co.touchlab.kermit.Logger
import org.unifiedpush.android.connector.FailedReason
import org.unifiedpush.android.connector.PushService
import org.unifiedpush.android.connector.data.PushEndpoint
import org.unifiedpush.android.connector.data.PushMessage

class PushServiceImpl : PushService() {
    private val log = Logger.withTag("AndroidPushService")

    override fun onMessage(message: PushMessage, instance: String) {
        PushNotificationHandler.onPushReceived(message.content, instance)
        /* TODO
        val payload = UnifiedPushPayloadParser.parse(message.content)
        if (payload == null) {
            log.w { "Ignoring malformed UnifiedPush payload" }
            return
        }
        runBlocking {
            val initialRegistration = UnifiedPushRegistrations.findByClientSecret(instance)
            if (initialRegistration == null) {
                log.w { "Ignoring UnifiedPush payload for an unknown instance" }
                return@runBlocking
            }
            UnifiedPushQueue.withSessionProcessLock(initialRegistration.sessionId) {
                val registration = UnifiedPushRegistrations.findByClientSecret(instance)
                    ?: return@withSessionProcessLock
                UnifiedPushQueue.enqueue(
                    UnifiedPushQueueRecord(
                        sessionId = registration.sessionId,
                        clientSecret = instance,
                        roomId = payload.roomId,
                        eventId = payload.eventId,
                        receivedAt = System.currentTimeMillis(),
                    )
                )
                UnifiedPushWorkScheduler.enqueueNotifications(applicationContext, registration.sessionId)
            }
        }
         */
    }

    override fun onNewEndpoint(endpoint: PushEndpoint, instance: String) {
        AndroidPushRegistrationHandler.onNewEndpoint(endpoint, instance)
    }

    override fun onRegistrationFailed(reason: FailedReason, instance: String) {
        log.e { "UnifiedPush registration failed: $reason" }
        /* TODO
        runBlocking {
            UnifiedPushRegistrations.findByClientSecret(instance)?.let {
                UnifiedPushManager.allowRegistrationRetry(it.sessionId)
            }
        }
         */
    }

    override fun onTempUnavailable(instance: String) {
        log.w { "UnifiedPush distributor is temporarily unavailable" }
        /* TODO
        runBlocking {
            UnifiedPushRegistrations.findByClientSecret(instance)?.let {
                UnifiedPushManager.allowRegistrationRetry(it.sessionId)
            }
        }
         */
    }

    override fun onUnregistered(instance: String) {
        /* TODO
        runBlocking {
            val initialRegistration = UnifiedPushRegistrations.findByClientSecret(instance) ?: return@runBlocking
            UnifiedPushQueue.withSessionProcessLock(initialRegistration.sessionId) {
                val registration = UnifiedPushRegistrations.findByClientSecret(instance)
                    ?: return@withSessionProcessLock
                if (UnifiedPushManager.notificationsAllowed(applicationContext)) {
                    UnifiedPush.register(applicationContext, instance = registration.clientSecret)
                }
            }
        }
         */
    }
}
