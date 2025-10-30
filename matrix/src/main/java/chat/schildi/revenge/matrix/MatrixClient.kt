package chat.schildi.revenge.matrix

import chat.schildi.revenge.util.ScAppDirs
import chat.schildi.revenge.util.escapeForFilename
import chat.schildi.revenge.util.tryOrNull
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.matrix.rustcomponents.sdk.Client
import org.matrix.rustcomponents.sdk.ClientBuilder
import java.io.File
import java.net.InetAddress

class MatrixClient(
    val homeserver: String,
    val username: String,
) {

    private val log = Logger.withTag("Matrix/$username")
    private val lock = Mutex()

    private var rustClient: Client? = null

    private val sessionSubDirs =
        homeserver.removePrefix("https://").escapeForFilename() + File.separator +
                username.escapeForFilename()
    private val sessionDataDir =
        File(ScAppDirs.getUserDataDir() + File.separator + sessionSubDirs).also {
            it.mkdirs()
        }
    private val sessionFile = File(sessionDataDir.path + File.separator + "scSession.json")
    private val sessionCacheDir =
        File(ScAppDirs.getUserCacheDir() + File.separator + sessionSubDirs).also {
            it.mkdirs()
        }

    init {
        log.d { "Using session storage at ${sessionDataDir.path}" }
    }

    private suspend fun ensureClient(): Client {
        rustClient?.let { return it }
        val newClient = ClientBuilder()
            .homeserverUrl(homeserver)
            .username(username)
            .sessionPaths(
                dataPath = sessionDataDir.path,
                cachePath = sessionCacheDir.path,
            )
            //.setSessionDelegate() // TODO for secure keychain storing...?
            .enableShareHistoryOnInvite(true)
            .threadsEnabled(false, false)
            .build()
        lock.withLock {
            if (rustClient != null) {
                log.e { "Race condition initializing new client" }
                rustClient?.destroy()
                rustClient = newClient
            }
            rustClient = newClient
        }
        return newClient
    }

    suspend fun restoreSession() = runCatching {
        withContext(Dispatchers.IO) {
            val session = sessionFile.inputStream().use {
                Json.decodeFromString<ScSession>(it.readAllBytes().decodeToString())
            }.toSdkSession()
            val newClient = ensureClient()
            newClient.restoreSession(session)
            newClient.session()
            log.d { "Successfully restored session" }
        }
    }.also {
        if (it.isFailure) {
            log.w(it.exceptionOrNull()) { "Failed to restore session" }
        }
    }

    fun userId() = rustClient?.userId()

    suspend fun login(password: String) = runCatching {
        val newClient = ensureClient()
        val deviceName = initialDeviceName()
        log.d { "Logging in client \"${deviceName}\" via password" }
        newClient.login(
            username = username,
            password = password,
            initialDeviceName = deviceName,
            deviceId = null
        )
        log.d { "Successfully logged in" }
        persistSession()
    }.also {
        if (it.isFailure) {
            log.w(it.exceptionOrNull()) { "Failed to log in" }
        }
    }

    suspend fun persistSession() = runCatching {
        rustClient?.let { client ->
            withContext(Dispatchers.IO) {
                val session = client.session()
                val encodedSession = Json.encodeToString(session.toScSession())
                    .toByteArray(Charsets.UTF_8)
                sessionFile.outputStream().use {
                    it.write(encodedSession)
                }
                log.d { "Session persisted for ${session.userId}" }
            }
        }
    }

    private fun initialDeviceName(): String = buildString {
        append("Schildi's Revenge")
        val deviceName = tryOrNull { InetAddress.getLocalHost().hostName }
            ?: System.getenv("HOST")
            ?: System.getenv("HOSTNAME")
            ?: System.getenv("COMPUTERNAME")
        if (deviceName != null) {
            append(" (")
            append(deviceName)
            append(")")
        }
    }

    suspend fun logout() {
        log.d { "Logging client out" }
        val oldClient = lock.withLock {
            rustClient.also {
                rustClient = null
            }
        }
        try {
            oldClient?.logout()
        } catch (e: Throwable) {
            log.e(e) { "Failed to log out client" }
        }
        oldClient?.destroy()
    }

    suspend fun destroy() {
        lock.withLock {
            rustClient?.destroy()
            rustClient = null
        }
    }
}
