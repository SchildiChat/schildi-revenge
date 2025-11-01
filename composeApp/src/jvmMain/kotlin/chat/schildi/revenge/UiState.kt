package chat.schildi.revenge

import androidx.compose.ui.window.ApplicationScope
import chat.schildi.revenge.navigation.AccountManagementDestination
import chat.schildi.revenge.navigation.Destination
import chat.schildi.revenge.navigation.InboxDestination
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndIncrement

@OptIn(ExperimentalAtomicApi::class)
object UiState {
    private val windowCounter = AtomicInt(0)
    private val _windows = MutableStateFlow<ImmutableList<WindowState>>(
        // TODO splash screen
        persistentListOf(
            //createWindow(AccountManagementDestination),
            createWindow(InboxDestination),
        )
    )
    val windows = _windows.asStateFlow()

    private fun createWindow(initialDestination: Destination): WindowState {
        return WindowState(
            windowId = windowCounter.fetchAndIncrement(),
            destination = DestinationState(MutableStateFlow(initialDestination)),
        )
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

data class WindowState(
    val destination: DestinationState,
    val windowId: Int,
)

data class DestinationState(
    val state: MutableStateFlow<Destination>,
)
