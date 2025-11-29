package chat.schildi.revenge.compose.focus

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.onClick
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.actions.ActionProvider
import chat.schildi.revenge.actions.LocalKeyboardNavigationHandler
import chat.schildi.revenge.actions.defaultActionProvider
import chat.schildi.revenge.compose.components.ifNotNull
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.keyFocusable(
    actionProvider: ActionProvider = defaultActionProvider(),
    enableClicks: Boolean = true,
): Modifier {
    val focusRequester = remember { FocusRequester() }
    val id = remember { UUID.randomUUID() }
    val keyHandler = LocalKeyboardNavigationHandler.current
    val destinationState = LocalDestinationState.current
    DisposableEffect(id) {
        onDispose {
            keyHandler.unregisterFocusTarget(id)
        }
    }
    return focusRequester(focusRequester)
        .onFocusChanged {
            keyHandler.onFocusChanged(id, it)
        }
        .focusable()
        .ifNotNull(actionProvider.primaryAction) { action ->
            clickable(enabled = enableClicks) {
                keyHandler.executeAction(action, destinationState)
            }
        }
        .ifNotNull(actionProvider.secondaryAction) { action ->
            onClick(enabled = enableClicks, matcher = PointerMatcher.mouse(PointerButton.Secondary)) {
                keyHandler.executeAction(action, destinationState)
            }
        }
        .ifNotNull(actionProvider.tertiaryAction) { action ->
            onClick(enabled = enableClicks, matcher = PointerMatcher.mouse(PointerButton.Tertiary)) {
                keyHandler.executeAction(action, destinationState)
            }
        }
        .onGloballyPositioned { coordinates ->
            keyHandler.registerFocusTarget(
                id,
                coordinates,
                focusRequester,
                destinationState,
                actionProvider,
            )
        }.border( // TODO nicer design?
            1.dp,
            MaterialTheme.colorScheme.onSurfaceVariant.copy(
                alpha = animateFloatAsState(
                    if (id == keyHandler.currentFocus.collectAsState().value) {
                        1f
                    } else {
                        0f
                    }
                ).value
            )
        )
}
