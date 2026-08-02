package chat.schildi.revenge.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
internal class StartupCacheCleaner(
    private val clear: () -> Unit,
) {
    private val cleanupJob = AtomicReference<Job?>(null)

    fun clearAsync(scope: CoroutineScope) {
        val job = scope.launch(start = CoroutineStart.LAZY) {
            clear()
        }
        check(cleanupJob.compareAndSet(null, job)) { "Cache cleanup already scheduled" }
        job.start()
    }

    suspend fun awaitClear() {
        cleanupJob.load()?.join()
    }
}
