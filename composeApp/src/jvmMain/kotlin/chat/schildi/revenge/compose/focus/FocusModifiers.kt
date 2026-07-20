package chat.schildi.revenge.compose.focus

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.dp
import chat.schildi.revenge.DestinationStateHolder
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.actions.ActionProvider
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.KeyboardActionHandler
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.actions.actionProvider
import chat.schildi.revenge.compose.components.ifNotNull
import chat.schildi.revenge.compose.components.thenIf
import chat.schildi.theme.scExposures
import kotlin.uuid.Uuid

@Composable
fun Modifier.windowFocusContainer(): Modifier {
    val keyHandler = LocalKeyboardActionHandler.current
    return pointerInput(keyHandler) {
        awaitPointerEventScope {
            var lastPos: Offset? = null
            while (true) {
                val event = awaitPointerEvent()
                val pointer = event.changes.firstOrNull() ?: continue
                if (pointer.position != lastPos) {
                    keyHandler.handlePointer(pointer.position, event.type, pointer.type)
                    lastPos = pointer.position
                }
            }
        }
    }
}

@Composable
internal fun Modifier.keyFocusableContainer(
    id: Uuid,
    parent: FocusParent?,
    role: FocusRole = FocusRole.CONTAINER,
): Modifier {
    val keyHandler = LocalKeyboardActionHandler.current
    val focusRequester = remember(keyHandler, id) { FakeFocusRequester(keyHandler, id, role) }
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
    FocusRole.LIST_ITEM_EDITABLE,
    FocusRole.AUX_ITEM,
    FocusRole.AUX_ITEM_EDITABLE,
    FocusRole.NESTED_AUX_ITEM,
    FocusRole.CONTAINER_ITEM,
    FocusRole.DESTINATION_ROOT_CONTAINER,
    FocusRole.NESTING_DESTINATION_ROOT_CONTAINER,
    FocusRole.CONTAINER -> true
    FocusRole.CONTEXT_MENU_ENTRY_WITH_SUBMENU,
    FocusRole.TEXT_FIELD_SINGLE_LINE,
    FocusRole.TEXT_FIELD_MULTI_LINE,
    FocusRole.MESSAGE_COMPOSER,
    FocusRole.COMMAND_BAR,
    FocusRole.SEARCH_BAR -> false
}

fun FocusRole.preferFocusChildren() = when (this) {
    FocusRole.DESTINATION_ROOT_CONTAINER,
    FocusRole.NESTING_DESTINATION_ROOT_CONTAINER,
    FocusRole.CONTAINER -> true
    FocusRole.CONTEXT_MENU_ENTRY_WITH_SUBMENU,
    FocusRole.CONTAINER_ITEM,
    FocusRole.LIST_ITEM,
    FocusRole.LIST_ITEM_EDITABLE,
    FocusRole.AUX_ITEM,
    FocusRole.AUX_ITEM_EDITABLE,
    FocusRole.NESTED_AUX_ITEM,
    FocusRole.TEXT_FIELD_SINGLE_LINE,
    FocusRole.TEXT_FIELD_MULTI_LINE,
    FocusRole.MESSAGE_COMPOSER,
    FocusRole.COMMAND_BAR,
    FocusRole.SEARCH_BAR -> false
}

@Composable
fun rememberFocusId(): Uuid = remember { Uuid.random() }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.keyFocusable(
    role: FocusRole = FocusRole.AUX_ITEM,
    id: Uuid = rememberFocusId(),
    actionProvider: ActionProvider = actionProvider(),
    focusRequester: FocusRequester = remember { FocusRequester() },
    enableClicks: Boolean = true,
    addClickListener: Boolean = true,
    addMouseFocusable: Boolean = role.allowsFocusable() && actionProvider.primaryAction == null,
    highlight: Boolean = false,
): Modifier {
    val keyHandler = LocalKeyboardActionHandler.current
    val destinationState = LocalDestinationState.current
    if (role.shouldAutoRequestFocus()) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
    return focusRequester(focusRequester)
        .onFocusChanged {
            keyHandler.onFocusChanged(id, it, role)
        }
        .thenIf(addMouseFocusable) {
            focusable()
        }
        .ifNotNull(actionProvider.secondaryAction, addClickListener) { action ->
            combinedClickable(
                enabled = enableClicks,
                onLongClick = {
                    keyHandler.executeAction(action, destinationState)
                },
            ) {
                actionProvider.primaryAction?.let {
                    keyHandler.executeAction(it, destinationState)
                }
            }
        }
        .ifNotNull(actionProvider.primaryAction, addClickListener && actionProvider.secondaryAction == null) { action ->
            clickable(enabled = enableClicks) {
                keyHandler.executeAction(action, destinationState)
            }
        }
        .ifNotNull(actionProvider.secondaryAction, addClickListener) { action ->
            platformPointerClick(enabled = enableClicks, button = PlatformPointerButton.Secondary) {
                keyHandler.executeAction(action, destinationState)
            }
        }
        .ifNotNull(actionProvider.tertiaryAction, addClickListener) { action ->
            platformPointerClick(enabled = enableClicks, button = PlatformPointerButton.Tertiary) {
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
    id: Uuid,
    keyHandler: KeyboardActionHandler
): Modifier {
    val state = keyHandler.currentFocusState.collectAsState().value
    val color = when {
        customHighlight -> MaterialTheme.scExposures.accentColor
        state.commandFocus == id -> MaterialTheme.scExposures.commandHint
        !state.windowFocused -> Color.Transparent
        state.keyboardFocus == id -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> Color.Transparent
    }
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
    id: Uuid = rememberFocusId(),
    destinationState: DestinationStateHolder? = LocalDestinationState.current,
    actionProvider: ActionProvider? = actionProvider(),
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

@Composable
fun FocusRole.shouldAutoRequestFocus(): Boolean {
    if (!autoRequestFocus) return false
    if (this == FocusRole.MESSAGE_COMPOSER) {
        // LocalInputModeManager thinks everything that's not a keypress is "touch", so do our own pointer tracking
        val isTouch = LocalKeyboardActionHandler.current.lastPointerType.collectAsState().value?.let {
            it == PointerType.Touch
        } ?: (LocalInputModeManager.current.inputMode == InputMode.Touch)
        return !isTouch
    }
    return true
}
