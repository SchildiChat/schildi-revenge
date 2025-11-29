package chat.schildi.revenge.actions

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyDown
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import chat.schildi.revenge.DestinationStateHolder
import chat.schildi.revenge.UiState
import chat.schildi.revenge.navigation.Destination
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt

val LocalKeyboardNavigationHandler = compositionLocalOf { KeyboardNavigationHandler(-1) }

private data class FocusTarget(
    val coordinates: Rect,
    val focusRequester: FocusRequester,
    val destinationStateHolder: DestinationStateHolder?,
    val actions: ActionProvider?,
)

class KeyboardNavigationHandler(
    windowId: Int,
) {
    private val log = Logger.withTag("Nav/$windowId")

    var focusManager: FocusManager? = null
    var windowCoordinates: Rect? = null
    private var lastPointerPosition = Offset.Zero
    private val _currentFocus = MutableStateFlow<UUID?>(null)
    val currentFocus = _currentFocus.asStateFlow()

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

    private fun currentFocused() = currentFocus.value?.let { focusableTargets[it] }

    fun executeAction(
        action: InteractionAction,
        destinationStateHolder: DestinationStateHolder? = null
    ): Boolean {
        return when (action) {
            is InteractionAction.NavigationAction -> {
                val destination = action.buildDestination()
                when (action) {
                    is InteractionAction.OpenWindow -> {
                        UiState.openWindow(destination, action.initialTitle)
                        true
                    }
                    is InteractionAction.NavigateCurrent -> {
                        navigateCurrentDestination(destination, destinationStateHolder)
                    }
                }
            }
        }
    }

    private fun navigateCurrentDestination(
        destination: Destination,
        destinationStateHolder: DestinationStateHolder? = null,
    ): Boolean {
        return (
                destinationStateHolder
                    ?: currentFocused()?.destinationStateHolder ?:
                    focusableTargets.values.firstNotNullOfOrNull { it.destinationStateHolder }
        )?.navigate(destination) != null
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
                    // Some navigation
                    Key.I -> navigateCurrentDestination(Destination.Inbox)
                    Key.A -> navigateCurrentDestination(Destination.AccountManagement)
                    // Tertiary action (usually same as mouse middle click)
                    Key.Enter -> currentFocused()?.actions?.tertiaryAction?.let(::executeAction) ?: false
                    else -> false
                }
            } else if (event.isCtrlPressed) {
                when (event.key) {
                    // Secondary action (usually same as mouse right click)
                    Key.Enter -> currentFocused()?.actions?.secondaryAction?.let(::executeAction) ?: false
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
                    // Primary action (usually same as mouse click)
                    Key.Enter -> currentFocused()?.actions?.primaryAction?.let(::executeAction) ?: false
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

    fun onFocusChanged(target: UUID, state: FocusState) {
        log.v { "Focus changed for $target to $state" }
        if (state.isFocused) {
            _currentFocus.value = target
        } else if (!state.hasFocus) {
            _currentFocus.update {
                it.takeIf { it != target }
            }
        }
    }

    fun registerFocusTarget(
        target: UUID,
        coordinates: LayoutCoordinates,
        focusRequester: FocusRequester,
        destinationStateHolder: DestinationStateHolder?,
        actions: ActionProvider?,
    ) {
        focusableTargets[target] = FocusTarget(
            coordinates.boundsInWindow(),
            focusRequester,
            destinationStateHolder,
            actions
        )
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
