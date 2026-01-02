package chat.schildi.revenge.compose.destination.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import chat.schildi.preferences.AbstractScPref
import chat.schildi.preferences.LocalScPreferencesStore
import chat.schildi.preferences.ScBoolPref
import chat.schildi.preferences.ScFloatPref
import chat.schildi.preferences.ScIntPref
import chat.schildi.preferences.ScListPref
import chat.schildi.preferences.ScPref
import chat.schildi.preferences.ScPrefCategory
import chat.schildi.preferences.ScPrefCollection
import chat.schildi.preferences.ScPrefContainer
import chat.schildi.preferences.ScPrefScreen
import chat.schildi.preferences.ScStringListPref
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.InteractionAction
import chat.schildi.revenge.compose.components.ContextMenuEntry
import chat.schildi.revenge.compose.components.WithContextMenu
import chat.schildi.revenge.compose.components.keyboardShortcutFromIndex
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.revenge.compose.focus.rememberFocusId
import chat.schildi.revenge.config.keybindings.Action
import chat.schildi.theme.scExposures
import co.touchlab.kermit.Logger
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

fun LazyListScope.renderPref(
    pref: AbstractScPref,
    isVeryFirst: Boolean = true,
    isFirst: Boolean = true,
) {
    when (pref) {
        is ScPref<*> -> {
            item(key = pref.sKey) {
                pref.AutoRendered()
            }
        }
        is ScPrefContainer -> {
            if (!isVeryFirst) {
                when (pref) {
                    is ScPrefCategory -> {
                        item {
                            ScPrefCategoryHeader(
                                title = stringResource(pref.titleRes),
                                isFirst = isFirst,
                            )
                        }
                    }
                    is ScPrefScreen -> {
                        item {
                            ScPrefCategoryHeader(
                                title = stringResource(pref.titleRes),
                                isFirst = isFirst,
                                color = MaterialTheme.scExposures.accentColor,
                                style = MaterialTheme.typography.headlineMedium,
                            )
                        }
                    }
                    is ScPrefCollection -> {}
                }
            }
            pref.prefs.forEachIndexed { index, childPref ->
                renderPref(childPref, isFirst = index == 0, isVeryFirst = false)
            }
        }
    }
}

@Composable
fun <T>ScPref<T>.AutoRendered() {
    when (this) {
        is ScBoolPref -> Rendered()
        is ScFloatPref -> Rendered()
        is ScIntPref -> Rendered()
        is ScStringListPref -> Rendered()
    }
}

@Composable
private fun <T>ScPref<T>.persistSettingValue(): (T) -> Unit {
    val scope = rememberCoroutineScope()
    return { value ->
        scope.launch {
            LocalScPreferencesStore.current.setSetting(this@persistSettingValue, value)
        }
    }
}

@Composable
fun ScPref<Boolean>.Rendered() {
    val onCheckedChange = persistSettingValue()
    ScPrefLayout(
        clickAction = {
            InteractionAction.Invoke {
                onCheckedChange(!it)
                true
            }
        }
    ) { value, enabled ->
        Switch(
            checked = value,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

@Composable
fun <T> ScPref<T>.RenderedWithTextField() {
    ScPrefLayout { value, enabled ->
        var textFieldValue by remember { mutableStateOf(value.toString()) }
        var isError by remember { mutableStateOf(false) }
        val persistValue = persistSettingValue()
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = {
                textFieldValue = it
                val parsed = parseType(it)
                if (parsed == null) {
                    isError = true
                } else {
                    isError = false
                    persistValue(parsed)
                }
            },
            isError = isError,
            enabled = enabled,
            modifier = Modifier.keyFocusable(
                role = FocusRole.TEXT_FIELD_SINGLE_LINE,
            ),
            singleLine = true,
            maxLines = 1,
        )
    }
}

@Composable
fun ScIntPref.Rendered() = RenderedWithTextField()

@Composable
fun ScFloatPref.Rendered() = RenderedWithTextField()

@Composable
fun <T> ScListPref<T>.Rendered() {
    val focusId = rememberFocusId()
    WithContextMenu(
        focusId = focusId,
        entries = remember(items) {
            items.mapIndexed { index, item ->
                ContextMenuEntry(
                    title = item.name,
                    action = Action.Global.SetSetting,
                    actionArgs = persistentListOf(sKey, item.value.toString()),
                    keyboardShortcut = index.keyboardShortcutFromIndex(),
                )
            }.toPersistentList()
        },
    ) { openContextMenu ->
        ScPrefLayout(
            focusId = focusId,
            valueToString = { value ->
                val item = items.find { it.value == value }
                if (item == null) {
                    Logger.withTag("ScListPref.Rendered").e("Failed to look up item name for value $value")
                    value.toString()
                } else {
                    item.name.render()
                }
            },
            selectionAsSummary = true,
            clickAction = { openContextMenu },
        )
    }
}
