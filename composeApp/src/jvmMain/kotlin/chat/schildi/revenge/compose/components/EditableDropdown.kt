package chat.schildi.revenge.compose.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import chat.schildi.revenge.actions.ActionContext
import chat.schildi.revenge.actions.ActionResult
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.actionProvider
import chat.schildi.revenge.actions.plainTextCopyAction
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.revenge.compose.focus.rememberFocusId
import chat.schildi.revenge.compose.util.ComposableStringHolder
import kotlinx.collections.immutable.toPersistentList

data class EditableDropdownEntry<T>(
    val value: T,
    val title: ComposableStringHolder,
    val icon: Painter? = null,
    val enabled: Boolean = true,
) {
    fun toContextMenuEntry(
        index: Int,
        persist: suspend ActionContext.(T) -> ActionResult,
        decoration: ContextMenuDecoration? = null,
        enabled: Boolean = true,
    ): ContextMenuEntry {
        return ContextMenuCallbackEntry(
            title = title,
            icon = icon,
            decoration = decoration,
            enabled = enabled && this.enabled,
            keyboardShortcut = index.keyboardShortcutFromIndex(),
            action = { persist(it, value) },
            autoCloseMenu = true,
        )
    }
}

@Composable
fun <T>EditableDropdown(
    currentValue: T,
    items: List<EditableDropdownEntry<out T>>,
    focusRole: FocusRole,
    persist: suspend ActionContext.(T) -> ActionResult,
    enabled: Boolean = true,
    renderCurrentValue: @Composable (Modifier, T, EditableDropdownEntry<out T>?) -> Unit = { modifier, value, entry ->
        Text(
            entry?.title?.render() ?: value.toString(),
            modifier = modifier,
        )
    },
) {
    val focusId = rememberFocusId()
    WithContextMenu(
        focusId = focusId,
        entries = remember(items, currentValue) {
            items.mapIndexed { index, item ->
                item.toContextMenuEntry(
                    index,
                    persist,
                    decoration = if (currentValue == item.value) {
                        if (item.enabled) {
                            ContextMenuDecoration.CheckMark
                        } else {
                            ContextMenuDecoration.DisabledCheckMark
                        }
                    } else {
                        null
                    },
                    enabled = item.enabled,
                )
            }.toPersistentList()
        },
    ) { openContextMenu ->
        val currentEntry = remember(items, currentValue) { items.find { it.value == currentValue } }
        val currentTitle = currentEntry?.title?.render()
        renderCurrentValue(
            Modifier.keyFocusable(
                role = focusRole,
                id = focusId,
                actionProvider = actionProvider(
                    primaryAction = openContextMenu.takeIf { enabled },
                    copyActions = plainTextCopyAction { currentTitle },
                ),
            ),
            currentValue,
            currentEntry,
        )
    }
}
