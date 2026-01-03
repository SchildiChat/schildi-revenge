package chat.schildi.revenge.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chat.schildi.revenge.UiState
import chat.schildi.revenge.flatMergeCombinedWith
import chat.schildi.revenge.model.account.AccountComparator
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.Inject
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.encryption.BackupState
import io.element.android.libraries.matrix.api.encryption.RecoveryState
import io.element.android.libraries.matrix.api.verification.SessionVerifiedStatus
import io.element.android.libraries.sessionstorage.api.SessionData
import io.element.android.x.di.AppGraph
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class AccountManagementData(
    val session: SessionData,
    val sessionVerifiedStatus: SessionVerifiedStatus? = null,
    val backupState: BackupState? = null,
    val recoveryState: RecoveryState? = null,
) {
    val needsVerification: Boolean
        get() = sessionVerifiedStatus == SessionVerifiedStatus.NotVerified
}

@Inject
class AccountManagementViewModel(
    appGraph: AppGraph = UiState.appGraph,
) : ViewModel() {
    private val sessionStore = appGraph.sessionStore
    private val authService = appGraph.authenticationService
    private val sessionCache = appGraph.sessionCache

    private val log = Logger.withTag("AccountManagement")

    private val sessions = sessionStore.sessionsFlow()

    private val sessionIdComparator = UiState.sessionIdComparator

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
        authService.setHomeserver(homeserver)
            .onSuccess { log.d { "Set homeserver to $homeserver" } }
            .onFailure { log.w("Failed to set homeserver to $homeserver", it) }

    suspend fun login(username: String, password: String): Result<SessionId> =
        authService.login(username, password)
            .onSuccess { log.i { "Logged in to $username" } }
            .onFailure { log.w("Failed to log in to $username", it) }

    suspend fun verify(session: SessionData, recoveryKey: String): Result<Unit> {
        return sessionCache.getOrRestore(SessionId(session.userId))
            .getOrElse { return Result.failure(it) }
            .encryptionService
            .recover(recoveryKey)
            .onSuccess { log.i { "Verified ${session.userId}" } }
            .onFailure { log.w("Failed to verify ${session.userId}", it) }
    }

    suspend fun logout(session: SessionData): Result<Unit> {
        sessionCache.getOrRestore(SessionId(session.userId))
            .getOrElse { return Result.failure(it) }
            .logout(userInitiated = true, ignoreSdkError = false)
        return Result.success(Unit)
    }
}
