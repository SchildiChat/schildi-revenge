package chat.schildi.matrixsdk

import kotlinx.coroutines.flow.StateFlow

interface ScSyncOrchestrationAppStateProvider {
    val isAppActive: StateFlow<Boolean>
    val isNetworkAvailable: StateFlow<Boolean>
}
