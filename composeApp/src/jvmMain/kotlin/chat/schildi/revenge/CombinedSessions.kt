package chat.schildi.revenge

import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.x.di.SessionGraph
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

data class LoadedSession(
    val client: MatrixClient,
    val sessionGraph: SessionGraph,
)

typealias CombinedSessions = StateFlow<List<LoadedSession>>

@OptIn(ExperimentalCoroutinesApi::class)
inline fun <I, reified T, M>Flow<List<I>>.flatMerge(
    crossinline map: suspend (I) -> Flow<T>,
    crossinline onUpdatedInput: suspend (List<I>) -> Unit = ::inlinableNoOp,
    crossinline merge: suspend (Array<T>) -> M,
) = flatMapLatest {
    onUpdatedInput(it)
    combine(it.map { graph -> map(graph) }) {
        merge(it)
    }
}

suspend fun <T>inlinableNoOp(ignored: List<T>) {}
