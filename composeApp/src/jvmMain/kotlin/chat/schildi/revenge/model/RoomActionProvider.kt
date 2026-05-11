package chat.schildi.revenge.model

import chat.schildi.matrixsdk.ROOM_ACCOUNT_DATA_PERSONAL_ROOM_NAME
import chat.schildi.matrixsdk.RoomNamePrivateContent
import chat.schildi.revenge.GlobalActionsScope
import chat.schildi.revenge.actions.ActionContext
import chat.schildi.revenge.actions.ActionResult
import chat.schildi.revenge.actions.formatEventContentDump
import chat.schildi.revenge.actions.KeyboardActionProvider
import chat.schildi.revenge.actions.execute
import chat.schildi.revenge.actions.launchActionAsync
import chat.schildi.revenge.actions.orActionValidationError
import chat.schildi.revenge.actions.runWithMessage
import chat.schildi.revenge.actions.toActionResult
import chat.schildi.revenge.compose.util.StringResourceHolder
import chat.schildi.revenge.compose.util.toStringHolder
import chat.schildi.revenge.config.keybindings.Action
import chat.schildi.revenge.config.keybindings.ActionArgumentPrimitive
import chat.schildi.revenge.config.keybindings.ActionRoomNotificationSetting
import chat.schildi.revenge.config.keybindings.KeyTrigger
import chat.schildi.revenge.toPrettyJson
import chat.schildi.revenge.util.matrix.updateAccountData
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.room.BaseRoom
import io.element.android.libraries.matrix.api.room.JoinedRoom
import io.element.android.libraries.matrix.api.room.RoomMember
import io.element.android.libraries.matrix.api.room.RoomNotificationMode
import io.element.android.libraries.matrix.api.room.powerlevels.UserRoleChange
import io.element.android.libraries.matrix.api.timeline.ReceiptType
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.action_leave
import shire.composeapp.generated.resources.action_leave_room_prompt
import shire.composeapp.generated.resources.action_leave_unnamed_room_prompt
import shire.composeapp.generated.resources.command_copy_name_full_account_data
import shire.composeapp.generated.resources.toast_added_to_space
import shire.composeapp.generated.resources.toast_adding_to_space
import shire.composeapp.generated.resources.toast_removed_from_space
import shire.composeapp.generated.resources.toast_removing_from_space
import kotlin.collections.map
import kotlin.collections.orEmpty

private val RoomInviteActions = setOf(Action.Room.Join)

