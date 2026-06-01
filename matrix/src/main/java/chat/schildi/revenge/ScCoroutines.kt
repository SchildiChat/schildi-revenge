package chat.schildi.revenge

import io.element.android.libraries.core.coroutine.childScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withTimeoutOrNull

object ScCoroutines {
    private val appJob = SupervisorJob()

    private val appScope = CoroutineScope(appJob + Dispatchers.Default)

    fun scope(dispatcher: CoroutineDispatcher, name: String) = appScope.childScope(dispatcher, name)

    fun shutdown() {
        appScope.cancel("App shutdown")
    }

    suspend fun awaitShutdownFinished(timeoutMillis: Long): Boolean {
        return withTimeoutOrNull(timeoutMillis) {
            appJob.join()
            true
        } ?: false
    }
}
