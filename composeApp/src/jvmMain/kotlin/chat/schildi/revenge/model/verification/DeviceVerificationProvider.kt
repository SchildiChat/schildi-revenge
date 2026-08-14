package chat.schildi.revenge.model.verification

import chat.schildi.revenge.CombinedSessions
import chat.schildi.revenge.ScCoroutines
import chat.schildi.revenge.UiState
import chat.schildi.resources.ComposableStringHolder
import chat.schildi.resources.toStringHolder
import chat.schildi.revenge.flatMerge
import chat.schildi.revenge.model.LoadCheckPoint
import chat.schildi.revenge.model.LoadStateHolder
import chat.schildi.revenge.model.asCheckpointLoadedOrPending
import chat.schildi.revenge.notification.platformNotify
import chat.schildi.revenge.notification.toNotificationId
import co.touchlab.kermit.Logger
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.verification.SessionVerificationServiceListener
import io.element.android.libraries.matrix.api.verification.VerificationRequest
import io.ktor.util.collections.ConcurrentMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import shire.res.generated.resources.Res
import shire.res.generated.resources.verification_incoming_request_prompt
import shire.res.generated.resources.verification_incoming_request_summary
import shire.res.generated.resources.verification_request_title
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.decrementAndFetch
import kotlin.concurrent.atomics.incrementAndFetch

val RevengeDeviceVerificationProvider = DeviceVerificationProvider()

// That timeout is how upstream does it, without any clearing on cancel...? :/
private const val timeoutMillis = 120_000L

sealed interface ScVerificationRequest {
    val sessionId: SessionId
    val ts: Long
    val request: VerificationRequest
}

data class ScOutgoingVerificationRequest(
    override val sessionId: SessionId,
    override val ts: Long,
    override val request: VerificationRequest.Outgoing,
) : ScVerificationRequest

data class ScIncomingVerificationRequest(
    override val sessionId: SessionId,
    override val ts: Long,
    override val request: VerificationRequest.Incoming,
) : ScVerificationRequest {
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
    val title: ComposableStringHolder = Res.string.verification_request_title.toStringHolder()
    val summary: ComposableStringHolder = Res.string.verification_incoming_request_summary.toStringHolder(*messageVarargs)
}

@OptIn(ExperimentalAtomicApi::class)
class DeviceVerificationProvider(
    private val combinedSessions: CombinedSessions = UiState.combinedSessions,
) {
    private val log = Logger.withTag("DeviceVerificationProvider")
    private val scope = ScCoroutines.scope(Dispatchers.Default, "DeviceVerificationProvider")

    private val activeRequests = MutableStateFlow<Map<SessionId, ScVerificationRequest>>(emptyMap())
    private val currentRequestDestinationCounts = ConcurrentMap<SessionId, AtomicInt>()
    private val incomingRequestListeners = ConcurrentMap<SessionId, VerificationListener>()

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
                scope.launch {
                    platformNotify(
                        id = request.toNotificationId(),
                        title = request.title.renderSuspend(),
                        message = request.summary.renderSuspend(),
                    )
                }
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
                val listener = incomingRequestListeners.getOrPut(session.client.sessionId) {
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

    fun selectVerificationRequest(
        sessionId: SessionId,
        scope: CoroutineScope,
        loadStateHolder: LoadStateHolder? = null,
    ) = activeRequests.map {
        val client = it[sessionId]
        loadStateHolder?.set(
            LoadCheckPoint.VerificationRequest,
            client.asCheckpointLoadedOrPending(),
        )
        client
    }.stateIn(scope, SharingStarted.Eagerly, null)

    fun setActiveRequest(request: ScVerificationRequest): Boolean {
        val verificationService = UiState.currentClientFor(request.sessionId)?.sessionVerificationService
        return if (verificationService == null) {
            log.e("Cannot set active request, no verification service found")
            false
        } else {
            (request.request as? VerificationRequest.Incoming)?.let {
                scope.launch {
                    verificationService.acknowledgeVerificationRequest(it)
                }
            }
            activeRequests.update {
                it + (request.sessionId to request)
            }
            true
        }
    }

    fun registerRequestDestination(sessionId: SessionId) {
        val counter = currentRequestDestinationCounts.getOrPut(sessionId) { AtomicInt(0) }
            .incrementAndFetch()
        log.d { "Increment counter for $sessionId: $counter" }
    }

    /**
     * @return whether the last destination was unregistered - should cancel in flight verification requests
     */
    fun unregisterRequestDestination(sessionId: SessionId): Boolean {
        val counter = currentRequestDestinationCounts.getOrPut(sessionId) { AtomicInt(1) }
            .decrementAndFetch()
        log.d { "Decrement counter for $sessionId: $counter" }
        val isLast = counter <= 0
        if (isLast) {
            activeRequests.update {
                it - sessionId
            }
        }
        return isLast
    }
}
