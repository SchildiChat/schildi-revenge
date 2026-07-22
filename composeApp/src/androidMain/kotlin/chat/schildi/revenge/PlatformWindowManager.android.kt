package chat.schildi.revenge

import android.content.Intent
import chat.schildi.resources.ComposableStringHolder
import chat.schildi.revenge.actions.ActionResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentHashMap
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.lang.ref.WeakReference

data class AndroidWindowState(
    override val destinationHolder: DestinationStateHolder,
    override val windowId: WindowId,
    val activity: WeakReference<MainActivity>?,
) : WindowState

val androidWindowManager = AndroidWindowManager()
actual val platformWindowManager: PlatformWindowManager = androidWindowManager

class AndroidWindowManager : PlatformWindowManager {
    private val scope = CoroutineScope(Dispatchers.Default)

    override val appOwnsWindows = false

    private val androidWindows =
        MutableStateFlow<ImmutableMap<WindowId, AndroidWindowState>>(persistentMapOf())
    var currentActivity: WeakReference<MainActivity>? = null
        private set

    override val windows: StateFlow<ImmutableList<WindowState>> = androidWindows
        .map { it.values.toPersistentList() }
        .stateIn(scope, SharingStarted.Eagerly, persistentListOf())

    override val minimizedToTray = MutableStateFlow(false).asStateFlow()

    override fun openWindow(
        destination: Destination,
        preferNewTask: Boolean,
        initialTitle: ComposableStringHolder?
    ) {
        val currentActivity = androidWindowManager.currentActivity?.get()
        val context = currentActivity ?: RevengeApplication.instance
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_DESTINATION, destination.serializedToString())
        if (preferNewTask || currentActivity == null) {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK))
        } else {
            context.startActivity(intent)
        }
    }

    override fun closeWindow(
        windowId: WindowId,
        closeUnlessLast: Boolean,
        onLastWindowClosed: () -> Unit
    ): Boolean {
        return androidWindows.value[windowId]?.activity?.get()?.let {
            it.finish()
            true
        } ?: false
    }

    override fun setMinimized(minimized: Boolean): ActionResult {
        return if (minimized) {
            androidWindows.value.forEach { it.value.activity?.get()?.moveTaskToBack(true) }
            ActionResult.Success()
        } else {
            ActionResult.Inapplicable
        }
    }

    override fun recreateWindow(scope: CoroutineScope, windowId: WindowId) {}

    fun register(windowId: Int, initialDestination: Destination?, activity: MainActivity): DestinationStateHolder {
        val destinationHolder =
            DestinationStateHolder.forInitialDestination(initialDestination ?: UiState.getInboxDestination())
        androidWindows.update {
            (
                it + (windowId to AndroidWindowState(
                        windowId = windowId,
                        destinationHolder = destinationHolder,
                        activity = WeakReference(activity),
                    )
                )
            ).toPersistentHashMap()
        }
        return destinationHolder
    }

    fun unregister(windowId: Int) {
        androidWindows.update {
            (it - windowId).toPersistentHashMap()
        }
    }

    fun onResume(activity: MainActivity) {
        currentActivity = WeakReference(activity)
    }

    fun onPause(activity: MainActivity) {
        if (currentActivity?.get() == activity) {
            currentActivity = null
        }
    }
}
