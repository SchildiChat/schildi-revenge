package chat.schildi.revenge

import chat.schildi.resources.ComposableStringHolder
import chat.schildi.revenge.actions.ActionResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

expect val platformWindowManager: PlatformWindowManager

interface PlatformWindowManager {
    val windows: StateFlow<ImmutableList<WindowState>>
    val minimizedToTray: StateFlow<Boolean>
    fun openWindow(destination: Destination, initialTitle: ComposableStringHolder? = null)
    fun closeWindow(windowId: WindowId, closeUnlessLast: Boolean = false, onLastWindowClosed: () -> Unit): Boolean
    fun setMinimized(minimized: Boolean): ActionResult
    fun recreateWindow(scope: CoroutineScope, windowId: WindowId)
}
