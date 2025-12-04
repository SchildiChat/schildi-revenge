package chat.schildi.revenge.actions

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val LocalListActionProvider = compositionLocalOf<ListAction?> { null }

data class ListAction(
    val state: LazyListState,
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
}
