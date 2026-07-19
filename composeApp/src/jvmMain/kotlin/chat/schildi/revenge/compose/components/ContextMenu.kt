package chat.schildi.revenge.compose.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RocketLaunch
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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.key.Key
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
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.ActionContext
import chat.schildi.revenge.actions.ActionResult
import chat.schildi.revenge.actions.InteractionAction
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.resources.ComposableStringHolder
import chat.schildi.resources.toStringHolder
import chat.schildi.revenge.config.keybindings.Action
import chat.schildi.revenge.config.keybindings.displayName
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import shire.res.generated.resources.Res
import shire.res.generated.resources.action_enter_command_mode
import kotlin.uuid.Uuid

sealed interface ContextMenuEntry {
    val title: ComposableStringHolder
    val icon: Painter?
    val decoration: ContextMenuDecoration?
    val keyboardShortcut: Key?
    val critical: Boolean
    val enabled: Boolean
    val autoCloseMenu: Boolean
}

sealed interface ContextMenuDecoration {
    data class Toggle(val checked: Boolean) : ContextMenuDecoration
    data object CheckMark : ContextMenuDecoration
    data object DisabledCheckMark : ContextMenuDecoration
}

data class ContextMenuActionEntry(
    override val title: ComposableStringHolder,
    override val icon: Painter? = null,
    val action: Action,
    val actionArgs: ImmutableList<String> = persistentListOf(),
    override val decoration: ContextMenuDecoration? = null,
    override val keyboardShortcut: Key? = null,
    override val critical: Boolean = false,
    override val enabled: Boolean = true,
    override val autoCloseMenu: Boolean = decoration == null,
) : ContextMenuEntry

data class ContextMenuCallbackEntry(
    override val title: ComposableStringHolder,
    override val icon: Painter? = null,
    val action: suspend (ActionContext) -> ActionResult,
    override val decoration: ContextMenuDecoration? = null,
    override val keyboardShortcut: Key? = null,
    override val critical: Boolean = false,
    override val enabled: Boolean = true,
    override val autoCloseMenu: Boolean = decoration == null,
) : ContextMenuEntry

data class ContextMenuSubmenuEntry(
    override val title: ComposableStringHolder,
    override val icon: Painter? = null,
    val submenuId: Uuid,
    val submenu: ImmutableList<ContextMenuEntry>,
    override val decoration: ContextMenuDecoration? = null,
    override val keyboardShortcut: Key? = null,
    override val critical: Boolean = false,
    override val enabled: Boolean = true,
) : ContextMenuEntry {
    override val autoCloseMenu = false
}

/**
 * @param focusId: The focus ID of the keyFocusable to operate the action on.
 * @param menuId: The ID of the menu, should be equal to [focusId] except for submenus.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WithContextMenu(
    focusId: Uuid,
    popupContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    menuId: Uuid = focusId,
    content: @Composable () -> Unit,
) {
    val keyHandler = LocalKeyboardActionHandler.current
    val currentMenu = keyHandler.currentOpenContextMenu.collectAsState().value
    val expanded = currentMenu?.hasMenu(menuId) == true
    val hasChild = expanded && currentMenu.menuId != menuId
    var anchorBounds by remember { mutableStateOf<Rect?>(null) }
    var pointerPositionOnOpen by remember { mutableStateOf<Offset?>(null) }
    val density = LocalDensity.current

    val containerColor = animateColorAsState(
        if (hasChild) {
            MaterialTheme.colorScheme.surfaceContainerLow
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        }
    ).value

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

    Box(
        modifier.onGloballyPositioned {
            anchorBounds = it.boundsInWindow()
        }
    ) {
        // If we have selectable text, want to override that menu as well
        // TODO teach that menu some copy-selection item to replicate platform menu's functionality?
        WithPlatformTextContextMenuDisabled {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { keyHandler.dismissContextMenu(menuId) },
                containerColor = containerColor,
                offset = offset,
                content = popupContent,
            )
            content()
        }
    }
}

/**
 * @param focusId: The focus ID of the keyFocusable to operate the action on.
 * @param menuId: The ID of the menu, should be equal to [focusId] except for submenus.
 * @param parentMenuId: the menuId of the parent menu in case of submenus.
 */
