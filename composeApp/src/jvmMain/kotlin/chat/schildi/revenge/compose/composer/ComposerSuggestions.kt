package chat.schildi.revenge.compose.composer

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.compose.command.TextFieldSuggestions
import chat.schildi.revenge.model.ComposerSuggestion
import chat.schildi.revenge.model.ComposerSuggestionsState

@Composable
fun ComposerSuggestions(
    suggestionsState: ComposerSuggestionsState,
    onSuggestionClick: (ComposerSuggestion) -> Unit,
    modifier: Modifier = Modifier
) {
    TextFieldSuggestions(
        suggestions = suggestionsState.suggestions,
        key = { it },
        currentSelection = suggestionsState.selectedSuggestion,
        onSuggestionClick = onSuggestionClick,
        modifier = modifier,
    ) { suggestion, modifier ->
        Row(
            modifier = modifier,
            horizontalArrangement = Dimens.horizontalArrangement,
        ) {
            Text(
                suggestion.value,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(0.5f, fill = false),
            )
            suggestion.hint?.let { hint ->
                Text(
                    hint.render(),
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(0.5f, fill = false),
                )
            }
        }
    }
}
