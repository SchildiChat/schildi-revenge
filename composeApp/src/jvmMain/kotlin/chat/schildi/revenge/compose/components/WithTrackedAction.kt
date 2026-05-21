package chat.schildi.revenge.compose.components

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import chat.schildi.revenge.model.PendingAction
import chat.schildi.revenge.model.PendingActionState
import chat.schildi.revenge.model.PendingGlobalActions

@Composable
fun WithTrackedAction(
    action: PendingAction,
    block: @Composable (enabled: Boolean) -> Unit,
) {
    val inProgress = action.isInProgress()
    if (inProgress) {
        TrackedActionInProgressSpinner()
    } else {
        block(action.isAllowed())
    }
}

@Composable
private fun TrackedActionInProgressSpinner(modifier: Modifier = Modifier) {
    CircularProgressIndicator(modifier)
}

@Composable
fun PendingAction.isInProgress() = PendingGlobalActions.follow(this).collectAsState(null).value == PendingActionState.InProgress

@Composable
fun PendingAction.isAllowed() = PendingGlobalActions.map { get ->
    get(this) !is PendingActionState.Pending && conflictingActions().none {
        get(it) is PendingActionState.Pending
    }
}.collectAsState(true).value
