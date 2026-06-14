package chat.schildi.revenge.actions

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi

interface ActionResultCallback {
    suspend fun onActionResult(result: ActionResult)
}

class DeferredActionResultCallback : ActionResultCallback {
    val actionResult = CompletableDeferred<ActionResult>()
    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun onActionResult(result: ActionResult) {
        if (!actionResult.complete(result)) {
            Logger.withTag("DeferredActionResult").e("Completed twice (${actionResult.getCompleted()}, $result")
        }
    }
}
