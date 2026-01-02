package chat.schildi.revenge.compose.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import chat.schildi.revenge.actions.InteractionAction
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.compose.util.ComposableStringHolder
import chat.schildi.revenge.config.keybindings.Action
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.awt.event.KeyEvent
import java.util.UUID

data class ContextMenuEntry(
    val title: ComposableStringHolder,
    val icon: Painter? = null,
    val action: Action,
    val actionArgs: ImmutableList<String> = persistentListOf(),
    val toggleState: Boolean? = null,
    val keyboardShortcut: Key? = null,
    val critical: Boolean = false,
    val enabled: Boolean = true,
    val autoCloseMenu: Boolean = toggleState == null,
)

@Composable
fun WithContextMenu(
    focusId: UUID,
    entries: ImmutableList<ContextMenuEntry>,
    modifier: Modifier = Modifier,
    content: @Composable (InteractionAction.ContextMenu?) -> Unit,
) {
    val keyHandler = LocalKeyboardActionHandler.current
    val expanded = keyHandler.currentOpenContextMenu.collectAsState().value == focusId
    var anchorBounds by remember { mutableStateOf<Rect?>(null) }
    var pointerPositionOnOpen by remember { mutableStateOf<Offset?>(null) }
    val density = LocalDensity.current
    Box(
        modifier.onGloballyPositioned {
            anchorBounds = it.boundsInWindow()
        }
    ) {
        content(
            if (entries.isEmpty())
                null
            else
                InteractionAction.ContextMenu(focusId, entries)
        )

        LaunchedEffect(expanded) {
            if (expanded) {
                pointerPositionOnOpen = keyHandler.lastPointerPosition
            }
        }

        val offset = remember(pointerPositionOnOpen, anchorBounds) {
            val anchor = anchorBounds
            val position = pointerPositionOnOpen
            if (anchor == null || position == null) {
                DpOffset(0.dp, 0.dp)
            } else {
                val localX = (position.x - anchor.left).coerceAtLeast(0f)
                with(density) {
                    DpOffset(localX.toDp(), 0.dp)
                }
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { keyHandler.dismissContextMenu(focusId) },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            offset = offset,
        ) {
            entries.forEach { entry ->
                val primaryColor = if (entry.critical) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
                DropdownMenuItem(
                    enabled = entry.enabled,
                    colors = MenuItemColors(
                        textColor = primaryColor,
                        leadingIconColor = primaryColor,
                        trailingIconColor = primaryColor,
                        disabledTextColor = MaterialTheme.colorScheme.tertiary,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.tertiary,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.tertiary,
                    ),
                    leadingIcon = entry.icon?.let {{
                        Icon(
                            entry.icon,
                            null,
                            Modifier.size(24.dp)
                        )
                    }},
                    trailingIcon = if (entry.toggleState == null) null else {{
                        Switch(
                            enabled = entry.enabled,
                            checked = entry.toggleState,
                            onCheckedChange = {
                                if (!entry.enabled) {
                                    return@Switch
                                }
                                keyHandler.handleAction(focusId, entry.action, entry.actionArgs)
                                if (entry.autoCloseMenu) {
                                    keyHandler.dismissContextMenu(focusId)
                                }
                            }
                        )
                    }},
                    text = {
                        val title = entry.title.render()
                        val text = remember(entry, title) {
                            if (entry.keyboardShortcut == null) {
                                AnnotatedString(title)
                            } else {
                                val keyText = KeyEvent.getKeyText(entry.keyboardShortcut.nativeKeyCode).lowercase()
                                val keyIndex = title.lowercase().indexOf(keyText)
                                buildAnnotatedString {
                                    append(title)
                                    if (keyIndex >= 0) {
                                        addStyle(
                                            SpanStyle(textDecoration = TextDecoration.Underline),
                                            keyIndex,
                                            keyIndex + 1,
                                        )
                                    } else {
                                        append(" (")
                                        withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                                            append(keyText)
                                        }
                                        append(")")
                                    }
                                }
                            }
                        }
                        Text(text)
                    },
                    onClick = {
                        if (!entry.enabled) {
                            return@DropdownMenuItem
                        }
                        keyHandler.handleAction(focusId, entry.action, entry.actionArgs)
                        if (entry.autoCloseMenu) {
                            keyHandler.dismissContextMenu(focusId)
                        }
                    }
                )
            }
        }
    }
}

fun Int.keyboardShortcutFromIndex() = when (this) {
    0 -> Key.One
    1 -> Key.Two
    2 -> Key.Three
    3 -> Key.Four
    4 -> Key.Five
    5 -> Key.Six
    6 -> Key.Seven
    7 -> Key.Eight
    8 -> Key.Nine
    9 -> Key.Zero
    else -> null
}
