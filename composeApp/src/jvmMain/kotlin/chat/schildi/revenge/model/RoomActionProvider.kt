package chat.schildi.revenge.model

import chat.schildi.revenge.GlobalActionsScope
import chat.schildi.revenge.actions.ActionContext
import chat.schildi.revenge.actions.ActionResult
import chat.schildi.revenge.actions.KeyboardActionProvider
import chat.schildi.revenge.actions.execute
import chat.schildi.revenge.actions.launchActionAsync
import chat.schildi.revenge.actions.toActionResult
import chat.schildi.revenge.compose.util.StringResourceHolder
import chat.schildi.revenge.compose.util.toStringHolder
import chat.schildi.revenge.config.keybindings.Action
import chat.schildi.revenge.config.keybindings.KeyTrigger
import io.element.android.libraries.matrix.api.room.BaseRoom
import io.element.android.libraries.matrix.api.timeline.ReceiptType
import kotlinx.coroutines.Dispatchers
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.action_leave
import shire.composeapp.generated.resources.action_leave_room_prompt
import shire.composeapp.generated.resources.action_leave_unnamed_room_prompt
import shire.composeapp.generated.resources.command_copy_name_event_id

private val RoomInviteActions = setOf(Action.Room.Join)

class RoomActionProvider(
    val isInvite: Boolean,
    val getRoom: suspend () -> BaseRoom?,
) : KeyboardActionProvider<Action.Room> {
    override fun getPossibleActions() = if (isInvite)
        RoomInviteActions
    else
        Action.Room.entries.toSet() - RoomInviteActions

    override fun ensureActionType(action: Action) = action as? Action.Room

    override fun handleNavigationModeEvent(
        context: ActionContext,
        key: KeyTrigger
    ): ActionResult {
        val keyConfig = context.keybindingConfig ?: return ActionResult.NoMatch
        return keyConfig.room.execute(context, key, ::handleAction)
    }

    override fun handleAction(
        context: ActionContext,
        action: Action.Room,
        args: List<String>
    ): ActionResult {
        return context.launchActionAsync(
            actionName = action.name,
            scope = GlobalActionsScope,
            context = Dispatchers.IO,
        ) {
            val room = getRoom() ?: return@launchActionAsync ActionResult.Failure("Room not ready")
            handleActionWithRoom(context, action, args, room)
        }
    }

    private suspend fun handleActionWithRoom(
        context: ActionContext,
        action: Action.Room,
        args: List<String>,
        room: BaseRoom,
    ): ActionResult {
        return when (action) {
            Action.Room.MarkFavorite -> {
                val value = args.firstOrNull()?.toBooleanStrictOrNull() ?: true
                room.setIsFavorite(value).toActionResult(async = true)
            }
            Action.Room.MarkLowPriority -> {
                val value = args.firstOrNull()?.toBooleanStrictOrNull() ?: true
                room.setIsLowPriority(value).toActionResult(async = true)
            }
            Action.Room.ToggleIsFavorite -> {
                val value = !room.info().isFavorite
                room.setIsFavorite(value).toActionResult(async = true)
            }
            Action.Room.ToggleIsLowPriority -> {
                val value = !room.info().isLowPriority
                room.setIsLowPriority(value).toActionResult(async = true)
            }
            Action.Room.MarkRoomUnread -> {
                val value = args.firstOrNull()?.toBooleanStrictOrNull() ?: true
                room.setUnreadFlag(value).toActionResult(async = true)
            }
            Action.Room.ClearUnreadFlag -> {
                room.setUnreadFlag(false).toActionResult(async = true)
            }
            Action.Room.MarkRoomRead -> {
                room.markAsRead(ReceiptType.READ).toActionResult(async = true)
            }
            Action.Room.MarkRoomReadPrivate -> {
                room.markAsRead(ReceiptType.READ_PRIVATE).toActionResult(async = true)
            }
            Action.Room.MarkRoomFullyRead -> {
                room.markAsRead(ReceiptType.FULLY_READ).toActionResult(async = true)
            }
            Action.Room.Join -> {
                room.join().toActionResult(async = true)
            }
            Action.Room.Leave -> context.withCriticalActionConfirmationSuspend(
                prompt = room.info().name?.let {
                    StringResourceHolder(Res.string.action_leave_room_prompt, it.toStringHolder())
                } ?: Res.string.action_leave_unnamed_room_prompt.toStringHolder(),
                confirmText = Res.string.action_leave.toStringHolder(),
                scope = GlobalActionsScope,
                coroutineContext = Dispatchers.IO,
                actionName = action.name,
            ) {
               room.leave().toActionResult(async = true)
            }
            Action.Room.CopyRoomId -> {
                context.copyToClipboard(
                    room.roomId.value,
                    Res.string.command_copy_name_event_id.toStringHolder()
                )
            }
        }
    }
}
