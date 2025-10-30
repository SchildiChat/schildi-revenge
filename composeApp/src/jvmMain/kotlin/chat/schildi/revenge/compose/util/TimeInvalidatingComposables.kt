package chat.schildi.revenge.compose.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay

/**
 * A [remember] wrapper that forces invalidation every [expiryMs] milliseconds, in addition to any keys passed.
 */
@Composable
inline fun <T>rememberInvalidating(
    expiryMs: Long,
    vararg keys: Any?,
    crossinline calculation: @DisallowComposableCalls () -> T,
): T {
    val invalidationCounter = remember(expiryMs, *keys) { mutableIntStateOf(0) }
    LaunchedEffect(expiryMs, *keys) {
        while (true) {
            delay(expiryMs)
            invalidationCounter.intValue = invalidationCounter.intValue + 1
        }
    }
    return remember(invalidationCounter.intValue, *keys, calculation = calculation)
}
