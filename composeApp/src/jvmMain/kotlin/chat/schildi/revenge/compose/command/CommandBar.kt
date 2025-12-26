package chat.schildi.revenge.compose.command

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import chat.schildi.revenge.actions.CurrentCommandValidity
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.theme.scExposures
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.hint_command

@Composable
fun CommandBar(modifier: Modifier = Modifier) {
    val handler = LocalKeyboardActionHandler.current
    val (state, suggestionsState) = handler.commandSuggestionsState.collectAsState().value ?: return
    Column(modifier) {
        if (suggestionsState != null) {
            CommandSuggestions(
                suggestionsState = suggestionsState,
                currentSelection = state.selectedSuggestion,
                onSuggestionClick = { suggestion ->
                    handler.applyCommandSuggestion(state, suggestion)
                },
                modifier = Modifier.heightIn(max = 200.dp)
            )
        }
        val containerColor = animateColorAsState(
            when (suggestionsState?.validity) {
                CurrentCommandValidity.VALID -> MaterialTheme.scExposures.accentColor.copy(alpha = 0.1f)
                CurrentCommandValidity.INVALID -> MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                CurrentCommandValidity.INCOMPLETE,
                null -> Color.Transparent
            }
        ).value
        TextField(
            value = state.query,
            onValueChange = {
                handler.updateCommandInput(it)
            },
            label = { Text(stringResource(Res.string.hint_command)) },
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .keyFocusable(role = FocusRole.COMMAND_BAR),
            maxLines = 1,
            keyboardActions = KeyboardActions {
                handler.onCommandEnter()
            },
            colors = TextFieldDefaults.colors().copy(
                focusedContainerColor = containerColor,
                unfocusedContainerColor = containerColor,
            )
        )
    }
}
