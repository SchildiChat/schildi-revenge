package chat.schildi.revenge.model

import chat.schildi.matrixsdk.ROOM_ACCOUNT_DATA_SPACE_ORDER
import chat.schildi.revenge.GlobalActionsScope
import chat.schildi.revenge.actions.ActionContext
import chat.schildi.revenge.actions.ActionResult
import chat.schildi.revenge.actions.KeyboardActionProvider
import chat.schildi.revenge.actions.execute
import chat.schildi.revenge.actions.launchActionAsync
import chat.schildi.revenge.actions.orActionValidationError
import chat.schildi.revenge.config.keybindings.Action
import chat.schildi.revenge.config.keybindings.KeyTrigger
import chat.schildi.revenge.config.keybindings.SpaceCatchAllInviteMode
import chat.schildi.revenge.config.keybindings.SpaceCatchAllMode
import chat.schildi.revenge.model.spaces.SpaceListDataSource
import chat.schildi.revenge.model.spaces.SpaceOrder
import chat.schildi.revenge.util.matrix.updateRoomAccountData
import chat.schildi.revenge.util.matrix.updateRoomState
import chat.schildi.revenge.util.tryOrNull
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.room.JoinedRoom
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class SpaceActionProvider(
    val space: SpaceListDataSource.SpaceHierarchyItem,
    val peekClient: suspend (SessionId) -> MatrixClient?,
) : KeyboardActionProvider<Action.Space> {
    override fun getPossibleActions() = Action.Space.entries.toSet().let {
        if (space.order.canEdit) {
            it
        } else {
            it - Action.Space.SetSortOrder
        }
    }

    override fun ensureActionType(action: Action) = action as? Action.Space

    override fun handleNavigationModeEvent(
        context: ActionContext,
        key: KeyTrigger
    ): ActionResult {
        val keyConfig = context.keybindingConfig ?: return ActionResult.NoMatch
        return keyConfig.space.execute(context, key, ::handleAction)
    }

    override fun handleAction(
        context: ActionContext,
        action: Action.Space,
        args: List<String>
    ): ActionResult {
        return context.launchActionAsync(
            actionName = action.name,
            scope = GlobalActionsScope,
            context = Dispatchers.IO,
        ) {
            val sessionId = space.room.sessionId
            val client = peekClient(sessionId) ?: return@launchActionAsync ActionResult.Failure("Client not ready")

            when (action) {
                Action.Space.CopySortOrder -> context.copyToClipboard(space.order.order ?: "null")
                Action.Space.SetSortOrder -> {
                    val order = args.firstOrNull()
                    when (space.order) {
                        is SpaceOrder.AccountData -> setSortOrderAccountData(client, order)
                        is SpaceOrder.SpaceChild -> setSortOrderParentSpace(client, space.order.parentSpaceId, order)
                    }
                }
                Action.Space.SetCatchAll -> client.withJoinedRoom { room ->
                    val enable = args.firstOrNull()?.let { it.toBooleanStrictOrNull()?.orActionValidationError() } ?: true
                    room.updateRoomState("de.spiritcroc.space.catch_all", "") {
                        if (enable) {
                            it.orEmpty().toMutableMap().apply {
                                set("include_orphans", JsonPrimitive(true))
                            }.let(::JsonObject)
                        } else {
                            JsonObject(emptyMap())
                        }
                    }
                }
                Action.Space.SetCatchAllFilterMode -> client.withJoinedRoom { room ->
                    val rawMode = args.firstOrNull()
                    val mode = if (rawMode == null) SpaceCatchAllMode.All else tryOrNull { SpaceCatchAllMode.valueOf(rawMode) }.orActionValidationError()
                    room.updateRoomState("de.spiritcroc.space.catch_all", "") {
                        it.orEmpty().toMutableMap().apply {
                            when (mode) {
                                SpaceCatchAllMode.All -> {
                                    remove("filter_is_dm")
                                }
                                SpaceCatchAllMode.Dms -> {
                                    set("filter_is_dm", JsonPrimitive(true))
                                }
                                SpaceCatchAllMode.Groups -> {
                                    set("filter_is_dm", JsonPrimitive(false))
                                }
                            }
                        }.let(::JsonObject)
                    }
                }
                Action.Space.SetCatchAllInviteFilterMode -> client.withJoinedRoom { room ->
                    val rawMode = args.firstOrNull()
                    val mode = if (rawMode == null) SpaceCatchAllInviteMode.All else tryOrNull { SpaceCatchAllInviteMode.valueOf(rawMode) }.orActionValidationError()
                    room.updateRoomState("de.spiritcroc.space.catch_all", "") {
                        it.orEmpty().toMutableMap().apply {
                            when (mode) {
                                SpaceCatchAllInviteMode.All -> {
                                    remove("filter_is_invite")
                                }
                                SpaceCatchAllInviteMode.Invites -> {
                                    set("filter_is_invite", JsonPrimitive(true))
                                }
                                SpaceCatchAllInviteMode.NonInvites -> {
                                    set("filter_is_invite", JsonPrimitive(false))
                                }
                            }
                        }.let(::JsonObject)
                    }
                }
            }
        }
    }

    private suspend fun setSortOrderAccountData(client: MatrixClient, order: String?): ActionResult {
        return client.updateRoomAccountData(space.room.summary.roomId, ROOM_ACCOUNT_DATA_SPACE_ORDER) {
            it.orEmpty().toMutableMap().apply {
                if (order == null) {
                    remove("order")
                } else {
                    set("order", JsonPrimitive(order))
                }
            }.let(::JsonObject)
        }
    }

    private suspend fun setSortOrderParentSpace(
        client: MatrixClient,
        parentSpaceId: RoomId,
        order: String?
    ): ActionResult {
        return client.withJoinedRoom(parentSpaceId) { room ->
            room.updateRoomState("m.space.child", space.room.summary.roomId.value) {
                // Only set order for valid space children
                if (it.isNullOrEmpty()) {
                    it
                } else {
                    it.toMutableMap().apply {
                        if (order == null) {
                            remove("order")
                        } else {
                            set("order", JsonPrimitive(order))
                        }
                    }.let(::JsonObject)
                }
            }
        }
    }

    private suspend fun MatrixClient.withJoinedRoom(
        roomId: RoomId = space.room.summary.roomId,
        block: suspend (JoinedRoom) -> ActionResult,
    ): ActionResult {
        val room = getJoinedRoom(roomId) ?: return ActionResult.Failure("Room not ready")
        return room.use { block(it) }
    }
}
