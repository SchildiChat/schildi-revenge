package chat.schildi.revenge

import chat.schildi.resources.ComposableStringHolder
import chat.schildi.revenge.UiState.getInboxDestination
import chat.schildi.revenge.actions.ActionResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndIncrement
import kotlin.time.Duration.Companion.milliseconds

data class DesktopWindowState(
    override val destinationHolder: DestinationStateHolder,
    override val windowId: WindowId,
) : WindowState

@OptIn(ExperimentalAtomicApi::class)
actual val platformWindowManager = object : PlatformWindowManager {

    private val windowCounter = AtomicInt(0)
    private val _windows = MutableStateFlow<ImmutableList<DesktopWindowState>>(
        persistentListOf(
            createWindow(Destination.Splash),
        )
    )
    override val windows = _windows.asStateFlow()

    // Default to minimized - it's less disruptive to toggle true to false during init than other way round
    private val _minimizedToTray = MutableStateFlow(true)
    override val minimizedToTray = _minimizedToTray.asStateFlow()

    private fun createWindow(
        initialDestination: Destination,
        initialTitle: ComposableStringHolder? = null
    ): DesktopWindowState {
        return DesktopWindowState(
            windowId = windowCounter.fetchAndIncrement(),
            destinationHolder = DestinationStateHolder.forInitialDestination(initialDestination, initialTitle),
        )
    }

    override fun setMinimized(minimized: Boolean): ActionResult {
        var changed = false
        _minimizedToTray.update {
            changed = it != minimized
            minimized
        }
        if (!minimized) {
            // Ensure at least one window is open
            _windows.update {
                if (it.isEmpty()) {
                    changed = true
                    persistentListOf(createWindow(getInboxDestination()))
                } else {
                    it
                }
            }
        }
        return if (changed) {
            ActionResult.Success()
        } else {
            ActionResult.Inapplicable
        }
    }

    override fun openWindow(destination: Destination, initialTitle: ComposableStringHolder?) {
        val newWindow = createWindow(destination, initialTitle)
        val wasMinimized = UiState.minimizedToTray.value
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

    override fun closeWindow(
        windowId: WindowId,
        closeUnlessLast: Boolean,
        onLastWindowClosed: () -> Unit,
    ): Boolean {
        var closedLastWindow = false
        var closedAnyWindow = false
        _windows.update { windows ->
            if (closeUnlessLast && windows.size <= 1) {
                closedAnyWindow = false
                windows
            } else {
                windows.filter { it.windowId != windowId }.toPersistentList().also {
                    closedLastWindow = it.isEmpty()
                    closedAnyWindow = it.size != windows.size
                }
            }
        }
        if (closedLastWindow) {
            onLastWindowClosed()
        }
        return closedAnyWindow
    }

    override fun recreateWindow(scope: CoroutineScope, windowId: WindowId) {
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
                delay(50.milliseconds)
                _windows.update { it.filter { it.windowId != windowId }.toImmutableList() }
            }
        }
    }
}
