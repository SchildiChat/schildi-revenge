package chat.schildi.revenge.compose.focus

import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import chat.schildi.revenge.navigation.LocalKeyboardNavigationHandler
import java.util.UUID

@Composable
fun Modifier.keyContainer(): Modifier {
    val keyHandler = LocalKeyboardNavigationHandler.current
    return pointerInput(keyHandler) {
        awaitPointerEventScope {
            var lastPos: Offset? = null
            while (true) {
                val event = awaitPointerEvent()
                val pos = event.changes.first().position
                if (pos != lastPos) {
                    keyHandler.handlePointer(pos)
                    lastPos = pos
                }
            }
        }
    }
}

@Composable
fun Modifier.keyFocusable(): Modifier {
    val focusRequester = remember { FocusRequester() }
    val id = remember { UUID.randomUUID() }
    val keyHandler = LocalKeyboardNavigationHandler.current
    DisposableEffect(id) {
        onDispose {
            keyHandler.unregisterFocusTarget(id)
        }
    }
    return focusRequester(focusRequester)
        .focusable()
        .onGloballyPositioned { coordinates ->
            keyHandler.registerFocusTarget(id, coordinates, focusRequester)
        }
}
