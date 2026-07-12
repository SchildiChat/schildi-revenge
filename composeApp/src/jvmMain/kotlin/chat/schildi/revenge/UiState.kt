package chat.schildi.revenge

import chat.schildi.matrixsdk.StaticRevengeSdkConfig
import chat.schildi.revenge.preferences.RevengePrefs
import chat.schildi.lib.preferences.ScPrefs
import chat.schildi.revenge.actions.AbstractAppMessage
import chat.schildi.revenge.actions.AppMessage
import chat.schildi.revenge.actions.KeyboardActionHandler
import chat.schildi.revenge.actions.VerificationRequestAppMessage
import chat.schildi.resources.ComposableStringHolder
import chat.schildi.resources.StringResourceHolder
import chat.schildi.resources.toStringHolder
import chat.schildi.revenge.config.ConfigWatchers
import chat.schildi.revenge.model.LoadCheckPoint
import chat.schildi.revenge.model.LoadStateHolder
import chat.schildi.revenge.model.RevengeRoomListDataSource
import chat.schildi.revenge.model.verification.ScIncomingVerificationRequest
import chat.schildi.revenge.model.asCheckpointLoadedOrPending
import chat.schildi.revenge.store.AppStateStore
import chat.schildi.revenge.util.throttleLatest
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.createGraphFactory
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.x.di.AppGraph
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentHashMap
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import shire.res.generated.resources.Res
import shire.res.generated.resources.toast_key_config_reload_error
import shire.res.generated.resources.toast_key_config_reload_success
import java.util.Locale
import kotlin.collections.map
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.system.exitProcess

val GlobalActionsScope = ScCoroutines.scope(Dispatchers.IO, "GlobalActionScope")

private const val MESSAGE_ID_KEY_CONFIG = "keyConfig"

// For KeyboardActionHandler via headless IPC
const val HEADLESS_WINDOW_ID = -1

@OptIn(ExperimentalAtomicApi::class)
object UiState {
    private val log = Logger.withTag("UiState")
    private val scope = ScCoroutines.scope(Dispatchers.IO, "UiState")
    private val shutdownScope = CoroutineScope(Dispatchers.Default)
    private val isShuttingDown = AtomicBoolean(false)
    @Suppress("ConstantLocale")
    private val initialLocale = Locale.getDefault()

    val appGraph: AppGraph = createGraphFactory<AppGraph.Factory>().create()

    private var exitApplication: (() -> Unit)? = null
    var headlessKeyboardActionHandler: KeyboardActionHandler? = null
        private set

    private var hasClearedSplashScreen = false
    val minimizedToTray = platformWindowManager.minimizedToTray

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

    private val vsyncEnabled = RevengePrefs
        .settingFlow(ScPrefs.SKIKO_VSYNC)
        .distinctUntilChanged()
        .onEach {
            log.d { "Setting vsync=$it" }
            System.setProperty("skiko.vsync.enabled", it.toString())
        }
        .stateIn(scope, SharingStarted.Eagerly, null)

    private val sdkSqlitePoolSize = RevengePrefs
        .settingFlow(ScPrefs.SDK_SQLITE_MAX_POOL_SIZE)
        .distinctUntilChanged()
        .onEach {
            val safeValue = ScPrefs.SDK_SQLITE_MAX_POOL_SIZE.coerceValue(it).toUInt()
            log.d { "Setting SDK sqlite max pool size=$safeValue" }
            StaticRevengeSdkConfig.sqlitePoolLimit = safeValue
        }
        .stateIn(scope, SharingStarted.Eagerly, null)


    private val preferMultiPaneInbox = RevengePrefs
        .settingFlow(ScPrefs.PREFER_DUAL_PANE_INBOX)
        .distinctUntilChanged()
        .onEach { preferMultiPane ->
            // Recreate any potential inbox destinations to apply the new setting
            platformWindowManager.windows.value.forEach { window ->
                val destination = window.destinationHolder.state.value.destination
                if (destination.category == DestinationCategory.INBOX) {
                    val newDestination = getInboxDestination(preferMultiPane)
                    if (newDestination.destinationId != destination.destinationId) {
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

    val hasInboxOpen = platformWindowManager.windows.flatMerge(
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
        appGraph.sessionStore.sessionsFlow(),
    ) { disabled, allSessions ->
        // Need to block on client config being ready first
        val initialDbPoolSize = sdkSqlitePoolSize.first { it != null}
        val persistedSessions = allSessions.filter { SessionId(it.userId) !in disabled }
        globalLoadState.addExpected(
            *persistedSessions.map { LoadCheckPoint.Client(SessionId(it.userId)) }.toTypedArray()
        )
        val startTs = System.currentTimeMillis()
        log.i("Restoring ${persistedSessions.size} sessions; per-account SDK DB max pool size: $initialDbPoolSize")
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
            "" -> initialLocale
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

    fun initializeWith(exitApplication: () -> Unit, startInTray: Boolean) {
        if (this.exitApplication != null || this.headlessKeyboardActionHandler != null) {
            throw IllegalStateException("Initializing UiState twice")
        }

        // Initialize the desktop lifecycle callback and headless keyboard handler
        this.exitApplication = exitApplication
        headlessKeyboardActionHandler = KeyboardActionHandler(GlobalActionsScope, HEADLESS_WINDOW_ID)
        platformWindowManager.setMinimized(startInTray)

        // Block until vsync is set up, since we need to set it before opening windows
        val initialVsync = runBlocking {
            vsyncEnabled.first { it != null }
        }
        log.i { "Starting with vsync=$initialVsync" }
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
        platformWindowManager.windows.value.forEach { window ->
            if (window.destinationHolder.state.value.destination is Destination.Splash) {
                window.destinationHolder.navigate(destination, NavigationPreference.REPLACE)
            }
        }
    }

    fun openWindow(destination: Destination, initialTitle: ComposableStringHolder? = null) {
        val effectiveDestination = if (destination is Destination.Conversation && preferMultiPaneConversation.value) {
            Destination.ConversationDetailsMultiPane(destination)
        } else {
            destination
        }
        platformWindowManager.openWindow(effectiveDestination, initialTitle)
    }

    fun closeWindow(windowId: WindowId, closeUnlessLast: Boolean = false): Boolean {
        return platformWindowManager.closeWindow(
            windowId = windowId,
            closeUnlessLast = closeUnlessLast,
        ) {
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
                log.i("Shutting down coroutines")
                ScCoroutines.shutdown()
                log.i("Shutting down clients")
                shutdownClients()
                log.i("Shutting down application")
                exitApplication?.let {
                    withContext(Dispatchers.Main) { it() }
                } ?: run {
                    log.e("Compose application not tracked for shutdown")
                }
                log.i("Waiting for coroutines to finish")
                if (!ScCoroutines.awaitShutdownFinished(30_000L)) {
                    log.w("Timed out waiting for coroutines to finish")
                }
                log.i("Shutdown finished")
                exitProcess(0)
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
        platformWindowManager.recreateWindow(scope, windowId)
    }

    fun setMinimized(minimized: Boolean) = platformWindowManager.setMinimized(minimized)

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
