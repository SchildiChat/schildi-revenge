package chat.schildi.revenge.store

import io.element.android.libraries.matrix.api.core.SessionId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

@Serializable
data class PersistentAppState(
    val sortedAccounts: List<String> = emptyList(),
)

data class AppStateStore(
    val scope: CoroutineScope,
) : FileBackedStore<PersistentAppState>(
    tag = "AppStateStore",
    scope = scope,
    fileName = "state.json",
    serializer = PersistentAppState.serializer(),
    default = PersistentAppState(),
) {
    val sessionIdComparator = config.map { currentConfig ->
        val accountOrders = currentConfig?.sortedAccounts?.mapIndexed { index, account ->
            Pair(index, account)
        }?.associate { it.second to it.first }.orEmpty()
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
}
