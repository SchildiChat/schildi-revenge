package chat.schildi.revenge.glue

import chat.schildi.revenge.util.ScAppDirs
import chat.schildi.revenge.util.ScJson
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import io.element.android.libraries.sessionstorage.api.LoggedInState
import io.element.android.libraries.sessionstorage.api.SessionData
import io.element.android.libraries.sessionstorage.api.SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
private data class RevengeSessionStoreData(
    val sessions: List<SessionData> = emptyList(),
)

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
object RevengeSessionStore : SessionStore {
    private val log = Logger.withTag("SessionStore")
    private val sessionDataMutex = Mutex()
    private val sessions = MutableStateFlow<List<SessionData>?>(null)
    private val safeSessions = sessions.filterNotNull()

    private val dataDir = File(ScAppDirs.getUserDataDir()).also {
        it.mkdirs()
    }
    private val sessionsFile = File(dataDir, "sessions.json")

    private suspend fun loadFromFile(): RevengeSessionStoreData = withContext(Dispatchers.IO) {
        if (sessionsFile.exists()) {
            log.d("Loading sessions from config")
            sessionsFile.inputStream().use {
                ScJson.decodeFromString<RevengeSessionStoreData>(it.readAllBytes().decodeToString())
            }.also {
                log.i("Found ${it.sessions.size} sessions in config")
            }
        } else {
            log.i("Creating new sessions config")
            RevengeSessionStoreData()
        }
    }

    private suspend fun ensureLoaded(): List<SessionData> {
        var result: List<SessionData>? = null
        sessionDataMutex.withLock {
            sessions.update {
                result = it ?: loadFromFile().sessions
                result
            }
        }
        return result!!
    }

    private suspend fun persistSessions(sessions: List<SessionData>) = withContext(Dispatchers.IO) {
        log.d("Persisting sessions to config")
        val data = RevengeSessionStoreData(sessions)
        val encoded = ScJson.encodeToString(data).toByteArray(Charsets.UTF_8)
        sessionsFile.outputStream().use {
            it.write(encoded)
        }
        log.d { "Persisted session config for ${sessions.size} sessions" }
    }

    override fun loggedInStateFlow(): Flow<LoggedInState> = safeSessions.map {
        if (it.isEmpty()) {
            LoggedInState.NotLoggedIn
        } else {
            val session = it.toList().maxBy { it.lastUsageIndex }
            LoggedInState.LoggedIn(
                sessionId = session.userId,
                isTokenValid = session.isTokenValid,
            )
        }
    }

    override fun sessionsFlow(): Flow<List<SessionData>> = safeSessions

    override suspend fun addSession(sessionData: SessionData) {
        log.d("Adding new session ${sessionData.deviceId} for ${sessionData.userId}")
        sessionDataMutex.withLock {
            var newSessions: List<SessionData>? = null
            sessions.update {
                val oldSessions = it ?: loadFromFile().sessions
                newSessions = oldSessions.filter { it.userId != sessionData.userId } + sessionData
                newSessions
            }
            persistSessions(newSessions!!)
        }
    }

    override suspend fun updateData(sessionData: SessionData) {
        log.d("Updating session ${sessionData.deviceId} for ${sessionData.userId}")
        sessionDataMutex.withLock {
            var new: List<SessionData>? = null
            sessions.update {
                val oldSessions = it ?: loadFromFile().sessions
                val oldSession = oldSessions.find { it.userId == sessionData.userId }
                if (oldSession == null) {
                    // Cannot update
                    new = null
                    oldSessions
                } else {
                    new = oldSessions.filter { it.userId != sessionData.userId } + sessionData
                    new
                }
            }
            if (new == null) {
                log.e("Did not find ${sessionData.userId} persisted, cannot update data")
            } else {
                persistSessions(new)
            }
        }
    }

    override suspend fun updateUserProfile(
        sessionId: String,
        displayName: String?,
        avatarUrl: String?
    ) {
        log.d("Updating profile $sessionId")
        sessionDataMutex.withLock {
            var new: List<SessionData>? = null
            sessions.update {
                val oldSessions = it ?: loadFromFile().sessions
                val oldSession = oldSessions.find { it.userId == sessionId }
                if (oldSession == null) {
                    // Cannot update
                    new = null
                    oldSessions
                } else {
                    new = oldSessions.filter { it.userId != sessionId } + oldSession.copy(
                        userDisplayName = displayName,
                        userAvatarUrl = avatarUrl,
                    )
                    new
                }
            }
            if (new == null) {
                log.e("Did not find $sessionId persisted, cannot update user profile")
            } else {
                persistSessions(new)
            }
        }
    }

    override suspend fun getSession(sessionId: String): SessionData? {
        return ensureLoaded().find { it.userId == sessionId }
    }

    override suspend fun getAllSessions(): List<SessionData> {
        return ensureLoaded()
    }

    override suspend fun numberOfSessions(): Int {
        return ensureLoaded().size
    }

    override suspend fun getLatestSession(): SessionData? {
        return ensureLoaded().lastOrNull()
    }

    override suspend fun setLatestSession(sessionId: String) {
        // Not supported yet, do we actually need this for anything?
    }

    override suspend fun removeSession(sessionId: String) {
        sessionDataMutex.withLock {
            var new: List<SessionData>? = null
            sessions.update {
                val oldSessions = it ?: loadFromFile().sessions
                oldSessions.filter { it.userId != sessionId }.also {
                    new = it.takeIf { it.size < oldSessions.size }
                }
            }
            if (new == null) {
                log.e("Did not find $sessionId persisted, cannot delete session")
            } else {
                persistSessions(new)
            }
        }
    }
}
