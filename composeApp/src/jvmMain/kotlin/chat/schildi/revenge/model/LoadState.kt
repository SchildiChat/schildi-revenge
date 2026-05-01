package chat.schildi.revenge.model

import chat.schildi.revenge.compose.util.ComposableStringHolder
import chat.schildi.revenge.compose.util.toStringHolder
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.room.RoomMembersState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.action_show_room_members
import shire.composeapp.generated.resources.hint_member_profile
import shire.composeapp.generated.resources.hint_room
import shire.composeapp.generated.resources.hint_timeline
import shire.composeapp.generated.resources.hint_user_profile

interface LoadCheckPoint {
    val name: ComposableStringHolder

    data class Client(
        val sessionId: SessionId
    ) : LoadCheckPoint {
        override val name = sessionId.value.toStringHolder()
    }

    data object Room : LoadCheckPoint {
        override val name = Res.string.hint_room.toStringHolder()
    }

    data object UserProfile : LoadCheckPoint {
        override val name = Res.string.hint_user_profile.toStringHolder()
    }

    data object MemberProfile : LoadCheckPoint {
        override val name = Res.string.hint_member_profile.toStringHolder()
    }

    data object RoomMembers : LoadCheckPoint {
        override val name = Res.string.action_show_room_members.toStringHolder()
    }

    data object Timeline : LoadCheckPoint {
        override val name = Res.string.hint_timeline.toStringHolder()
    }

    data object TimelineItems : LoadCheckPoint {
        override val name = "Timeline items".toStringHolder()
    }
}

enum class CheckpointLoadState {
    PENDING,
    LOADED,
    FAILED,
}

fun Any?.asCheckpointLoadedOrPending() = if (this == null) CheckpointLoadState.PENDING else CheckpointLoadState.LOADED
fun Any?.asCheckpointLoadedOrFailed() = if (this == null) CheckpointLoadState.FAILED else CheckpointLoadState.LOADED
fun RoomMembersState.asCheckpointLoadState() = when (this) {
    is RoomMembersState.Ready -> CheckpointLoadState.LOADED
    is RoomMembersState.Error -> CheckpointLoadState.FAILED
    is RoomMembersState.Pending,
    RoomMembersState.Unknown -> CheckpointLoadState.PENDING
}

data class LoadStateEntry(
    val checkpoint: LoadCheckPoint,
    val state: CheckpointLoadState,
    val extraInfo: String? = null,
)

typealias LoadState = ImmutableList<LoadStateEntry>

/**
 * TODO use me: when developer info on screen enabled, show load progress in empty screen placeholder,
 *  if passed from view Model. ViewModel created with expected and then sets as failed/completed on the go.
 *  Initial support for:
 *  - A client for each Account for splash screen, to track until splash is cleared
 *  - Room list: everything needed to render it (client, joinedRoom, timeline, ???)
 */
class LoadStateHolder(
    initialExpectedCheckpoints: List<LoadCheckPoint>,
) {
    constructor(vararg expectedCheckpoints: LoadCheckPoint) : this(expectedCheckpoints.toList())

    private val _state = MutableStateFlow(
        initialExpectedCheckpoints.map {
            LoadStateEntry(
                checkpoint = it,
                state = CheckpointLoadState.PENDING,
            )
        }.toImmutableList()
    )

    val state: StateFlow<LoadState> = _state.asStateFlow()

    fun addExpected(vararg checkpoints: LoadCheckPoint) {
        _state.update { oldState ->
            val newCheckpoints = (checkpoints.toSet() - oldState.map { it.checkpoint }.toSet()).map {
                LoadStateEntry(it, CheckpointLoadState.PENDING)
            }
            if (newCheckpoints.isEmpty()) {
                oldState
            } else {
                (oldState + newCheckpoints).toImmutableList()
            }
        }
    }

    fun set(checkpoint: LoadCheckPoint, state: CheckpointLoadState, extraInfo: String? = null) {
        _state.update {
            val index = it.indexOfFirst { it.checkpoint == checkpoint }
            if (index != -1) {
                it.toMutableList().apply {
                    set(index, LoadStateEntry(checkpoint, state, extraInfo))
                }
            } else {
                it + LoadStateEntry(checkpoint, state, extraInfo)
            }.toImmutableList()
        }
    }

    fun <T>handleResult(checkpoint: LoadCheckPoint, result: Result<T>?, extraInfo: ((T) -> String?)? = null) {
        val state = when {
            result == null -> CheckpointLoadState.PENDING
            result.isSuccess -> CheckpointLoadState.LOADED
            else -> CheckpointLoadState.FAILED
        }
        set(checkpoint, state, extraInfo?.let { result?.getOrNull()?.let { extraInfo(it)} })
    }
}
