package chat.schildi.revenge.model.verification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chat.schildi.revenge.Destination
import chat.schildi.revenge.TitleProvider
import chat.schildi.revenge.UiState
import chat.schildi.revenge.compose.util.toStringHolder
import chat.schildi.revenge.model.account.ScIncomingVerificationRequest
import co.touchlab.kermit.Logger
import io.element.android.libraries.matrix.api.verification.SessionVerificationService
import io.element.android.libraries.matrix.api.verification.VerificationFlowState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.verification_incoming_request_title
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
class IncomingVerificationRequestViewModel(
    private val request: ScIncomingVerificationRequest,
) : ViewModel(), TitleProvider {
    private val log = Logger.withTag("IncomingVerificationVM")

    private val verificationService: SessionVerificationService? = UiState.currentClientFor(request.sessionId)
        ?.sessionVerificationService

    val cancelRequested = AtomicBoolean(false)
    private val hasAcceptedOrStarted = AtomicBoolean(false)
    private val _pendingApproveAck = MutableStateFlow(false)
    val pendingApproveAck = _pendingApproveAck.asStateFlow()

    val verificationFlowState: StateFlow<VerificationFlowState> = verificationService?.verificationFlowState
        ?: MutableStateFlow(VerificationFlowState.DidFail)

    override val windowTitle = flowOf(Res.string.verification_incoming_request_title.toStringHolder())

    override fun verifyDestination(destination: Destination) =
        destination is Destination.IncomingVerificationRequest &&
                destination.request.sessionId == request.sessionId &&
                destination.request.request.details.flowId == request.request.details.flowId

    init {
        viewModelScope.launch {
            verificationService?.reset(cancelAnyPendingVerificationAttempt = false)
            verificationService?.acknowledgeVerificationRequest(request.request)
        }
    }

    fun acceptVerificationRequest() {
        hasAcceptedOrStarted.store(true)
        _pendingApproveAck.value = false
        viewModelScope.launch {
            verificationService?.acceptVerificationRequest()
        }
    }

    @OptIn(ExperimentalAtomicApi::class)
    fun cancelVerification() {
        if (cancelRequested.exchange(true)) {
            log.w("Ignoring duplicated cancel request")
            return
        }
        _pendingApproveAck.value = false
        viewModelScope.launch {
            verificationService?.cancelVerification()
        }
    }

    fun startSasVerification() {
        hasAcceptedOrStarted.store(true)
        _pendingApproveAck.value = false
        viewModelScope.launch {
            verificationService?.startSasVerification()
        }
    }

    fun approveVerification() {
        hasAcceptedOrStarted.store(true)
        _pendingApproveAck.value = true
        viewModelScope.launch {
            verificationService?.approveVerification()
        }
    }

    fun declineVerification() {
        hasAcceptedOrStarted.store(true)
        _pendingApproveAck.value = false
        viewModelScope.launch {
            verificationService?.declineVerification()
        }
    }

    @OptIn(ExperimentalAtomicApi::class)
    fun resetOnDispose() {
        _pendingApproveAck.value = false
        viewModelScope.launch {
            verificationService?.reset(
                cancelAnyPendingVerificationAttempt = hasAcceptedOrStarted.load() && !cancelRequested.load(),
            )
        }
    }
}
