package chat.schildi.revenge.model.verification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chat.schildi.revenge.Destination
import chat.schildi.revenge.GlobalActionsScope
import chat.schildi.revenge.TitleProvider
import chat.schildi.revenge.UiState
import chat.schildi.resources.toStringHolder
import chat.schildi.revenge.model.LoadCheckPoint
import chat.schildi.revenge.model.LoadStateHolder
import chat.schildi.revenge.model.account.DeviceVerificationProvider
import chat.schildi.revenge.model.account.RevengeDeviceVerificationProvider
import co.touchlab.kermit.Logger
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.verification.SessionVerificationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import shire.res.generated.resources.Res
import shire.res.generated.resources.verification_request_title
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
class VerificationRequestViewModel(
    val sessionId: SessionId,
    private val verificationProvider: DeviceVerificationProvider = RevengeDeviceVerificationProvider,
) : ViewModel(), TitleProvider {
    private val log = Logger.withTag("VerificationVM")

    private val loadStateHolder = LoadStateHolder(
        LoadCheckPoint.Client(sessionId),
        LoadCheckPoint.VerificationRequest,
    )
    val loadState = loadStateHolder.state

    private val clientFlow = UiState.selectClient(sessionId, viewModelScope, loadStateHolder)

    val ownDeviceId = clientFlow
        .map { it?.deviceId }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val verificationService = clientFlow.map {
        it?.sessionVerificationService
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val cancelRequested = AtomicBoolean(false)
    private val hasAcceptedOrStarted = AtomicBoolean(false)
    private val _pendingApproveAck = MutableStateFlow(false)
    val pendingApproveAck = _pendingApproveAck.asStateFlow()

    private fun resetViewModelState() {
        cancelRequested.store(false)
        hasAcceptedOrStarted.store(false)
        _pendingApproveAck.value = false
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val verificationFlowState = verificationService.flatMapLatest { service ->
        // Reset on new service
        resetViewModelState()
        service?.reset(cancelAnyPendingVerificationAttempt = false)
        // Map flow
        service?.verificationFlowState ?: flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val activeVerificationRequest = verificationProvider.selectVerificationRequest(sessionId, viewModelScope)

    override val windowTitle = flowOf(Res.string.verification_request_title.toStringHolder())

    override fun verifyDestination(destination: Destination) =
        destination is Destination.VerificationRequest &&
                destination.sessionId == sessionId

    private fun withVerificationService(
        scope: CoroutineScope = viewModelScope,
        onNoService: () -> Unit = {
            log.e("Tried to run action on null verification service")
        },
        block: suspend (SessionVerificationService) -> Unit,
    ) {
        val service = verificationService.value
        if (service == null) {
            onNoService()
            return
        }
        scope.launch(Dispatchers.IO) {
            block(service)
        }
    }

    fun acceptVerificationRequest() = withVerificationService { service ->
        hasAcceptedOrStarted.store(true)
        _pendingApproveAck.value = false
        service.acceptVerificationRequest()
    }

    @OptIn(ExperimentalAtomicApi::class)
    fun cancelVerification() = withVerificationService { service ->
        if (cancelRequested.exchange(true)) {
            log.w("Ignoring duplicated cancel request")
            return@withVerificationService
        }
        _pendingApproveAck.value = false
        service.cancelVerification()
    }

    fun startSasVerification() =  withVerificationService { service ->
        hasAcceptedOrStarted.store(true)
        _pendingApproveAck.value = false
        service.startSasVerification()
    }

    fun approveVerification() = withVerificationService { service ->
        hasAcceptedOrStarted.store(true)
        _pendingApproveAck.value = true
        service.approveVerification()
    }

    fun declineVerification() = withVerificationService { service ->
        hasAcceptedOrStarted.store(true)
        _pendingApproveAck.value = false
        service.declineVerification()
    }

    fun registerRenderer() {
        verificationProvider.registerRequestDestination(sessionId)
    }

    fun unregisterRenderer() {
        resetViewModelState()
        withVerificationService(
            scope = GlobalActionsScope,
            onNoService = {
                if (verificationProvider.unregisterRequestDestination(sessionId)) {
                    log.e("Cannot reset pending verification requests, no service")
                }
            }
        ) { service ->
            if (verificationProvider.unregisterRequestDestination(sessionId)) {
                val cancel = hasAcceptedOrStarted.load() && !cancelRequested.exchange(true)
                if (cancel) {
                    service.cancelVerification()
                }
                service.reset(cancelAnyPendingVerificationAttempt = true)
            }
        }
    }
}
