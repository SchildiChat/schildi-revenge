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
    }

    override fun onNewEndpoint(endpoint: PushEndpoint, instance: String) {
        AndroidPushRegistrationHandler.onNewEndpoint(endpoint, instance)
    }

    override fun onRegistrationFailed(reason: FailedReason, instance: String) {
        log.e { "UnifiedPush registration failed: $reason" }
        AndroidPushRegistrationHandler.onPushUnregistered(instance)
    }

    override fun onUnregistered(instance: String) {
        AndroidPushRegistrationHandler.onPushUnregistered(instance)
    }
}
