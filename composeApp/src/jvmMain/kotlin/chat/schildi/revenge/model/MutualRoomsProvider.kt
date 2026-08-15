package chat.schildi.revenge.model

import chat.schildi.revenge.UiState
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.core.UserId
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

private const val MAX_ROOMS_IN_COMMON = 50

class MutualRoomsProvider(
    val sessionId: SessionId,
    private val userId: Flow<UserId?>,
    private val scope: CoroutineScope,
    client: StateFlow<MatrixClient?> = UiState.selectClient(sessionId, scope),
    private val loadStateHolder: LoadStateHolder? = null,
    private val maxPreviews: Int = MAX_ROOMS_IN_COMMON,
) {

    val mutualRooms = combine(
        client,
        userId
    ) { client, userId ->
        if (userId == null || userId == sessionId) {
            null
        } else {
            client?.getMutualRooms(userId)
                ?.also { loadStateHolder?.handleResult(LoadCheckPoint.RoomsInCommon, it) }
                ?.getOrNull()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val mutualRoomsPreview = combine(
        mutualRooms,
        client,
    ) { a, b ->
        Pair(a, b)
    }.flatMapLatest { (mutualRoomsInfo, client) ->
        client ?: return@flatMapLatest flowOf(null)
        val roomInfoFlows = mutualRoomsInfo?.joined
            ?.take(maxPreviews)
            ?.mapNotNull { roomId ->
                client.getRoom(roomId)?.roomInfoFlow
            } ?: return@flatMapLatest flowOf(null)
        combine(roomInfoFlows) { infos ->
            infos.sortedWith(
                compareBy(
                    // Show rooms with name first
                    { it.name == null },
                    // Show tombstoned rooms last
                    { it.successorRoom != null },
                    // Show non-space rooms first
                    { it.isSpace }
                )
            ).toPersistentList()
        }
    }.stateIn(scope, SharingStarted.Lazily, null)
}
