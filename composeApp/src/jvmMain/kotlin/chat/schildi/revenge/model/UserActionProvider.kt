package chat.schildi.revenge.model

import chat.schildi.revenge.actions.ActionContext
import chat.schildi.revenge.actions.ActionResult
import chat.schildi.revenge.actions.KeyboardActionProvider
import chat.schildi.revenge.actions.execute
import chat.schildi.revenge.compose.util.toStringHolder
import chat.schildi.revenge.config.keybindings.Action
import chat.schildi.revenge.config.keybindings.ActionArgumentPrimitive
import chat.schildi.revenge.config.keybindings.KeyTrigger
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.core.UserId
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.command_copy_name_mxid

class UserActionProvider(
    val sessionId: SessionId,
    val userId: UserId,
    val roomId: RoomId?,
) : KeyboardActionProvider<Action.User> {
    override fun getPossibleActions() = Action.User.entries.toSet()
    override fun ensureActionType(action: Action) = action as? Action.User

    override fun handleNavigationModeEvent(
        context: ActionContext,
        key: KeyTrigger
    ): ActionResult {
        val keyConfig = context.keybindingConfig ?: return ActionResult.NoMatch
        return keyConfig.user.execute(context, key, ::handleAction)
    }

    override fun handleAction(
        context: ActionContext,
        action: Action.User,
        args: List<String>,
    ): ActionResult {
        return when (action) {
            Action.User.CopyMxId -> {
                context.copyToClipboard(userId.value, Res.string.command_copy_name_mxid.toStringHolder())
            }
        }
    }

    override fun impliedArguments(): List<Pair<ActionArgumentPrimitive, String>> = listOfNotNull(
        ActionArgumentPrimitive.SessionId to sessionId.value,
        ActionArgumentPrimitive.UserId to userId.value,
        roomId?.value?.let { ActionArgumentPrimitive.RoomId to it },
    )
}
