package chat.schildi.revenge.glue

import android.Manifest
import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import androidx.annotation.RequiresPermission
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import chat.schildi.matrixsdk.ScSyncOrchestrationAppStateProvider
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object AndroidSyncOrchestrationAppStateProvider : ScSyncOrchestrationAppStateProvider {
    private lateinit var appLifecycle: Lifecycle

    private val appActive = MutableStateFlow(false)
    private val networkAvailable = MutableStateFlow(false)

    override val isAppActive: StateFlow<Boolean> = appActive
    override val isNetworkAvailable: StateFlow<Boolean> = networkAvailable

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    fun start(application: Application) {
        if (::appLifecycle.isInitialized) return

        appLifecycle = ProcessLifecycleOwner.get().lifecycle
        val connectivityManager = application.getSystemService(ConnectivityManager::class.java)
        appActive.value = isAppInForeground()

        appLifecycle.addObserver(LifecycleEventObserver { _, _ ->
            appActive.value = isAppInForeground()
        })
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            networkAvailable.value = true
        }

        override fun onLost(network: Network) {
            networkAvailable.value = false
        }
    }

    private fun isAppInForeground(): Boolean {
        return appLifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    }
}

@BindingContainer
@ContributesTo(AppScope::class)
object AndroidSyncOrchestrationModule {
    @Provides
    @SingleIn(AppScope::class)
    fun provideScSyncOrchestrationAppStatProvider(): ScSyncOrchestrationAppStateProvider =
        AndroidSyncOrchestrationAppStateProvider
}
