package chat.schildi.revenge.compose.focus

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.onClick
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import chat.schildi.revenge.DestinationStateHolder
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.actions.ActionProvider
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.KeyboardActionHandler
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.actions.defaultActionProvider
import chat.schildi.revenge.compose.components.ifNotNull
import chat.schildi.revenge.compose.components.thenIf
import chat.schildi.theme.scExposures
import java.util.UUID

@Composable
fun Modifier.windowFocusContainer(): Modifier {
    val keyHandler = LocalKeyboardActionHandler.current
    return pointerInput(keyHandler) {
        awaitPointerEventScope {
            var lastPos: Offset? = null
            while (true) {
                val event = awaitPointerEvent()
                // Only react to actual mouse movement within the window. Do not trigger
                // on focus changes, clicks, or other pointer events that may occur when
                // the window gains/loses focus.
                if (event.type == PointerEventType.Move) {
                    val pos = event.changes.firstOrNull()?.position ?: continue
                    if (pos != lastPos) {
                        keyHandler.handlePointer(pos)
                        lastPos = pos
                    }
                }
            }
        }
    }
}

@Composable
internal fun Modifier.keyFocusableContainer(
    id: UUID,
    parent: FocusParent?,
    role: FocusRole = FocusRole.CONTAINER,
): Modifier {
    val keyHandler = LocalKeyboardActionHandler.current
    val focusRequester = remember(keyHandler, id) { FakeFocusRequester(keyHandler, id) }
    return this.keyFocusableCommon(role = role, id = id, parent = parent, focusRequester = focusRequester)
        .let {
            if (role == FocusRole.DESTINATION_ROOT_CONTAINER) {
                it
            } else {
                it.focusableItemBackground(false, id, keyHandler)
            }
        }
}

fun FocusRole.allowsFocusable() = when (this) {
    FocusRole.LIST_ITEM,
    FocusRole.AUX_ITEM,
    FocusRole.NESTED_AUX_ITEM,
    FocusRole.CONTAINER_ITEM,
    FocusRole.DESTINATION_ROOT_CONTAINER,
    FocusRole.CONTAINER -> true
    FocusRole.TEXT_FIELD_SINGLE_LINE,
    FocusRole.TEXT_FIELD_MULTI_LINE,
    FocusRole.MESSAGE_COMPOSER,
    FocusRole.COMMAND_BAR,
    FocusRole.SEARCH_BAR -> false
}

@Composable
fun rememberFocusId(): UUID = remember { UUID.randomUUID() }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.keyFocusable(
    role: FocusRole = FocusRole.AUX_ITEM,
    id: UUID = rememberFocusId(),
    actionProvider: ActionProvider = defaultActionProvider(),
    focusRequester: FocusRequester = remember { FocusRequester() },
    enableClicks: Boolean = true,
    addClickListener: Boolean = true,
    addMouseFocusable: Boolean = role.allowsFocusable() && actionProvider.primaryAction == null,
    highlight: Boolean = false,
): Modifier {
    val keyHandler = LocalKeyboardActionHandler.current
    val destinationState = LocalDestinationState.current
    if (role.autoRequestFocus) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
    return focusRequester(focusRequester)
        .onFocusChanged {
            keyHandler.onFocusChanged(id, it)
        }
        .thenIf(addMouseFocusable) {
            focusable()
        }
        .ifNotNull(actionProvider.primaryAction, addClickListener) { action ->
            clickable(enabled = enableClicks) {
                keyHandler.executeAction(action, destinationState)
            }
        }
        .ifNotNull(actionProvider.secondaryAction, addClickListener) { action ->
            onClick(enabled = enableClicks, matcher = PointerMatcher.mouse(PointerButton.Secondary)) {
                keyHandler.executeAction(action, destinationState)
            }
        }
        .ifNotNull(actionProvider.tertiaryAction, addClickListener) { action ->
            onClick(enabled = enableClicks, matcher = PointerMatcher.mouse(PointerButton.Tertiary)) {
                keyHandler.executeAction(action, destinationState)
            }
        }
        .keyFocusableCommon(
            role = role,
            keyHandler = keyHandler,
            id = id,
            destinationState = destinationState,
            actionProvider = actionProvider,
            focusRequester = remember(focusRequester) { FocusRequesterWrapper(focusRequester) },
        ).focusableItemBackground(highlight, id, keyHandler)
}

@Composable
private fun Modifier.focusableItemBackground(
    customHighlight: Boolean,
    id: UUID,
    keyHandler: KeyboardActionHandler
): Modifier {
    val state = keyHandler.currentFocusState.collectAsState().value
    val color = animateColorAsState(
        when {
            customHighlight -> MaterialTheme.scExposures.accentColor
            state.commandFocus == id -> MaterialTheme.colorScheme.error
            state.keyboardFocus == id -> MaterialTheme.colorScheme.onSurfaceVariant
            else -> Color.Transparent
        }
    ).value
    return border(
        1.dp,
        color,
    )
}

@Composable
private fun Modifier.keyFocusableCommon(
    role: FocusRole,
    focusRequester: AbstractFocusRequester,
    keyHandler: KeyboardActionHandler = LocalKeyboardActionHandler.current,
    id: UUID = rememberFocusId(),
    destinationState: DestinationStateHolder? = LocalDestinationState.current,
    actionProvider: ActionProvider? = defaultActionProvider(),
    parent: FocusParent? = LocalFocusParent.current,
): Modifier {
    var cached by remember { mutableStateOf<LayoutCoordinates?>(null) }
    DisposableEffect(id) {
        cached?.let { coordinates ->
            keyHandler.registerFocusTarget(
                id,
                parent,
                coordinates,
                focusRequester,
                destinationState,
                actionProvider,
                role,
            )
        }
        onDispose {
            keyHandler.unregisterFocusTarget(id)
        }
    }
    return onGloballyPositioned { coordinates ->
        cached = coordinates
        keyHandler.registerFocusTarget(
            id,
            parent,
            coordinates,
            focusRequester,
            destinationState,
            actionProvider,
            role,
        )
    }
}