@Composable
fun WithContextMenu(
    focusId: Uuid,
    entries: ImmutableList<ContextMenuEntry>,
    modifier: Modifier = Modifier,
    menuId: Uuid = focusId,
    parentMenuId: Uuid? = null,
    content: @Composable (InteractionAction.ContextMenu?) -> Unit,
) {
    val keyHandler = LocalKeyboardActionHandler.current
    WithContextMenu(
        focusId = focusId,
        menuId = menuId,
        modifier = modifier,
        content = {
            content(
                if (entries.isEmpty())
                    null
                else
                    InteractionAction.ContextMenu(focusId, entries, menuId, parentMenuId)
            )
        },
        popupContent = {
            (entries + globalContextMenuEntries()).forEach { entry ->
                ContextMenuDropdownMenuItem(
                    entry = entry,
                    focusId = focusId,
                    menuId = menuId,
                    onClick = {
                        keyHandler.handleContextMenuEntry(focusId, menuId, entry)
                    }
                )
            }
        }
    )
}

// Always append command mode option to context menus - TODO make setting (maybe disable together with other non-advanced-user stuff)
@Composable
fun globalContextMenuEntries() = listOf(
    ContextMenuActionEntry(
        Res.string.action_enter_command_mode.toStringHolder(),
        rememberVectorPainter(Icons.Default.RocketLaunch),
        Action.Global.Command,
    )
)

@Composable
private fun ContextMenuDropdownMenuItem(
    entry: ContextMenuEntry,
    focusId: Uuid,
    menuId: Uuid,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (entry is ContextMenuSubmenuEntry) {
        WithContextMenu(
            focusId = focusId,
            menuId = entry.submenuId,
            parentMenuId = menuId,
            entries = entry.submenu,
            modifier = modifier,
        ) {
            ContextMenuDropdownMenuItemContent(
                entry = entry,
                onClick = onClick,
            )
        }
    } else {
        ContextMenuDropdownMenuItemContent(
            entry = entry,
            onClick = onClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun ContextMenuDropdownMenuItemContent(
    entry: ContextMenuEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primaryColor = if (entry.critical) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }
    DropdownMenuItem(
        modifier = modifier,
        enabled = entry.enabled,
        colors = MenuItemColors(
            textColor = primaryColor,
            leadingIconColor = primaryColor,
            trailingIconColor = primaryColor,
            disabledTextColor = MaterialTheme.colorScheme.tertiary,
            disabledLeadingIconColor = MaterialTheme.colorScheme.tertiary,
            disabledTrailingIconColor = MaterialTheme.colorScheme.tertiary,
        ),
        leadingIcon = entry.icon?.let { icon -> {
            Icon(
                icon,
                null,
                Modifier.size(24.dp)
            )
        }},
        trailingIcon = when (val decoration = entry.decoration) {
            is ContextMenuDecoration.Toggle -> {
                {
                    Switch(
                        enabled = entry.enabled,
                        checked = decoration.checked,
                        onCheckedChange = null,
                    )
                }
            }
            is ContextMenuDecoration.CheckMark -> {
                {
                    Icon(
                        Icons.Default.Check,
                        null,
                    )
                }
            }
            is ContextMenuDecoration.DisabledCheckMark -> {
                {
                    Icon(
                        Icons.Default.Check,
                        null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = Dimens.fgDisabledAlpha),
                    )
                }
            }
            null -> {{}}
        },
        text = {
            val title = entry.title.render()
            val text = remember(entry, title) {
                val keyboardShortcut = entry.keyboardShortcut
                if (keyboardShortcut == null) {
                    AnnotatedString(title)
                } else {
                    val keyText = keyboardShortcut.displayName().lowercase()
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
        onClick = onClick,
    )
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
