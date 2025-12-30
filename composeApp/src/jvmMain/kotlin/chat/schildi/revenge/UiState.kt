package chat.schildi.revenge

import androidx.compose.ui.window.ApplicationScope
import chat.schildi.revenge.actions.AbstractAppMessage
import chat.schildi.revenge.actions.AppMessage
import chat.schildi.revenge.compose.util.ComposableStringHolder
import chat.schildi.revenge.compose.util.StringResourceHolder
import chat.schildi.revenge.compose.util.throttleLatest
import chat.schildi.revenge.compose.util.toStringHolder
import chat.schildi.revenge.config.ConfigWatchers
import chat.schildi.revenge.store.AppStateStore
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
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.toast_key_config_reload_error
import shire.composeapp.generated.resources.toast_key_config_reload_success
import kotlin.collections.map
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndIncrement

val GlobalActionsScope = CoroutineScope(Dispatchers.IO)

private const val MESSAGE_ID_KEY_CONFIG = "keyConfig"

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

    private val _minimizedToTray = MutableStateFlow(false)
    val minimizedToTray = _minimizedToTray.asStateFlow()

    private val _globalMessageBoard = MutableSharedFlow<AbstractAppMessage>(3)
    val globalMessageBoard = _globalMessageBoard.asSharedFlow()

    private val keybindingsConfigWatcher = ConfigWatchers.keybindings(
        scope,
        readDefaultFallback = {
            Res.readBytes("files/keybindings-default.toml").decodeToString()
        },
        onReloadSuccess = {
            _globalMessageBoard.tryEmit(
                AppMessage(
                    message = Res.string.toast_key_config_reload_success.toStringHolder(),
                    uniqueId = MESSAGE_ID_KEY_CONFIG,
                )
            )
        },
        onError = { error ->
            _globalMessageBoard.tryEmit(
                AppMessage(
                    message = StringResourceHolder(
                        Res.string.toast_key_config_reload_error,
                        (error?.message ?: "Unknown").toStringHolder()
                    ),
                    uniqueId = MESSAGE_ID_KEY_CONFIG,
                    isError = true,
                    canAutoDismiss = false,
                )
            )
        },
    )
    val keybindingsConfig = keybindingsConfigWatcher.config

    val matrixClients = appGraph.sessionStore.sessionsFlow().map {
        val persistedSessions = appGraph.sessionStore.getAllSessions()
        val startTs = System.currentTimeMillis()
        log.i("Restoring ${persistedSessions.size} sessions")
        val sessions = appGraph.sessionCache.runBatchRestore {
            val sessionJobs = persistedSessions.map { sessionData ->
                scope.async {
                    log.i("Restoring session for ${sessionData.userId}")
                    getOrRestoreInBatch(UserId(sessionData.userId))
                        .onFailure { log.e("Failed to restore session for ${sessionData.userId}", it) }
                        .getOrNull()
                }
            }
            sessionJobs.mapNotNull { it.await() }
        }
        val finishedTs = System.currentTimeMillis()
        log.i("${sessions.size} sessions restored in ${finishedTs - startTs} ms")

        if (!hasClearedSplashScreen) {
            val destination = if (sessions.isEmpty()) {
                Destination.AccountManagement
            } else {
                Destination.Inbox
            }
            clearSplashScreen(destination)
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

    val currentValidSessionIds = combinedSessions.map { it.map { it.client.sessionId.value } }
        .stateIn(scope, SharingStarted.Eagerly, null)

    val appStateStore = AppStateStore(scope)
    val sessionIdComparator = appStateStore.sessionIdComparator

    val mutedAccounts = MutableStateFlow<Set<SessionId>>(emptySet())

    init {
        scope.launch {
            // Kick initial session load
            appGraph.sessionStore.getAllSessions()
        }

        // Restore muted accounts state. After restore, start persisting stuff.
        scope.launch {
            val restoredMutedAccounts = appStateStore.config.filterNotNull().first().mutedAccounts
            log.d { "Restoring ${restoredMutedAccounts.size} muted accounts" }
            if (restoredMutedAccounts.isNotEmpty()) {
                mutedAccounts.update {
                    if (it.isEmpty()) {
                        restoredMutedAccounts.map { SessionId(it) }.toSet()
                    } else {
                        log.w { "Race condition restoring muted accounts" }
                        it
                    }
                }
            }

            // Ensure all accounts tracked in app state sort order
            combine(
                appStateStore.config,
                currentValidSessionIds,
            ) { currentAccountMeta, sessionIds ->
                currentAccountMeta ?: return@combine
                sessionIds ?: return@combine
                if (sessionIds.any { it !in currentAccountMeta.sortedAccounts }) {
                    // Fill in missing accounts, so we get a deterministic order and user has it easier to modify manually
                    appStateStore.ensureAllSessionIdsTracked(sessionIds)
                }
            }.launchIn(scope)

            // Persist muted account setting
            mutedAccounts.throttleLatest(1000).onEach {
                appStateStore.persistMutedAccounts(it.map(SessionId::value))
            }.launchIn(scope)
        }
    }

    fun selectClient(sessionId: SessionId, scope: CoroutineScope) = matrixClients.map {
        it[sessionId]
    }.stateIn(scope, SharingStarted.Eagerly, null)

    fun currentClientFor(sessionId: SessionId) = matrixClients.value[sessionId]

    private fun clearSplashScreen(destination: Destination) {
        _windows.update { windows ->
            windows.mapNotNull { window ->
                if (window.destinationHolder.state.value.destination is Destination.Splash) {
                    if (windows.size > 1) {
                        // Already have other windows open??
                        null
                    } else {
                        window.also {
                            it.destinationHolder.navigate(destination)
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
            exit(scope)
        }
    }

    fun exit(scope: ApplicationScope) {
        scope.exitApplication()
    }

    fun setMinimized(minimized: Boolean) {
        _minimizedToTray.value = minimized
    }
}