class RoomActionProvider(
    val sessionId: SessionId,
    val roomId: RoomId,
    val isInvite: Boolean,
    val peekClient: suspend () -> MatrixClient?,
    val peekRoom: (suspend () -> BaseRoom?)?,
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
            if (peekRoom == null) {
                val client = peekClient() ?: return@launchActionAsync ActionResult.Failure("Client not ready")
                val room = client.getJoinedRoom(roomId) ?: client.getRoom(roomId) ?: return@launchActionAsync ActionResult.Failure("Room not ready")
                room.use {
                    handleActionWithRoom(context, action, args, room)
                }
            } else {
                val room = peekRoom() ?: return@launchActionAsync ActionResult.Failure("Room not ready")
                handleActionWithRoom(context, action, args, room)
            }
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
                context.copyToClipboard(room.roomId.value)
            }
            Action.Room.ClearEventCache -> {
                room.clearEventCacheStorage().toActionResult()
            }
            Action.Room.SetRoomUserDisplayName -> {
                val name = args.firstOrNull()
                room.setRoomUserDisplayName(name).toActionResult()
            }
            Action.Room.SetRoomNotifications -> {
                val client = peekClient() ?: return ActionResult.Failure("Client not ready")
                val modeString = args.firstOrNull().orActionValidationError()
                val actionMode = ActionRoomNotificationSetting.tryResolve(modeString).orActionValidationError()
                val mode = actionMode.toNotificationMode()
                if (mode == null) {
                    client.notificationSettingsService.restoreDefaultRoomNotificationMode(room.roomId).toActionResult()
                } else {
                    client.notificationSettingsService.setRoomNotificationMode(room.roomId, mode).toActionResult()
                }
            }
            Action.Room.AddToSpace -> {
                val spaceId = args.firstOrNull()?.let { RoomId(it) }.orActionValidationError()
                val client = peekClient() ?: return ActionResult.Failure("Client not ready")
                val space = client.getRoom(spaceId) ?: return ActionResult.Failure("Space not found")
                space.use {
                    addRoomToSpace(context, space, room)
                }
            }
            Action.Room.RemoveFromSpace -> {
                val spaceId = args.firstOrNull()?.let { RoomId(it) }.orActionValidationError()
                val client = peekClient() ?: return ActionResult.Failure("Client not ready")
                val space = client.getRoom(spaceId) ?: return ActionResult.Failure("Space not found")
                space.use {
                    removeRoomFromSpace(context, space, room)
                }
            }
            Action.Room.ToggleRoomInSpace -> {
                val spaceId = args.firstOrNull()?.let { RoomId(it) }.orActionValidationError()
                val client = peekClient() ?: return ActionResult.Failure("Client not ready")
                val space = client.getRoom(spaceId) ?: return ActionResult.Failure("Space not found")
                space.use {
                    if (space.info().spaceChildren.any { it.roomId == room.roomId.value }) {
                        removeRoomFromSpace(context, space, room)
                    } else {
                        addRoomToSpace(context, space, room)
                    }
                }
            }
            Action.Room.SetRoomName -> {
                val joinedRoom = (room as? JoinedRoom) ?: return ActionResult.Inapplicable
                val name = args.firstOrNull() ?: ""
                joinedRoom.setName(name).toActionResult()
            }
            Action.Room.SetPrivateRoomName -> {
                val name = args.firstOrNull() ?: ""
                val client = peekClient() ?: return ActionResult.Failure("Client not ready")
                val content = if (name.isBlank()) {
                    "{}"
                } else {
                    Json.encodeToString(RoomNamePrivateContent(name))
                }
                client.setRoomAccountData(
                    roomId,
                    ROOM_ACCOUNT_DATA_PERSONAL_ROOM_NAME,
                    content,
                ).toActionResult()
            }
            Action.Room.SetRoomTopic -> {
                val joinedRoom = (room as? JoinedRoom) ?: return ActionResult.Inapplicable
                val topic = args.firstOrNull() ?: ""
                joinedRoom.setTopic(topic).toActionResult()
            }
            Action.Room.SetRoomAvatar -> {
                val joinedRoom = (room as? JoinedRoom) ?: return ActionResult.Inapplicable
                val avatarUrl = args.firstOrNull()
                if (avatarUrl == null) {
                    joinedRoom.removeAvatar().toActionResult()
                } else {
                    joinedRoom.setAvatarUrl(avatarUrl).toActionResult()
                }
            }
            Action.Room.CopyFullRoomAccountData,
            Action.Room.ViewFullRoomAccountData -> {
                val client = peekClient() ?: return ActionResult.Failure("Client not ready")
                val result = client.getRoomAccountData(room.roomId)
                if (result.isSuccess) {
                    val joined = result.getOrNull().formatEventContentDump({ it.eventType }, { it.content.toPrettyJson() })
                    if (action == Action.Room.CopyFullRoomAccountData) {
                        context.copyToClipboard(
                            joined,
                            Res.string.command_copy_name_full_account_data.toStringHolder()
                        )
                    } else {
                        context.viewInExternalApp(joined, ".md")
                    }
                } else {
                    result.toActionResult(async = true)
                }
            }
            Action.Room.ConvertToDm -> {
                room.convertToDm()
            }
            Action.Room.ConvertToGroup -> {
                room.convertToGroup()
            }
            Action.Room.SetPowerLevel -> {
                if (room !is JoinedRoom) {
                    return ActionResult.Inapplicable
                }
                val userId = UserId(args.firstOrNull().orActionValidationError())
                val powerLevel = args.getOrNull(1)?.toLongOrNull().orActionValidationError()
                room.updateUsersRoles(
                    listOf(UserRoleChange(
                        userId,
                        RoomMember.Role.forPowerLevel(powerLevel),
                    ))
                ).toActionResult()
            }
        }
    }

    private suspend fun BaseRoom.convertToDm(): ActionResult {
        val members = getMembers()
        if (members.isFailure) {
            return members.toActionResult()
        }
        val dmContact = members.getOrNull()?.filter { it.userId != sessionId }?.takeIf { it.size == 1 }?.firstOrNull()
        if (dmContact == null) {
            return ActionResult.Failure("Found $dmContact DM member candidates")
        }
        return updateGlobalAccountData("m.direct") {
            it.orEmpty().toMutableMap().apply {
                this[dmContact.userId.value] = JsonArray(
                    (
                            this[dmContact.userId.value]?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty()
                                    + roomId.value
                            ).map(::JsonPrimitive)
                )
            }.let(::JsonObject)
        }
    }

    private suspend fun BaseRoom.convertToGroup(): ActionResult {
        return updateGlobalAccountData("m.direct") {
            it?.toMutableMap()?.apply {
                this.forEach { (key, element) ->
                    if (element.jsonArray.any { it.jsonPrimitive.content == roomId.value }) {
                        this[key]?.jsonArray?.filter { it.jsonPrimitive.content != roomId.value }?.let { content ->
                            this[key] = JsonArray(content)
                        }
                    }
                }
            }?.let(::JsonObject)
        }
    }

    private suspend fun updateGlobalAccountData(eventType: String, update: (JsonObject?) -> JsonObject?): ActionResult {
        val client = peekClient() ?: return ActionResult.Failure("Client not ready")
        return client.updateAccountData(eventType, update)
    }

    private suspend fun addRoomToSpace(context: ActionContext, space: BaseRoom, room: BaseRoom): ActionResult {
        val spaceName = (space.info().name ?: space.roomId.value).toStringHolder()
        val roomName = (room.info().name ?: room.roomId.value).toStringHolder()
        return context.runWithMessage(
            messageId = "addRoomToSpace/${space.roomId}/${room.roomId}",
            start = StringResourceHolder(Res.string.toast_adding_to_space, roomName, spaceName),
            end = {
                if (it is ActionResult.Success) {
                    StringResourceHolder(Res.string.toast_added_to_space, roomName, spaceName)
                } else {
                    (it as? ActionResult.Failure)?.message?.toStringHolder()
                        ?: it.toString().toStringHolder()
                }
            }
        ) {
            space.addSpaceChild(room.roomId).toActionResult()
        }
    }

    private suspend fun removeRoomFromSpace(context: ActionContext, space: BaseRoom, room: BaseRoom): ActionResult {
        val spaceName = (space.info().name ?: space.roomId.value).toStringHolder()
        val roomName = (room.info().name ?: room.roomId.value).toStringHolder()
        return context.runWithMessage(
            messageId = "removeRoomFromSpace/${space.roomId}/${room.roomId}",
            start = StringResourceHolder(Res.string.toast_removing_from_space, roomName, spaceName),
            end = {
                if (it is ActionResult.Success) {
                    StringResourceHolder(Res.string.toast_removed_from_space, roomName, spaceName)
                } else {
                    (it as? ActionResult.Failure)?.message?.toStringHolder()
                        ?: it.toString().toStringHolder()
                }
            }
        ) {
            space.removeSpaceChild(room.roomId).toActionResult()
        }
    }

    override fun impliedArguments(): List<Pair<ActionArgumentPrimitive, String>> = listOf(
        ActionArgumentPrimitive.SessionId to sessionId.value,
        ActionArgumentPrimitive.RoomId to roomId.value,
    )
}

fun ActionRoomNotificationSetting.toNotificationMode(): RoomNotificationMode? {
    return when (this) {
        ActionRoomNotificationSetting.Default -> null
        ActionRoomNotificationSetting.All -> RoomNotificationMode.ALL_MESSAGES
        ActionRoomNotificationSetting.Mentions -> RoomNotificationMode.MENTIONS_AND_KEYWORDS_ONLY
        ActionRoomNotificationSetting.Mute -> RoomNotificationMode.MUTE
    }
}
