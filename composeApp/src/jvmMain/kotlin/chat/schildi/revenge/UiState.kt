package chat.schildi.revenge

import androidx.compose.ui.window.ApplicationScope
import chat.schildi.preferences.RevengePrefs
import chat.schildi.preferences.ScPrefs
import chat.schildi.revenge.actions.AbstractAppMessage
import chat.schildi.revenge.actions.AppMessage
import chat.schildi.revenge.actions.KeyboardActionHandler
import chat.schildi.revenge.actions.VerificationRequestAppMessage
import chat.schildi.revenge.compose.util.ComposableStringHolder
import chat.schildi.revenge.compose.util.StringResourceHolder
import chat.schildi.revenge.compose.util.toStringHolder
import chat.schildi.revenge.config.ConfigWatchers
import chat.schildi.revenge.model.LoadCheckPoint
import chat.schildi.revenge.model.LoadStateHolder
import chat.schildi.revenge.model.RevengeRoomListDataSource
import chat.schildi.revenge.model.account.ScIncomingVerificationRequest
import chat.schildi.revenge.model.asCheckpointLoadedOrPending
import chat.schildi.revenge.store.AppStateStore
import chat.schildi.revenge.util.throttleLatest
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.createGraphFactory
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.x.di.AppGraph
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentHashMap
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.toast_key_config_reload_error
import shire.composeapp.generated.resources.toast_key_config_reload_success
import java.util.Locale
import kotlin.collections.map
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndIncrement

val GlobalActionsScope = ScCoroutines.scope(Dispatchers.IO, "GlobalActionScope")

private const val MESSAGE_ID_KEY_CONFIG = "keyConfig"

// For KeyboardActionHandler via headless IPC
private const val HEADLESS_WINDOW_ID = -1

@OptIn(ExperimentalAtomicApi::class)
object UiState {
    private val log = Logger.withTag("UiState")
    private val scope = ScCoroutines.scope(Dispatchers.IO, "UiState")
    private val shutdownScope = CoroutineScope(Dispatchers.Default)
    private val isShuttingDown = AtomicBoolean(false)

    val appGraph: AppGraph = createGraphFactory<AppGraph.Factory>().create()
    private val windowCounter = AtomicInt(0)
    private val _windows = MutableStateFlow<ImmutableList<WindowState>>(
        persistentListOf(
            createWindow(Destination.Splash),
        )
    )
    val windows = _windows.asStateFlow()
    private var hasClearedSplashScreen = false

    private var applicationScope: ApplicationScope? = null
    var headlessKeyboardActionHandler: KeyboardActionHandler? = null
        private set

    // Default to minimized - it's less disruptive to toggle true to false during init than other way round
    private val _minimizedToTray = MutableStateFlow(true)
    val minimizedToTray = _minimizedToTray.asStateFlow()

    private val _forceRecreationCounter = MutableStateFlow(0)
    val forceRecreationCounter = _forceRecreationCounter.asStateFlow()
    private val _trayIconRecreationCounter = MutableStateFlow(0)
    val trayIconRecreationCounter = _trayIconRecreationCounter.asStateFlow()

    private val _globalMessageBoard = MutableSharedFlow<AbstractAppMessage>(3)
    val globalMessageBoard = _globalMessageBoard.asSharedFlow()

    val globalLoadState = LoadStateHolder()

    private val closeToTray = RevengePrefs
        .settingFlow(ScPrefs.CLOSE_TO_TRAY)
        .stateIn(
            scope,
            SharingStarted.Eagerly,
            RevengePrefs.getCachedOrDefaultValue(ScPrefs.CLOSE_TO_TRAY),
        )

    private val preferMultiPaneInbox = RevengePrefs
        .settingFlow(ScPrefs.PREFER_DUAL_PANE_INBOX)
        .distinctUntilChanged()
        .onEach { preferMultiPane ->
            // Recreate any potential inbox destinations to apply the new setting
            windows.value.forEach { window ->
                val destination = window.destinationHolder.state.value.destination
                if (destination.category == DestinationCategory.INBOX) {
                    val newDestination = getInboxDestination(preferMultiPane)
                    if (newDestination.type != destination.type) {
                        window.destinationHolder.navigate(newDestination, NavigationPreference.REPLACE)
                    }
                }
            }
        }
        .stateIn(
            scope,
            SharingStarted.Eagerly,
            RevengePrefs.getCachedOrDefaultValue(ScPrefs.PREFER_DUAL_PANE_INBOX),
        )

