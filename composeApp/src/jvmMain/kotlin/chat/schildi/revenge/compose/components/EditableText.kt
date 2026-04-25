package chat.schildi.revenge.compose.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.InteractionAction
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.actions.PlaintextEditActions
import chat.schildi.revenge.actions.actionProvider
import chat.schildi.revenge.actions.plainTextCopyAction
import chat.schildi.revenge.compose.destination.conversation.event.message.TextLikeMessageContent
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.revenge.compose.focus.rememberFocusId
import chat.schildi.revenge.config.keybindings.Action
import com.beeper.android.messageformat.MatrixBodyParseResult
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.action_cancel
import shire.composeapp.generated.resources.action_edit
import shire.composeapp.generated.resources.action_save
import shire.composeapp.generated.resources.hint_not_set
import java.util.UUID

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
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
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
    val stableFocusId = rememberFocusId()
    val actionProvider = editableTextActionProvider(
        editId = editId,
        stableFocusId = stableFocusId,
        editState = editState,
        persist = persist,
        canEdit = canEdit,
    ) {
        currentValue?.rawText
    }
    Column(
        modifier.keyFocusable(
            role = role,
            id = stableFocusId,
            actionProvider = actionProvider,
        ),
        horizontalAlignment = horizontalAlignment,
    ) {
        header()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Dimens.horizontalArrangementSmall
        ) {
            if (isEditing && canEdit && !persistInProgress) {
                EditableTextField(
                    value = editState.value
                        ?: (currentValue?.rawText ?: "").let {
                            TextFieldValue(it, TextRange(it.length))
                        },
                    onValueChange = { editState.value = it },
                    colors = editColors,
                    style = style,
                    modifier = Modifier.weight(1f).keyFocusable(
                        role = FocusRole.AUX_ITEM_EDITABLE,
                    ),
                )
                EditableActionIcon(
                    InteractionAction.HandleAction(stableFocusId, Action.PlaintextEditAble.DiscardEdit),
                    Icons.Default.Cancel,
                    contentDescription = stringResource(Res.string.action_cancel),
                )
                EditableActionIcon(
                    InteractionAction.HandleAction(stableFocusId, Action.PlaintextEditAble.SaveEdit),
                    Icons.Default.Check,
                    contentDescription = stringResource(Res.string.action_save),
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
                        modifier = Modifier.weight(1f, fill = false),
                    )
                } else {
                    SelectionContainer(
                        modifier = Modifier.weight(1f, fill = false),
                    ) {
                        EditableTextDisplay(
                            renderedValue,
                            color = renderColor,
                            style = style,
                            textAlign = textAlign,
                        )
                    }
                }
                if (canEdit) {
                    EditableActionIcon(
                        InteractionAction.HandleAction(stableFocusId, Action.PlaintextEditAble.LaunchEdit),
                        Icons.Default.Edit,
                        contentDescription = stringResource(Res.string.action_edit),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun editableTextActionProvider(
    editId: Any,
    stableFocusId: UUID?,
    canEdit: Boolean,
    editState: MutableState<TextFieldValue?>,
    persist: suspend (String) -> Result<Unit>,
    accessCurrentValue: () -> String?,
) = actionProvider(
    editActions = plainTextEditActions(
        editId = editId,
        stableFocusId = stableFocusId,
        canEdit = canEdit,
        editState = editState,
        persist = persist,
        accessCurrentValue = accessCurrentValue,
    ),
    copyActions = plainTextCopyAction(accessCurrentValue)
)

@Composable
private fun plainTextEditActions(
    editId: Any,
    stableFocusId: UUID?,
    canEdit: Boolean,
    editState: MutableState<TextFieldValue?>,
    persist: suspend (String) -> Result<Unit>,
    accessCurrentValue: () -> String?,
) = if (canEdit) remember(
    editId,
    stableFocusId,
    editState,
    accessCurrentValue,
    persist,
) {
    PlaintextEditActions(
        editId = editId,
        stableFocusId = stableFocusId,
        editEcho = editState,
        accessPersistedValue = accessCurrentValue,
        persistValue = persist,
    )
} else null

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

@Composable
private fun EditableActionIcon(
    action: InteractionAction,
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    Icon(
        imageVector,
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.clip(Dimens.squareButtonClip).keyFocusable(
            role = FocusRole.NESTED_AUX_ITEM,
            actionProvider = actionProvider(
                primaryAction = action,
            )
        ).padding(4.dp).size(16.dp)
    )
}
