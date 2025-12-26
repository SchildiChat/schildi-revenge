package chat.schildi.revenge

import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.x.di.SessionGraph
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

data class LoadedSession(
    val client: MatrixClient,
    val sessionGraph: SessionGraph,
)

typealias CombinedSessions = StateFlow<List<LoadedSession>>

@OptIn(ExperimentalCoroutinesApi::class)
inline fun <I, reified T, M>Flow<List<I>>.flatMerge(
    crossinline map: suspend (I) -> Flow<T>,
    crossinline onUpdatedInput: suspend (List<I>) -> Unit = ::inlinableNoOp,
    crossinline onEmpty: () -> M,
    crossinline merge: suspend (Array<T>) -> M,
) = flatMapLatest {
    onUpdatedInput(it)
    if (it.isEmpty()) {
        flowOf(onEmpty())
    } else {
        combine(it.map { graph -> map(graph) }) {
            merge(it)
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
inline fun <I, reified T, M, S>Flow<List<I>>.flatMergeCombinedWith(
    crossinline map: suspend (I, S) -> Flow<T>,
    crossinline onUpdatedInput: suspend (List<I>, S) -> Unit = ::inlinableNoOp,
    crossinline onEmpty: (S) -> M,
    crossinline merge: suspend (Array<T>, S) -> M,
    other: Flow<S>,
) = combine(other) { a, b -> Pair(a, b) }.flatMapLatest { (it, other) ->
    onUpdatedInput(it, other)
    if (it.isEmpty()) {
        flowOf(onEmpty(other))
    } else {
        combine(it.map { graph -> map(graph, other) }) {
            merge(it, other)
        }
    }
}

suspend fun <T>inlinableNoOp(ignored: List<T>) {}
suspend fun <T, S>inlinableNoOp(ignored1: List<T>, ignored2: S) {}
