package chat.schildi.revenge.actions

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val LocalListActionProvider = compositionLocalOf<ListActions?> { null }

@Composable
fun rememberListActions(
    state: LazyListState,
    isReverseList: Boolean = false,
): ListActions {
    val density = LocalDensity.current
    return remember(state, isReverseList) {
        ListActions(state, density, isReverseList)
    }
}

data class ListActions(
    val state: LazyListState,
    val density: Density,
    val isReverseList: Boolean = false,
) {

    fun scrollToStart(scope: CoroutineScope, onFinished: suspend () -> Unit): Boolean {
        return if (state.layoutInfo.totalItemsCount > 0) {
            scope.launch {
                state.scrollToItem(0)
                delay(100)
                onFinished()
            }
            true
        } else {
            false
        }
    }

    fun scrollToEnd(scope: CoroutineScope, onFinished: suspend () -> Unit): Boolean {
        val index = state.layoutInfo.totalItemsCount - 1
        return if (index >= 0) {
            scope.launch {
                state.scrollToItem(index)
                delay(100)
                onFinished()
            }
            true
        } else {
            false
        }
    }

    fun scrollToTop(scope: CoroutineScope, onFinished: suspend () -> Unit): Boolean {
        return if (isReverseList) {
            scrollToEnd(scope, onFinished)
        } else {
            scrollToStart(scope, onFinished)
        }
    }

    fun scrollToBottom(scope: CoroutineScope, onFinished: suspend () -> Unit): Boolean {
        return if (isReverseList) {
            scrollToStart(scope, onFinished)
        } else {
            scrollToEnd(scope, onFinished)
        }
    }

    fun scrollByDp(scope: CoroutineScope, amount: Float) {
        scope.launch {
            val sign = if (isReverseList) -1 else 1
            state.scrollBy(amount * density.density * sign)
        }
    }

    fun animateScrollByDp(scope: CoroutineScope, amount: Float) {
        scope.launch {
            val sign = if (isReverseList) -1 else 1
            state.animateScrollBy(amount * density.density * sign)
        }
    }

    fun scrollByPage(scope: CoroutineScope, percent: Float) {
        scope.launch {
            val pageSize = state.layoutInfo.viewportSize.height
            val sign = if (isReverseList) -1 else 1
            state.scrollBy(pageSize * percent * sign)
        }
    }

    fun animateScrollByPage(scope: CoroutineScope, percent: Float) {
        scope.launch {
            val pageSize = state.layoutInfo.viewportSize.height
            val sign = if (isReverseList) -1 else 1
            state.animateScrollBy(pageSize * percent * sign)
        }
    }
}
