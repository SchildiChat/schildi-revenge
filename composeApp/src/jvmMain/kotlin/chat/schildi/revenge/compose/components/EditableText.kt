package chat.schildi.revenge.compose.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.actions.PlaintextEditActions
import chat.schildi.revenge.actions.actionProvider
import chat.schildi.revenge.actions.plainTextCopyAction
import chat.schildi.revenge.compose.destination.conversation.event.message.TextLikeMessageContent
import chat.schildi.revenge.compose.focus.keyFocusable
import com.beeper.android.messageformat.MatrixBodyParseResult
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.hint_not_set

sealed interface EditTextValue {
    val rawText: String
    data class Plain(override val rawText: String) : EditTextValue
    data class AutoFormatted(override val rawText: String, val parsed: MatrixBodyParseResult) : EditTextValue
}

@Composable
fun EditableText(
    editId: Any,
    currentValue: EditTextValue?,
    role: FocusRole,
    persist: suspend (String) -> Result<Unit>,
    modifier: Modifier = Modifier,
    canEdit: Boolean = true,
    renderColor: Color = Color.Unspecified,
    editColors: TextFieldColors = TextFieldDefaults.colors(),
    style: TextStyle = LocalTextStyle.current,
    textAlign: TextAlign? = null,
    emptyFallbackRenderColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    emptyFallbackText: String = stringResource(Res.string.hint_not_set),
    emptyFallbackFontStyle: FontStyle = FontStyle.Italic,
    header: @Composable () -> Unit = {},
) {
    val keyboardActionHandler = LocalKeyboardActionHandler.current
    val isEditing = keyboardActionHandler.activeEditAbleId.collectAsState().value == editId
    val persistInProgress = keyboardActionHandler.editPersistInProgress.collectAsState().value.contains(editId)
    val editState = remember(currentValue) { mutableStateOf<TextFieldValue?>(null) }
    Column(
        modifier.editableTextFocusable(
            editId = editId,
            role = role,
            editState = editState,
            persist = persist,
            canEdit = canEdit,
        ) {
            currentValue?.rawText
        }
    ) {
        header()
        Row {
            if (isEditing && canEdit && !persistInProgress) {
                EditableTextField(
                    value = editState.value
                        ?: (currentValue?.rawText ?: "").let {
                            TextFieldValue(it, TextRange(it.length))
                        },
                    onValueChange = { editState.value = it },
                    colors = editColors,
                    style = style,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                val renderedValue =
                    editState.value?.text?.takeIf { persistInProgress }?.let(EditTextValue::Plain) ?: currentValue
                if (renderedValue?.rawText.isNullOrEmpty()) {
                    EditableTextEmptyDisplay(
                        emptyFallbackText,
                        color = emptyFallbackRenderColor,
                        style = style,
                        textAlign = textAlign,
                        fontStyle = emptyFallbackFontStyle,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    SelectionContainer {
                        EditableTextDisplay(
                            renderedValue,
                            color = renderColor,
                            style = style,
                            textAlign = textAlign,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            // TODO buttons for starting edit, saving and/or cancel? without breaking modifiers and compgoser auto-focus
        }
    }
}

@Composable
private fun Modifier.editableTextFocusable(
    editId: Any,
    canEdit: Boolean,
    role: FocusRole,
    editState: MutableState<TextFieldValue?>,
    persist: suspend (String) -> Result<Unit>,
    accessCurrentValue: () -> String?,
) = keyFocusable(
    role = role,
    actionProvider = actionProvider(
        editActions = if (canEdit) PlaintextEditActions(
            editId = editId,
            editEcho = editState,
            accessPersistedValue = accessCurrentValue,
            persistValue = persist,
        ) else null,
        copyActions = plainTextCopyAction(accessCurrentValue)
    ),
)

@Composable
private fun EditableTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: TextStyle = LocalTextStyle.current,
    colors: TextFieldColors = TextFieldDefaults.colors(),
) {
    // TODO allow passing callback to render error state depending on current value
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }
    TextField(
        value,
        onValueChange = onValueChange,
        modifier = modifier.focusRequester(focusRequester),
        textStyle = style,
        colors = colors,
        enabled = enabled,
    )
}

@Composable
private fun EditableTextDisplay(
    value: EditTextValue,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    textAlign: TextAlign? = null,
) {
    when (value) {
        is EditTextValue.AutoFormatted -> {
            TextLikeMessageContent(
                value.parsed,
                modifier = modifier,
                textColor = color,
                textStyle = style,
                textAlign = textAlign,
            )
        }
        is EditTextValue.Plain -> {
            Text(
                value.rawText,
                modifier = modifier,
                color = color,
                style = style,
                textAlign = textAlign,
            )
        }
    }
}

@Composable
private fun EditableTextEmptyDisplay(
    hint: String,
    modifier: Modifier = Modifier,
    color: Color,
    style: TextStyle,
    fontStyle: FontStyle,
    textAlign: TextAlign? = null,
) {
    Text(
        hint,
        modifier = modifier,
        color = color,
        style = style,
        textAlign = textAlign,
        fontStyle = fontStyle,
    )
}
