package chat.schildi.revenge.store

import chat.schildi.revenge.model.ScopedRoomKey
import chat.schildi.revenge.model.invites.SeenInvitesStore
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

@Serializable
data class PersistentAppState(
    val sortedAccounts: List<String> = emptyList(),
    val lastInboxState: PersistentInboxState? = null,
    val mutedAccounts: List<String> = emptyList(),
    val seenInvites: List<PersistentScopedRoomKey>? = null,
)

@Serializable
data class PersistentScopedRoomKey(
    val sessionId: String,
    val roomId: String,
) {
    fun map() = ScopedRoomKey(SessionId(sessionId), RoomId(roomId))
    companion object {
        fun from(key: ScopedRoomKey) = PersistentScopedRoomKey(
            sessionId = key.sessionId.value,
            roomId = key.roomId.value,
        )
    }
}

@Serializable
data class PersistentInboxState(
    val spaceSelection: List<String> = emptyList(),
    val hiddenAccounts: List<String> = emptyList(),
)

data class AppStateStore(
    val scope: CoroutineScope,
) : SeenInvitesStore, FileBackedStore<PersistentAppState>(
    tag = "AppStateStore",
    scope = scope,
    fileName = "state.json",
    serializer = PersistentAppState.serializer(),
    default = PersistentAppState(),
) {
    val sessionIdOrder = config.map { currentConfig ->
        currentConfig?.sortedAccounts?.mapIndexed { index, account ->
            Pair(index, account)
        }?.associate { it.second to it.first }.orEmpty()
    }

    val sessionIdComparator = sessionIdOrder.map { accountOrders ->
        Comparator<SessionId> { left, right ->
            val leftOrder = accountOrders[left.value]
            val rightOrder = accountOrders[right.value]
            if (leftOrder == null && rightOrder == null) {
                // Fall back to just string comparison
                compareValues(left.value, right.value)
            } else {
                compareValues(leftOrder ?: Integer.MAX_VALUE, rightOrder ?: Integer.MAX_VALUE)
            }
        }
    }

    fun persistInboxState(state: PersistentInboxState) = update {
        it.copy(lastInboxState = state)
    }

    fun persistMutedAccounts(mutedAccounts: List<String>) = update {
        it.copy(mutedAccounts = mutedAccounts.sorted())
    }

    fun ensureAllSessionIdsTracked(sessionIds: List<String>) = update { meta ->
        meta.copy(
            sortedAccounts = meta.sortedAccounts + sessionIds.filter { it !in meta.sortedAccounts }
        )
    }

    override fun seenInvites(): Flow<Set<ScopedRoomKey>> = config.map {
        it?.seenInvites?.map { it.map() }.orEmpty().toPersistentSet()
    }

    override fun isInviteSeen(key: ScopedRoomKey): Boolean =
        config.value?.seenInvites?.contains(PersistentScopedRoomKey.from(key)) == true

    override suspend fun markInviteAsSeen(key: ScopedRoomKey) = update {
        it.copy(
            seenInvites = (it.seenInvites?.toSet().orEmpty() + PersistentScopedRoomKey.from(key)).toList()
        )
    }

    override suspend fun markInviteAsUnSeen(key: ScopedRoomKey) = update {
        val persistentKey = PersistentScopedRoomKey.from(key)
        it.copy(
            seenInvites = it.seenInvites?.filter { it != persistentKey }
        )
    }
}
