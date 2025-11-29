package chat.schildi.revenge

import androidx.compose.ui.window.ApplicationScope
import chat.schildi.revenge.compose.util.ComposableStringHolder
import chat.schildi.revenge.Destination
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.createGraphFactory
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.x.di.AppGraph
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentHashMap
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndIncrement

@OptIn(ExperimentalAtomicApi::class)
object UiState {
    private val log = Logger.withTag("UiState")
    private val scope = CoroutineScope(Dispatchers.IO)

    val appGraph: AppGraph = createGraphFactory<AppGraph.Factory>().create()
    private val windowCounter = AtomicInt(0)
    private val _windows = MutableStateFlow<ImmutableList<WindowState>>(
        persistentListOf(
            createWindow(Destination.Splash),
        )
    )
    val windows = _windows.asStateFlow()
    private var hasClearedSplashScreen = false

    init {
        scope.launch {
            // Kick initial session load
            appGraph.sessionStore.getAllSessions()
        }
    }

    val matrixClients = appGraph.sessionStore.sessionsFlow().map {
        val persistedSessions = appGraph.sessionStore.getAllSessions()
        log.i("Restoring ${persistedSessions.size} sessions")
        val sessions = persistedSessions.mapNotNull { sessionData ->
            log.i("Restoring session for ${sessionData.userId}")
            appGraph.sessionCache.getOrRestore(UserId(sessionData.userId))
                .onFailure { log.e("Failed to restore session for ${sessionData.userId}", it) }
                .getOrNull()
        }
        log.i("${sessions.size} sessions restored")

        if (!hasClearedSplashScreen) {
            clearSplashScreen()
            hasClearedSplashScreen = true
        }
        sessions.associateBy { it.sessionId }.toPersistentHashMap()
    }.stateIn(scope, SharingStarted.Eagerly, persistentHashMapOf())

    val combinedSessions: CombinedSessions = matrixClients.map {
        it.values.map {
            LoadedSession(it, appGraph.sessionGraphFactory.create(it)).also { session ->
                session.client.syncService.startSync()
                    .onFailure { log.e("Failed to start sync for ${session.client.sessionId}", it) }
            }
        }
    }.stateIn(scope, SharingStarted.Eagerly, persistentListOf())

    fun selectClient(sessionId: SessionId, scope: CoroutineScope) = matrixClients.map {
        it[sessionId]
    }.stateIn(scope, SharingStarted.Eagerly, null)

    private fun clearSplashScreen() {
        _windows.update { windows ->
            windows.mapNotNull { window ->
                if (window.destinationHolder.state.value.destination is Destination.Splash) {
                    if (windows.size > 1) {
                        // Already have other windows open??
                        null
                    } else {
                        window.also {
                            it.destinationHolder.navigate(Destination.Inbox)
                        }
                    }
                } else {
                    window
                }
            }.toPersistentList()
        }
    }

    private fun createWindow(
        initialDestination: Destination,
        initialTitle: ComposableStringHolder? = null
    ): WindowState {
        return WindowState(
            windowId = windowCounter.fetchAndIncrement(),
            destinationHolder = DestinationStateHolder.forInitialDestination(initialDestination, initialTitle),
        )
    }

    fun openWindow(destination: Destination, initialTitle: ComposableStringHolder? = null) {
        val newWindow = createWindow(destination, initialTitle)
        _windows.update {
            (it + newWindow).toPersistentList()
        }
    }

    fun closeWindow(windowId: Int, scope: ApplicationScope) {
        var closedLastWindow = false
        _windows.update {
            it.filter { it.windowId != windowId }.toPersistentList().also {
                closedLastWindow = it.isEmpty()
            }
        }
        if (closedLastWindow) {
            // TODO close to tray if we can
            scope.exitApplication()
        }
    }
}
