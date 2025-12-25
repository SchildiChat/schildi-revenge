package chat.schildi.revenge.actions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import chat.schildi.revenge.config.keybindings.Action
import chat.schildi.revenge.config.keybindings.KeyTrigger

val LocalKeyboardActionProvider = compositionLocalOf<HierarchicalKeyboardActionProvider?> { null }

@Composable
fun KeyboardActionProvider<*>.hierarchicalKeyboardActionProvider(): HierarchicalKeyboardActionProvider {
    val instance = this
    val parent = LocalKeyboardActionProvider.current
    return remember(instance, parent) { HierarchicalKeyboardActionProvider(instance, parent) }
}

interface KeyboardActionProvider<A : Action> {
    /** Possible actions to suggest in auto-completions while typing. */
    fun getPossibleActions(): Set<A>
    /** In case of action providers only covering a specific subset of possible actions, typecast if this action is contained or return null? */
    fun ensureActionType(action: Action): A?
    /** Handle key events while in navigation mode. */
    fun handleNavigationModeEvent(context: ActionContext, key: KeyTrigger): ActionResult
    /** Handle specific actions from command mode or UI elements. */
    fun handleAction(context: ActionContext, action: A, args: List<String>): ActionResult
    /** Util function to call [handleAction] if [ensureActionType] is not null, or return [ActionResult.Inapplicable] otherwise. */
    fun handleActionOrInapplicable(context: ActionContext, action: Action, args: List<String>): ActionResult {
        val safeAction = ensureActionType(action)
        return if (safeAction == null) {
            ActionResult.Inapplicable
        } else {
            handleAction(context, safeAction, args)
        }
    }
}

data class HierarchicalKeyboardActionProvider(
    val instance: KeyboardActionProvider<*>,
    val parent: HierarchicalKeyboardActionProvider?,
) : KeyboardActionProvider<Action> {
    override fun ensureActionType(action: Action) = action
    override fun getPossibleActions(): Set<Action> {
        return instance.getPossibleActions() + parent?.getPossibleActions().orEmpty()
    }
    override fun handleNavigationModeEvent(context: ActionContext, key: KeyTrigger): ActionResult {
        return ActionResult.chain(
            { instance.handleNavigationModeEvent(context, key) },
            { parent?.handleNavigationModeEvent(context, key) ?: ActionResult.NoMatch }
        )
    }
    override fun handleAction(
        context: ActionContext,
        action: Action,
        args: List<String,>
    ): ActionResult {
        return ActionResult.chain(
            { instance.handleActionOrInapplicable(context, action, args) },
            { parent?.handleActionOrInapplicable(context, action, args) ?: ActionResult.Inapplicable }
        )
    }
}
