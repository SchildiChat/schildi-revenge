package chat.schildi.revenge.compose.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chat.schildi.revenge.UiState
import chat.schildi.revenge.flatMerge
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.Inject
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.sessionstorage.api.SessionData
import io.element.android.x.di.AppGraph
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class AccountManagementData(
    val session: SessionData,
    val needsVerification: Boolean,
)

@Inject
class AccountManagementViewModel(
    appGraph: AppGraph = UiState.appGraph,
) : ViewModel() {
    private val sessionStore = appGraph.sessionStore
    private val authService = appGraph.authenticationService
    private val sessionCache = appGraph.sessionCache

    private val log = Logger.withTag("AccountManagement")

    private val sessions = sessionStore.sessionsFlow()

    val data = sessions.flatMerge(
        map = { session ->
            val client = sessionCache.getOrRestore(SessionId(session.userId)).getOrNull()
                ?: return@flatMerge flowOf(AccountManagementData(session, false))
            client.sessionVerificationService.needsSessionVerification.map {
                AccountManagementData(
                    session = session,
                    needsVerification = it,
                )
            }
        },
        merge = {
            it.toPersistentList()
        },
    ).stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        persistentListOf()
    )

    suspend fun setHomeserver(homeserver: String) =
        authService.setHomeserver(homeserver)
            .onSuccess { log.d { "Set homeserver to $homeserver" } }
            .onFailure { log.w { "Failed to set homeserver to $homeserver" } }

    suspend fun login(username: String, password: String): Result<SessionId> =
        authService.login(username, password)
            .onSuccess { log.i { "Logged in to $username" } }
            .onFailure { log.w { "Failed to log in to $username" } }

    suspend fun verify(session: SessionData, recoveryKey: String): Result<Unit> {
        return sessionCache.getOrRestore(SessionId(session.userId))
            .getOrElse { return Result.failure(it) }
            .encryptionService
            .recover(recoveryKey)
    }

    suspend fun logout(session: SessionData): Result<Unit> {
        sessionCache.getOrRestore(SessionId(session.userId))
            .getOrElse { return Result.failure(it) }
            .logout(userInitiated = true, ignoreSdkError = false)
        return Result.success(Unit)
    }
}
