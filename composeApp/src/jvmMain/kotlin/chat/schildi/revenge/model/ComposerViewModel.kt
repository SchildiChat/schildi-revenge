package chat.schildi.revenge.model

import chat.schildi.revenge.actions.ActionResult
import kotlinx.coroutines.flow.StateFlow

// TODO also expose can-send-message permissions
interface ComposerViewModel {
    val composerState: StateFlow<DraftValue>
    fun onComposerUpdate(value: DraftValue)
    fun sendMessage(): ActionResult
}
