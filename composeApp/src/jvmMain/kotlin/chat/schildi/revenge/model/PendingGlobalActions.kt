package chat.schildi.revenge.model

import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.RoomIdOrAlias
import io.element.android.libraries.matrix.api.room.BaseRoom
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

sealed interface PendingActionState {
    sealed interface Pending : PendingActionState
    data object InProgress : Pending
    data object AwaitingServerEcho : Pending
}

sealed interface PendingAction {
    val requiresEcho: Boolean
    fun conflictingActions(): Set<PendingAction> = emptySet()
    sealed interface JoinableRoomAction {
        val roomId: RoomId
    }
    data class RoomJoin(override val roomId: RoomId) : PendingAction, JoinableRoomAction {
        override val requiresEcho = true
        override fun conflictingActions() = setOf(RoomLeave(roomId))
    }
    data class RoomLeave(override val roomId: RoomId) : PendingAction, JoinableRoomAction {
        override val requiresEcho = false
        override fun conflictingActions() = setOf(RoomJoin(roomId))
    }
}

object PendingGlobalActions {
    private val pending = MutableStateFlow(emptyMap<PendingAction, PendingActionState>())

    fun onActionLaunched(action: PendingAction) {
        pending.update { it + (action to PendingActionState.InProgress) }
    }

    fun onActionEcho(action: PendingAction) {
        pending.update { it - action }
    }

    fun onResult(action: PendingAction, success: Boolean) {
        pending.update {
            when {
                success && action.requiresEcho -> it + (action to PendingActionState.AwaitingServerEcho)
                else -> it - action
            }
        }
    }

    fun follow(action: PendingAction) = pending.map { it[action] }
    fun <T>map(transform: (get: (PendingAction) -> PendingActionState?) -> T) = pending.map { transform(it::get) }

    inline fun <T>withActionTracked(action: PendingAction, block: () -> Result<T>): Result<T> {
        var success = false
        return try {
            onActionLaunched(action)
            block().also {
                success = it.isSuccess
            }
        } finally {
            onResult(action, success)
        }
    }
}

suspend fun MatrixClient.joinRoomTracked(roomId: RoomId) = PendingGlobalActions.withActionTracked(PendingAction.RoomJoin(roomId)) {
    joinRoom(roomId)
}

suspend fun MatrixClient.joinRoomByIdOrAliasTracked(
    roomIdOrAlias: RoomIdOrAlias,
    serverNames: List<String>,
) = when (roomIdOrAlias) {
    // Alias tracking not supported
    is RoomIdOrAlias.Alias -> joinRoomByIdOrAlias(roomIdOrAlias, serverNames)
    is RoomIdOrAlias.Id -> PendingGlobalActions.withActionTracked(PendingAction.RoomJoin(roomIdOrAlias.roomId)) {
        joinRoomByIdOrAlias(roomIdOrAlias, serverNames)
    }
}

suspend fun BaseRoom.joinTracked() = PendingGlobalActions.withActionTracked(PendingAction.RoomJoin(roomId)) {
    join()
}

suspend fun BaseRoom.leaveTracked() = PendingGlobalActions.withActionTracked(PendingAction.RoomLeave(roomId)) {
    leave()
}
