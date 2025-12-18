package chat.schildi.revenge.compose.composer

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.compose.command.TextFieldSuggestions
import chat.schildi.revenge.model.ComposerEmojiSuggestion
import chat.schildi.revenge.model.ComposerSuggestion
import chat.schildi.revenge.model.ComposerSuggestionsState
import chat.schildi.theme.scExposures

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
                color = if (suggestionsState.selectedSuggestion == suggestion) {
                    MaterialTheme.scExposures.accentColor
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                style = if (suggestion is ComposerEmojiSuggestion)
                    Dimens.emojiSuggestionsTextStyle
                else
                    Dimens.suggestionsTextStyle,
                modifier = Modifier.weight(0.5f, fill = false),
            )
            suggestion.hint?.let { hint ->
                Text(
                    hint.render(),
                    color = MaterialTheme.colorScheme.tertiary,
                    style = Dimens.suggestionsTextStyle,
                    modifier = Modifier.weight(0.5f, fill = false),
                )
            }
        }
    }
}
