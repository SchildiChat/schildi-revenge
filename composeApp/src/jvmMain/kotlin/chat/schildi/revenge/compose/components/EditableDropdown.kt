package chat.schildi.revenge.compose.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.Key
import chat.schildi.revenge.actions.ActionContext
import chat.schildi.revenge.actions.ActionResult
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.actionProvider
import chat.schildi.revenge.actions.plainTextCopyAction
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.revenge.compose.focus.rememberFocusId
import chat.schildi.resources.ComposableStringHolder
import kotlinx.collections.immutable.toPersistentList
import org.jetbrains.compose.resources.stringResource
import shire.res.generated.resources.Res
import shire.res.generated.resources.hint_nothing_selected

interface KeyboardShortcutAssigner<T> {
    operator fun invoke(index: Int, item: T?): Key?

    class ZeroIndexed<T> : KeyboardShortcutAssigner<T> {
        override fun invoke(index: Int, item: T?): Key? = index.keyboardShortcutFromIndexZero()
    }
    class Indexed<T> : KeyboardShortcutAssigner<T> {
        override fun invoke(index: Int, item: T?): Key? = index.keyboardShortcutFromIndex()
    }
}

data class EditableDropdownEntry<T>(
    val value: T,
    val title: ComposableStringHolder,
    val icon: Painter? = null,
    val enabled: Boolean = true,
) {
    fun toContextMenuEntry(
        keyboardShortcut: Key?,
        persist: suspend ActionContext.(T) -> ActionResult,
        decoration: ContextMenuDecoration? = null,
        enabled: Boolean = true,
    ): ContextMenuEntry {
        return ContextMenuCallbackEntry(
            title = title,
            icon = icon,
            decoration = decoration,
            enabled = enabled && this.enabled,
            keyboardShortcut = keyboardShortcut,
            action = { persist(it, value) },
            autoCloseMenu = true,
        )
    }
}

@Composable
fun <T>EditableDropdown(
    currentValue: T?,
    items: List<EditableDropdownEntry<out T>>,
    focusRole: FocusRole,
    persist: suspend ActionContext.(T) -> ActionResult,
    enabled: Boolean = true,
    nullItem: EditableDropdownEntry<Unit>? = null,
    persistNull: suspend () -> ActionResult = { ActionResult.Inapplicable },
    nullText: String = stringResource(Res.string.hint_nothing_selected),
    keyboardShortcutAssigner: KeyboardShortcutAssigner<T> = if (nullItem == null)
        KeyboardShortcutAssigner.Indexed()
    else
        KeyboardShortcutAssigner.ZeroIndexed(),
    renderCurrentValue: @Composable (Modifier, T?, EditableDropdownEntry<out T>?) -> Unit = { modifier, value, entry ->
        Text(
            entry?.title?.render() ?: value?.toString() ?: nullText,
            modifier = modifier,
        )
    },
) {
    if (nullItem == null) {
        EditableDropdownImpl(
            currentValue = currentValue,
            items = items,
            focusRole = focusRole,
            persist = persist,
            enabled = enabled,
            renderCurrentValue = renderCurrentValue,
            keyboardShortcutAssigner = keyboardShortcutAssigner,
        )
    } else {
        EditableDropdownWithNullItem(
            currentValue = currentValue,
            nullItem = nullItem,
            items = items,
            focusRole = focusRole,
            persist = {
                if (it == null) {
                    persistNull()
                } else {
                    persist(it)
                }
            },
            enabled = enabled,
            renderCurrentValue = renderCurrentValue,
            keyboardShortcutAssigner = keyboardShortcutAssigner,
        )
    }
}

@Composable
private fun <T>EditableDropdownWithNullItem(
    currentValue: T?,
    nullItem: EditableDropdownEntry<Unit>,
    items: List<EditableDropdownEntry<out T>>,
    focusRole: FocusRole,
    persist: suspend ActionContext.(T?) -> ActionResult,
    enabled: Boolean = true,
    keyboardShortcutAssigner: KeyboardShortcutAssigner<T> = KeyboardShortcutAssigner.ZeroIndexed(),
    renderCurrentValue: @Composable (Modifier, T?, EditableDropdownEntry<out T>?) -> Unit,
) {
    val mappedItems = remember(nullItem, items) {
        (listOf<EditableDropdownEntry<Option<T>>>(nullItem.wrapNullOption()) + items.map { it.wrapOption() })
    }
    EditableDropdownImpl(
        currentValue = currentValue.toOption(),
        items = mappedItems,
        focusRole = focusRole,
        persist = {
            when (it) {
                is Option.Some -> persist(it.value)
                is Option.None -> persist(null)
            }
        },
        enabled = enabled,
        renderCurrentValue = { modifier, option, entry ->
            when (option) {
                is Option.Some -> renderCurrentValue(modifier, option.value, entry?.unwrapOption())
                else -> renderCurrentValue(modifier, null, null)
            }
        },
        keyboardShortcutAssigner = object : KeyboardShortcutAssigner<Option<out T>> {
            override fun invoke(index: Int, item: Option<out T>?): Key? =
                keyboardShortcutAssigner.invoke(index, (item as? Option.Some)?.value)
        }
    )
}

@Composable
private fun <T>EditableDropdownImpl(
    currentValue: T?,
    items: List<EditableDropdownEntry<out T>>,
    focusRole: FocusRole,
    persist: suspend ActionContext.(T) -> ActionResult,
    enabled: Boolean,
    renderCurrentValue: @Composable (Modifier, T?, EditableDropdownEntry<out T>?) -> Unit,
    keyboardShortcutAssigner: KeyboardShortcutAssigner<T>,
) {
    val focusId = rememberFocusId()
    WithContextMenu(
        focusId = focusId,
        entries = remember(items, currentValue) {
            items.mapIndexed { index, item ->
                item.toContextMenuEntry(
                    keyboardShortcutAssigner(index, item.value),
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
        val modifier = Modifier.keyFocusable(
            role = focusRole,
            id = focusId,
            actionProvider = actionProvider(
                primaryAction = openContextMenu.takeIf { enabled },
                copyActions = plainTextCopyAction { currentTitle },
            ),
        )
        renderCurrentValue(
            modifier,
            currentValue,
            currentEntry,
        )
    }
}

private sealed interface Option<T> {
    data class Some<T>(val value: T): Option<T>
    data class None<T>(val value: Unit = Unit) : Option<T>
}

private fun <T>T?.toOption() = if (this == null) Option.None() else Option.Some(this)
private fun <T>EditableDropdownEntry<out T>.wrapOption() = EditableDropdownEntry<Option<out T>>(
    value = value.toOption(),
    title = title,
    icon = icon,
    enabled = enabled,
)
private fun <T>EditableDropdownEntry<Unit>.wrapNullOption() = EditableDropdownEntry<Option<T>>(
    value = Option.None(),
    title = title,
    icon = icon,
    enabled = enabled,
)
private fun <T>EditableDropdownEntry<out Option<out T>>.unwrapOption() = (value as? Option.Some)?.let {
    EditableDropdownEntry(
        value = value.value,
        title = title,
        icon = icon,
        enabled = enabled,
    )
}
