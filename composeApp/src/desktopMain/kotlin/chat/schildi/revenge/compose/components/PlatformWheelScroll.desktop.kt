package chat.schildi.revenge.compose.components

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

actual fun Modifier.horizontalScrollFromVerticalWheel(
    scrollState: ScrollState,
    scrollAmount: Dp
): Modifier = composed {
    val scrollAmountPx = with(LocalDensity.current) { scrollAmount.toPx() }
    @OptIn(ExperimentalComposeUiApi::class)
    onPointerEvent(PointerEventType.Scroll) { event ->
        val scrollDelta = event.changes.firstOrNull()?.scrollDelta ?: return@onPointerEvent
        if (scrollDelta.x == 0f && scrollDelta.y != 0f) {
            scrollState.dispatchRawDelta(scrollDelta.y * scrollAmountPx)
            event.changes.forEach { it.consume() }
        }
    }
}
