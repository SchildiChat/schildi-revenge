package chat.schildi.revenge.glue

import chat.schildi.matrixsdk.ScSyncOrchestrationAppStateProvider
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
class DesktopSyncOrchestrationAppStatProvider : ScSyncOrchestrationAppStateProvider {
    override val isAppActive: StateFlow<Boolean> = MutableStateFlow(true)
    override val isNetworkAvailable: StateFlow<Boolean> = MutableStateFlow(true)
}
