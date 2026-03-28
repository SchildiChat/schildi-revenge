package chat.schildi.revenge.util

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.io.IOException
import java.io.Closeable

@Suppress("UNCHECKED_CAST")
fun <T1, T2, T3, T4, T5, T6, R>combine(
    flow: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    transform: suspend (T1, T2, T3, T4, T5, T6) -> R
): Flow<R> = combine(flow, flow2, flow3, flow4, flow5, flow6) { args: Array<*> ->
    transform(
        args[0] as T1,
        args[1] as T2,
        args[2] as T3,
        args[3] as T4,
        args[4] as T5,
        args[5] as T6,
    )
}

// Emit immediately but delay too fast updates after that
fun <T>Flow<T>.throttleLatest(period: Long) = flow {
    conflate().collect {
        emit(it)
        delay(period)
    }
}

fun <T : Closeable>Flow<T?>.flowClosable(): Flow<T?> = flow {
    var current: T? = null
    try {
        collect { value ->
            current?.close()
            current = value
            emit(value)
        }
    } finally {
        try {
            current?.let {
                Logger.withTag("flowClosable").d("Closing ${it.javaClass.name}")
                it.close()
            }
        } catch (e: IOException) {
            Logger.withTag("flowClosable").e("Failed to close ${current?.javaClass?.name}", e)
        }
        current = null
    }
}.flowOn(Dispatchers.IO)
