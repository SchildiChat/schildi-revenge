package chat.schildi.revenge.compose.destination.split

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import chat.schildi.revenge.actions.ActionContext
import chat.schildi.revenge.actions.ActionResult
import chat.schildi.revenge.actions.HierarchicalKeyboardActionProvider
import chat.schildi.revenge.actions.KeyboardActionProvider
import chat.schildi.revenge.actions.execute
import chat.schildi.revenge.actions.hierarchicalKeyboardActionProvider
import chat.schildi.revenge.actions.orActionValidationError
import chat.schildi.revenge.actions.toDestinationEnum
import chat.schildi.revenge.config.keybindings.Action
import chat.schildi.revenge.config.keybindings.KeyTrigger

class MultiPaneKeyboardActionProvider(
    private val destinationStateHolder: MultiPaneLayoutDestinationStateHolderWrapper,
) : KeyboardActionProvider<Action.Split> {
    override fun getPossibleActions() = setOf(
        Action.Split.Unsplit,
        Action.Split.UnsplitDestination,
    )
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
            Action.Split.Unsplit -> destinationStateHolder.closeSplit()
            Action.Split.UnsplitDestination -> {
                val destinationType = args.firstOrNull()?.toDestinationEnum().orActionValidationError()
                destinationStateHolder.closeDestination(destinationType)
            }
            else -> ActionResult.Inapplicable
        }
    }
}

@Composable
fun multiPaneKeyboardActionProvider(destinationStateHolder: MultiPaneLayoutDestinationStateHolderWrapper): HierarchicalKeyboardActionProvider {
    return remember(destinationStateHolder) {
        MultiPaneKeyboardActionProvider(destinationStateHolder)
    }.hierarchicalKeyboardActionProvider()
}