    private val preferMultiPaneConversation = RevengePrefs
        .settingFlow(ScPrefs.PREFER_CONVERSATION_DETAILS_SPLIT)
        .stateIn(
            scope,
            SharingStarted.Eagerly,
            RevengePrefs.getCachedOrDefaultValue(ScPrefs.PREFER_CONVERSATION_DETAILS_SPLIT),
        )

    fun getInboxDestination(
        preferMultiPane: Boolean = preferMultiPaneInbox.value
    ): Destination = if (preferMultiPane) {
        Destination.InboxConversationMultiPane()
    } else {
        Destination.Inbox
    }

    fun getConversationDestinationFromInbox(
        sessionId: SessionId,
        roomId: RoomId,
        preferMultiPane: Boolean = RevengePrefs.getCachedOrDefaultValue(ScPrefs.PREFER_CONVERSATION_DETAILS_SPLIT),
    ): Destination {
        val conversation = Destination.Conversation(sessionId, roomId)
        return if (preferMultiPane) {
            Destination.ConversationDetailsMultiPane(conversation)
        } else {
            conversation
        }
    }

    val hasInboxOpen = windows.flatMerge(
        map = {
            it.destinationHolder.state
        },
        merge = {
            it.any { it.destination.category == DestinationCategory.INBOX }
        },
        onEmpty = { false },
    ).stateIn(scope, SharingStarted.Eagerly, false)

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
                    autoDismissDuration = null,
                )
            )
        },
    )
    val keybindingsConfig = keybindingsConfigWatcher.config

    // Allow temporarily disabling sessions on clear cache, in order to rebuild the client afterward.
    private val disabledSessions = MutableStateFlow(emptySet<SessionId>())

    val matrixClients = combine(
        disabledSessions,
        appGraph.sessionStore.sessionsFlow()
    ) { disabled, allSessions ->
        val persistedSessions = allSessions.filter { SessionId(it.userId) !in disabled }
        globalLoadState.addExpected(
            *persistedSessions.map { LoadCheckPoint.Client(SessionId(it.userId)) }.toTypedArray()
        )
        val startTs = System.currentTimeMillis()
        log.i("Restoring ${persistedSessions.size} sessions")
        val sessions = appGraph.sessionCache.runBatchRestore {
            val sessionJobs = persistedSessions.map { sessionData ->
                scope.async {
                    val sessionId = SessionId(sessionData.userId)
                    log.i("Restoring session for $sessionId")
                    getOrRestoreInBatch(sessionId)
                        .onFailure {
                            log.e("Failed to restore session for $sessionId", it)
                        }
                        .also {
                            globalLoadState.handleResult(LoadCheckPoint.Client(sessionId), it)
                        }
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
                getInboxDestination()
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
    val sessionIdOrder = appStateStore.sessionIdOrder

    private val _mutedAccounts = MutableStateFlow<Set<SessionId>?>(null)
    val mutedAccounts = _mutedAccounts.asStateFlow()

    val currentLocale = RevengePrefs.settingFlow(ScPrefs.LOCALE).onEach {
        val localeToSet = when (it) {
            "" -> ScPrefs.initialLocale
            else -> Locale.forLanguageTag(it)
        }
        if (localeToSet != Locale.getDefault()) {
            Locale.setDefault(localeToSet)
        }
    }.stateIn(scope, SharingStarted.Eagerly, "")

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
                _mutedAccounts.update {
                    if (it == null) {
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
                appStateStore.persistMutedAccounts(it.orEmpty().map(SessionId::value))
            }.launchIn(scope)
        }

        // Global room list needs just one observing scope for notification settings
        RevengeRoomListDataSource.observeInvalidationSignals(scope)
    }

    fun initializeWith(applicationScope: ApplicationScope, startInTray: Boolean) {
        if (this.applicationScope != null || this.headlessKeyboardActionHandler != null) {
            throw IllegalStateException("Initializing UiState with applicationScope twice")
        }

        // Initialize application scope and headless keyboard handler
        this.applicationScope = applicationScope
        headlessKeyboardActionHandler = KeyboardActionHandler(GlobalActionsScope, HEADLESS_WINDOW_ID)
        _minimizedToTray.value = startInTray
    }

    fun setAccountMuted(sessionId: SessionId, muted: Boolean) {
        _mutedAccounts.update {
            if (muted) {
                it.orEmpty() + sessionId
            } else {
                it.orEmpty() - sessionId
            }
        }
    }

    fun toggleAccountMuted(sessionId: SessionId) {
        _mutedAccounts.update {
            if (it == null) {
                setOf(sessionId)
            } else if (sessionId in it) {
                it - sessionId
            } else {
                it + sessionId
            }
        }
    }

    fun selectClient(
        sessionId: SessionId,
        scope: CoroutineScope,
        loadStateHolder: LoadStateHolder? = null,
    ) = matrixClients.map {
        val client = it[sessionId]
        loadStateHolder?.set(
            LoadCheckPoint.Client(sessionId),
            client.asCheckpointLoadedOrPending(),
        )
        client
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
                            it.destinationHolder.navigate(destination, NavigationPreference.REPLACE)
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
        val effectiveDestination = if (destination is Destination.Conversation && preferMultiPaneConversation.value) {
            Destination.ConversationDetailsMultiPane(destination)
        } else {
            destination
        }
        val newWindow = createWindow(effectiveDestination, initialTitle)
        val wasMinimized = minimizedToTray.value
        _windows.update {
            // New window replaces old state when launched via IPC while minimized
            if (wasMinimized) {
                persistentListOf(newWindow)
            } else {
                (it + newWindow).toPersistentList()
            }
        }
        _minimizedToTray.value = false
    }

    fun closeWindow(windowId: Int) {
        var closedLastWindow = false
        _windows.update {
            it.filter { it.windowId != windowId }.toPersistentList().also {
                closedLastWindow = it.isEmpty()
            }
        }
        if (closedLastWindow) {
            if (closeToTray.value) {
                setMinimized(true)
            } else {
                exit()
            }
        }
    }

    fun exit() {
        if (isShuttingDown.compareAndSet(false, true)) {
            shutdownScope.launch {
                log.i("Shutting down")
                ScCoroutines.shutdown()
                shutdownClients()
                withContext(Dispatchers.Main) {
                    applicationScope?.exitApplication()
                }
            }
        }
    }

    fun recreateUi() {
        _forceRecreationCounter.update { it + 1 }
        _trayIconRecreationCounter.update { it + 1 }
    }

    fun recreateTayIcon() {
        scope.launch {
            delay(1000)
            _trayIconRecreationCounter.update { it + 1 }
        }
    }

    fun recreateWindow(windowId: Int) {
        // Do with a slight delay - while immediately within one update() call would work too to recreate it,
        // I want to run this command to get broken window transparency to work, in which case doing both at the
        // same time doesn't work, as for some reason only new windows after already having one open are allowed
        // to get transparency in some scenarios? May be a window manager bug
        scope.launch {
            val newWindowId = windowCounter.fetchAndIncrement()
            var found =  false
            _windows.update {
                val window = it.find { it.windowId == windowId }
                if (window == null) {
                    found = true
                    it
                } else {
                    found = true
                    (it + window.copy(windowId = newWindowId)).toImmutableList()
                }
            }
            if (found) {
                delay(50)
                _windows.update { it.filter { it.windowId != windowId }.toImmutableList() }
            }
        }
    }

    fun setMinimized(minimized: Boolean) {
        _minimizedToTray.value = minimized
        if (!minimized) {
            // Ensure at least one window is open
            _windows.update {
                if (it.isEmpty()) {
                    persistentListOf(createWindow(getInboxDestination()))
                } else {
                    it
                }
            }
        }
    }

    fun disableSession(sessionId: SessionId) {
        disabledSessions.update { it + sessionId }
        appGraph.sessionCache.remove(sessionId)
    }

    private suspend fun shutdownClients() {
        val clients = matrixClients.value
        disabledSessions.update { it + currentValidSessionIds.value?.map(::SessionId).orEmpty() }
        appGraph.sessionCache.removeAll()
        clients.forEach { (sessionId, client) ->
            try {
                client.shutdownClient()
            } catch (e: Exception) {
                log.e("Failed to shutdown client $sessionId", e)
            }
        }
    }

    fun enableSession(sessionId: SessionId) {
        disabledSessions.update { it - sessionId }
    }

    fun postIncomingVerificationRequest(request: ScIncomingVerificationRequest): Boolean {
        return _globalMessageBoard.tryEmit(
            VerificationRequestAppMessage(request)
        )
    }
}
