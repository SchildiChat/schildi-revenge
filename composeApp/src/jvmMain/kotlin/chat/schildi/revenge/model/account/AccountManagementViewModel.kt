package chat.schildi.revenge.model.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chat.schildi.revenge.Destination
import chat.schildi.revenge.DestinationStateHolder
import chat.schildi.revenge.UiState
import chat.schildi.revenge.flatMergeCombinedWith
import chat.schildi.revenge.model.verification.RevengeDeviceVerificationProvider
import chat.schildi.revenge.model.verification.ScOutgoingVerificationRequest
import co.touchlab.kermit.Logger
import io.element.android.libraries.matrix.api.auth.MatrixHomeServerDetails
import io.element.android.libraries.matrix.api.auth.OAuthPrompt
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.encryption.BackupState
import io.element.android.libraries.matrix.api.encryption.RecoveryState
import io.element.android.libraries.matrix.api.verification.SessionVerifiedStatus
import io.element.android.libraries.matrix.api.verification.VerificationRequest
import io.element.android.libraries.sessionstorage.api.SessionData
import io.element.android.x.di.AppGraph
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.net.URI

data class AccountManagementData(
    val session: SessionData,
    val sessionVerifiedStatus: SessionVerifiedStatus? = null,
    val backupState: BackupState? = null,
    val recoveryState: RecoveryState? = null,
) {
    val needsVerification: Boolean
        get() = sessionVerifiedStatus == SessionVerifiedStatus.NotVerified
    val sessionId: SessionId
        get() = SessionId(session.userId)
}

enum class LoginVariant {
    PASSWORD,
    OAUTH,
}

class AccountManagementViewModel(
    appGraph: AppGraph = UiState.appGraph,
    private val oAuthRepo: OAuthRepo = RevengeOAUthRepo,
) : ViewModel() {
    private val sessionStore = appGraph.sessionStore
    private val authService = appGraph.authenticationService
    private val sessionCache = appGraph.sessionCache

    private val log = Logger.withTag("AccountManagement")

    private val sessions = sessionStore.sessionsFlow()

    private val sessionIdComparator = UiState.sessionIdComparator

    val oauthState = oAuthRepo.state

    val data = sessions.flatMergeCombinedWith(
        map = { session, _ ->
            val client = sessionCache.getOrRestore(SessionId(session.userId)).getOrNull()
                ?: return@flatMergeCombinedWith flowOf(AccountManagementData(session))
            combine(
                client.sessionVerificationService.sessionVerifiedStatus,
                client.encryptionService.backupStateStateFlow,
                client.encryptionService.recoveryStateStateFlow,
            ) { sessionVerifiedStatus, backupState, recoveryState ->
                AccountManagementData(
                    session = session,
                    sessionVerifiedStatus = sessionVerifiedStatus,
                    backupState = backupState,
                    recoveryState = recoveryState,
                )
            }
        },
        merge = { accounts, comparator ->
            accounts
                .sortedWith(AccountComparator(comparator) {
                    SessionId(it.session.userId)
                })
                .toPersistentList()
        },
        onEmpty = { persistentListOf() },
        other = sessionIdComparator,
    ).stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        persistentListOf()
    )

    suspend fun setHomeserver(homeserver: String) =
        authService.setHomeserver(normalizeHomeserver(homeserver))
            .onSuccess { log.d { "Set homeserver to $homeserver" } }
            .onFailure { log.w("Failed to set homeserver to $homeserver", it) }

    private fun normalizeHomeserver(homeserver: String): String =
        homeserver.trim().let {
            if (it.contains("://")) it else "https://$it"
        }

    suspend fun loginWithPassword(username: String, password: String): Result<SessionId> =
        authService.login(username, password)
            .onSuccess { log.i { "Logged in to $username" } }
            .onFailure { log.w("Failed to log in to $username", it) }

    suspend fun loginWithBrowser(): Result<Unit> {
        return runCatching {
            val oauthDetails = authService.getOAuthUrl(
                prompt = OAuthPrompt.Login,
                loginHint = null,
            ).getOrThrow()
            oAuthRepo.onOAuthRequestLaunched(oauthDetails)
            openBrowser(oauthDetails.url)
        }.onSuccess {
            log.i { "Opened browser login" }
        }.onFailure { failure ->
            log.w("Failed to log in with browser", failure)
            oAuthRepo.onOAuthRequestCancelled()
        }
    }

    private fun openBrowser(url: String) {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            error("Opening a browser is not supported on this desktop")
        }
        Desktop.getDesktop().browse(URI(url))
    }

    suspend fun verify(session: SessionData, recoveryKey: String): Result<Unit> {
        return sessionCache.getOrRestore(SessionId(session.userId))
            .getOrElse { return Result.failure(it) }
            .encryptionService
            .recover(recoveryKey)
            .onSuccess { log.i { "Verified ${session.userId}" } }
            .onFailure { log.w("Failed to verify ${session.userId}", it) }
    }

    fun launchDeviceVerification(
        sessionId: SessionId,
        destinationStateHolder: DestinationStateHolder,
    ): Result<Unit> {
        val client = UiState.currentClientFor(sessionId) ?: return Result.failure(IllegalStateException("Client not ready"))
        viewModelScope.launch {
            client.sessionVerificationService.requestDeviceVerification()
            RevengeDeviceVerificationProvider.setActiveRequest(
                ScOutgoingVerificationRequest(
                    sessionId = sessionId,
                    ts = System.currentTimeMillis(),
                    request = VerificationRequest.Outgoing.CurrentSession,
                )
            )
            destinationStateHolder.navigate(Destination.VerificationRequest(sessionId))
        }
        return Result.success(Unit)
    }

    suspend fun logout(session: SessionData, isTokenValid: Boolean): Result<Unit> {
        val sessionId = SessionId(session.userId)
        val restoreFailed = sessionCache.getOrRestore(sessionId)
            .getOrElse { if (isTokenValid) return Result.failure(it) else null }
            ?.logout(userInitiated = true, ignoreSdkError = !isTokenValid) == null
        if (restoreFailed && !isTokenValid) {
            log.e { "Failed to logout session for $sessionId, deleting anyway" }
            sessionStore.removeSession(session.userId)
        }
        return Result.success(Unit)
    }
}
