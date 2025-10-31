package chat.schildi.revenge.matrix

import chat.schildi.revenge.util.ScAppDirs
import chat.schildi.revenge.util.escapeForFilename
import chat.schildi.revenge.util.tryOrNull
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.matrix.rustcomponents.sdk.Client
import org.matrix.rustcomponents.sdk.ClientBuilder
import org.matrix.rustcomponents.sdk.RoomListService
import org.matrix.rustcomponents.sdk.RoomListServiceState
import org.matrix.rustcomponents.sdk.RoomListServiceStateListener
import org.matrix.rustcomponents.sdk.SlidingSyncVersionBuilder
import org.matrix.rustcomponents.sdk.SyncService
import org.matrix.rustcomponents.sdk.SyncServiceState
import org.matrix.rustcomponents.sdk.SyncServiceStateObserver
import java.io.File
import java.net.InetAddress

class MatrixClient(
    val homeserver: String,
    val username: String,
) {

    private val log = Logger.withTag("Matrix/$username/$homeserver")
    private val lock = Mutex()

    private var rustClient: Client? = null
    private var rustSyncService: SyncService? = null
    private var rustRoomListService: RoomListService? = null

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

    private val _syncServiceState = MutableStateFlow<SyncServiceState?>(null)
    val syncServiceState = _syncServiceState.asStateFlow()

    private val _roomListServiceState = MutableStateFlow<RoomListServiceState?>(null)
    val roomListServiceState = _roomListServiceState.asStateFlow()

    internal val selfFlowIfValid = syncServiceState.map { it?.let { this } }

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
            .slidingSyncVersionBuilder(SlidingSyncVersionBuilder.DISCOVER_NATIVE)
            .build()
        lock.withLock {
            if (rustClient != null) {
                log.e { "Race condition initializing new client" }
                destroyClient()
            }
            rustClient = newClient
        }
        return newClient
    }

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
        onLoggedIn()
    }.also {
        if (it.isFailure) {
            log.w(it.exceptionOrNull()) { "Failed to log in" }
        }
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
            onLoggedIn()
        }
    }.also {
        if (it.isFailure) {
            log.w(it.exceptionOrNull()) { "Failed to restore session" }
        }
    }

    fun userId() = rustClient?.userId()

    private suspend fun onLoggedIn() {
        buildSyncService()
        buildRoomListService()
    }

    private suspend fun buildSyncService(): SyncService? {
        log.d { "Building sync service..." }
        val syncService = lock.withLock {
            val client = rustClient ?: return null
            client.syncService()
                .withOfflineMode()
                .finish()
                .also {
                    rustSyncService?.destroy()
                    rustSyncService = it
                }
        }
        log.d { "Observing sync service..." }
        syncService.state(object : SyncServiceStateObserver {
            override fun onUpdate(state: SyncServiceState) {
                log.v { "Sync service update: $state" }
                _syncServiceState.value = state
            }

        })
        log.d { "Starting sync service..." }
        syncService.start()
        log.d { "Sync service started" }
        return syncService
    }

    private suspend fun buildRoomListService(): RoomListService? {
        log.d { "Building room list service..." }
        val roomListService = lock.withLock {
            val syncService = rustSyncService ?: return null
            syncService.roomListService().also {
                rustRoomListService?.destroy()
                rustRoomListService = it
            }
        }
        log.d { "Observing room list service..." }
        roomListService.state(object : RoomListServiceStateListener {
            override fun onUpdate(state: RoomListServiceState) {
                log.v { "Room list service update: $state" }
                _roomListServiceState.value = state
            }
        })
        return roomListService
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

    private suspend fun clearState() {
        _syncServiceState.emit(null)
        _roomListServiceState.emit(null)
    }

    internal suspend fun logout() = runCatching {
        log.d { "Logging client out" }
        clearState()
        lock.withLock {
            try {
                rustClient?.logout()
            } catch (e: Throwable) {
                log.e(e) { "Failed to log out client" }
            }
            destroyClient()
            sessionFile.delete()
            sessionCacheDir.deleteRecursively()
            sessionDataDir.deleteRecursively()
        }
        log.d { "Logout completed" }
    }

    suspend fun destroyClient() {
        clearState()
        lock.withLock {
            rustSyncService?.destroy()
            rustSyncService = null
            rustRoomListService?.destroy()
            rustRoomListService = null
            rustClient?.destroy()
            rustClient = null
            // Duplicate clear state just in case
            clearState()
        }
    }
}
