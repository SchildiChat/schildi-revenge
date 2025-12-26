package chat.schildi.revenge.compose.destination.split

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import chat.schildi.revenge.Destination
import chat.schildi.revenge.DestinationStateHolder
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.actions.ActionContext
import chat.schildi.revenge.actions.ActionResult
import chat.schildi.revenge.actions.HierarchicalKeyboardActionProvider
import chat.schildi.revenge.actions.KeyboardActionProvider
import chat.schildi.revenge.actions.execute
import chat.schildi.revenge.actions.hierarchicalKeyboardActionProvider
import chat.schildi.revenge.config.keybindings.Action
import chat.schildi.revenge.config.keybindings.KeyTrigger

class SplitKeyboardActionProvider(
    private val destinationStateHolder: DestinationStateHolder?,
    private val isPrimaryDestination: Boolean,
) : KeyboardActionProvider<Action.Split> {
    override fun getPossibleActions() = Action.Split.entries.toSet()
    override fun ensureActionType(action: Action) = action as? Action.Split

    override fun handleNavigationModeEvent(context: ActionContext, key: KeyTrigger): ActionResult {
        val keyConfig = context.keybindingConfig ?: return ActionResult.NoMatch
        return keyConfig.split.execute(context, key, ::handleAction)
    }

    override fun handleAction(
        context: ActionContext,
        action: Action.Split,
        args: List<String>
    ): ActionResult {
        return when (action) {
            Action.Split.Unsplit -> {
                val currentDestination = destinationStateHolder?.state?.value?.destination
                val newDestination = (currentDestination as? Destination.Split)?.let {
                    // Close the *current* destination -> navigate to the *other* destination
                    if (isPrimaryDestination)
                        it.secondary
                    else
                        it.primary

                }?.state?.value?.destination ?: return ActionResult.Inapplicable
                destinationStateHolder.navigate(newDestination)
                ActionResult.Success()
            }
        }
    }
}

@Composable
fun splitKeyboardActionProvider(isPrimaryDestination: Boolean): HierarchicalKeyboardActionProvider {
    val destinationStateHolder = LocalDestinationState.current
    return remember(destinationStateHolder,isPrimaryDestination) {
        SplitKeyboardActionProvider(destinationStateHolder, isPrimaryDestination)
    }.hierarchicalKeyboardActionProvider()
}
