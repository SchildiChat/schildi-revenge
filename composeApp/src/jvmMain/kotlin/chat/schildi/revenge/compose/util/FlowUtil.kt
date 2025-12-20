package chat.schildi.revenge.compose.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow


// Emit immediately but delay too fast updates after that
fun <T> Flow<T>.throttleLatest(period: Long) = flow {
    conflate().collect {
        emit(it)
        delay(period)
    }
}
