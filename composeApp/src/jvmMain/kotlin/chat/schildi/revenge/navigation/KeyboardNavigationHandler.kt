package chat.schildi.revenge.navigation

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyDown
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import co.touchlab.kermit.Logger
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt

val LocalKeyboardNavigationHandler = compositionLocalOf { KeyboardNavigationHandler(-1) }

private data class FocusTarget(
    val coordinates: Rect,
    val focusRequester: FocusRequester,
)

class KeyboardNavigationHandler(
    windowId: Int,
) {
    private val log = Logger.withTag("Nav/$windowId")

    var focusManager: FocusManager? = null
    var windowCoordinates: Rect? = null
    private var lastPointerPosition = Offset.Zero

    private val focusableTargets = ConcurrentHashMap<UUID, FocusTarget>()

    private fun moveFocus(focusDirection: FocusDirection): Boolean {
        return focusManager?.moveFocus(focusDirection) == true ||
                focusableTargets.values.firstOrNull()?.focusRequester?.let {
                    log.i { "Could not move focus to $focusDirection, force focus anything" }
                    it.requestFocus()
                } ?: false
    }

    private fun distanceToRect(rect: Rect, p: Offset): Float {
        val nearestX = p.x.coerceIn(rect.left, rect.right)
        val nearestY = p.y.coerceIn(rect.top, rect.bottom)

        val dx = p.x - nearestX
        val dy = p.y - nearestY

        return sqrt(dx * dx + dy * dy)
    }

    private fun focusClosestTo(position: Offset): Boolean {
        // TODO this works but doesn't reflect in the UI
        return focusableTargets.values.minByOrNull {
            distanceToRect(it.coordinates, position)
        }?.focusRequester?.requestFocus() ?: false
    }

    fun onPreviewKeyEvent(event: KeyEvent): Boolean {
        log.v { "Handle key preview $event" }
        // TODO
        if (event.type == KeyDown) {
            // TODO make this configurable
            return if (event.isShiftPressed) {
                when (event.key) {
                    // Relative focus - TODO should maintain X offset rather than assuming center
                    Key.H -> windowCoordinates?.let { focusClosestTo(it.topCenter) } ?: false
                    Key.M -> windowCoordinates?.let { focusClosestTo(it.center) } ?: false
                    Key.L -> windowCoordinates?.let { focusClosestTo(it.bottomCenter) } ?: false
                    else -> false
                }
            } else {
                when (event.key) {
                    // Arrow keys
                    Key.DirectionLeft -> moveFocus(FocusDirection.Left)
                    Key.DirectionRight -> moveFocus(FocusDirection.Right)
                    Key.DirectionUp -> moveFocus(FocusDirection.Up)
                    Key.DirectionDown -> moveFocus(FocusDirection.Down)
                    // QWERTY VIM-like navigation
                    Key.H -> moveFocus(FocusDirection.Left)
                    Key.J -> moveFocus(FocusDirection.Down)
                    Key.K -> moveFocus(FocusDirection.Up)
                    Key.L -> moveFocus(FocusDirection.Right)
                    // Colemak VIM-like navigation
                    Key.E -> moveFocus(FocusDirection.Up)
                    Key.N -> moveFocus(FocusDirection.Down)
                    Key.I -> moveFocus(FocusDirection.Right)
                    else -> false
                }
            }
        }
        return false
    }

    fun onKeyEvent(event: KeyEvent): Boolean {
        log.v { "Handle key $event" }
        // TODO
        return false
    }

    fun registerFocusTarget(target: UUID, coordinates: LayoutCoordinates, focusRequester: FocusRequester) {
        focusableTargets[target] = FocusTarget(coordinates.boundsInWindow(), focusRequester)
    }

    fun unregisterFocusTarget(target: UUID) {
        focusableTargets.remove(target)
    }

    fun handlePointer(position: Offset) {
        lastPointerPosition = position
        val focusable = focusableTargets.firstNotNullOfOrNull { target ->
            target.takeIf {
                it.value.coordinates.contains(position)
            }
        }
        val requested = focusable?.value?.focusRequester?.requestFocus()
        log.v { "Focused via pointer: $requested $focusable" }
    }
}
