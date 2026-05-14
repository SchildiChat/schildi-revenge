package chat.schildi.revenge.model.account

import chat.schildi.revenge.CombinedSessions
import chat.schildi.revenge.UiState
import chat.schildi.revenge.compose.util.ComposableStringHolder
import chat.schildi.revenge.compose.util.toStringHolder
import chat.schildi.revenge.flatMerge
import chat.schildi.revenge.notification.NotificationProcessor
import co.touchlab.kermit.Logger
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.verification.SessionVerificationServiceListener
import io.element.android.libraries.matrix.api.verification.VerificationRequest
import io.ktor.util.collections.ConcurrentMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.stateIn
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.verification_incoming_request_prompt
import shire.composeapp.generated.resources.verification_incoming_request_summary
import shire.composeapp.generated.resources.verification_incoming_request_title

val RevengeDeviceVerificationProvider = DeviceVerificationProvider()

// That timeout is how upstream does it, without any clearing on cancel...? :/
private const val timeoutMillis = 120_000L

data class ScIncomingVerificationRequest(
    val sessionId: SessionId,
    val ts: Long,
    val request: VerificationRequest.Incoming,
) {
    fun isTimedOut() = System.currentTimeMillis() - ts > timeoutMillis

    private val messageVarargs = arrayOf(
        when (request) {
            is VerificationRequest.Incoming.OtherSession -> (request.details.deviceDisplayName ?: request.details.deviceId.value).toStringHolder()
            is VerificationRequest.Incoming.User -> request.details.senderProfile.userId.value.toStringHolder()
        },
        sessionId.value.toStringHolder(),
    )

    // Single message
    val message: ComposableStringHolder = Res.string.verification_incoming_request_prompt.toStringHolder(*messageVarargs)
    // Message split in title & summary
    val title: ComposableStringHolder = Res.string.verification_incoming_request_title.toStringHolder()
    val summary: ComposableStringHolder = Res.string.verification_incoming_request_summary.toStringHolder(*messageVarargs)
}

class DeviceVerificationProvider(
    private val combinedSessions: CombinedSessions = UiState.combinedSessions,
) {
    private val log = Logger.withTag("DeviceVerificationProvider")
    private val scope = CoroutineScope(Dispatchers.Default)

    private val listeners = ConcurrentMap<SessionId, VerificationListener>()

    private inner class VerificationListener(
        val sessionId: SessionId,
    ) : SessionVerificationServiceListener {
        override fun onIncomingSessionRequest(verificationRequest: VerificationRequest.Incoming) {
            val now = System.currentTimeMillis()
            val request = ScIncomingVerificationRequest(
                sessionId = sessionId,
                ts = now,
                request = verificationRequest,
            )
            if (UiState.postIncomingVerificationRequest(request)) {
                NotificationProcessor.notifyIncomingVerificationRequest(request)
            } else {
                log.e("Could not post notification request $request, queue full?")
            }
        }
    }

    private val verificationFlows = combinedSessions.flatMerge(
        map = {
            flowOf(Unit)
        },
        onUpdatedInput = { sessions ->
            sessions.forEach { session ->
                val listener = listeners.getOrPut(session.client.sessionId) {
                    VerificationListener(session.client.sessionId)
                }
                session.client.sessionVerificationService.setListener(listener)
            }
        },
        merge = {},
        onEmpty = {},
    ).stateIn(scope, SharingStarted.Lazily, null)

    fun observe() {
        verificationFlows.launchIn(scope)
    }
}
