package chat.schildi.revenge.compose.command

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.CommandSuggestionsState
import chat.schildi.revenge.actions.InteractionAction
import chat.schildi.revenge.actions.defaultActionProvider
import chat.schildi.revenge.compose.focus.keyFocusable
import kotlinx.collections.immutable.ImmutableList

@Composable
fun CommandSuggestions(
    suggestionsState: CommandSuggestionsState,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextFieldSuggestions(
        suggestions = suggestionsState.currentSuggestions,
        key = { it },
        modifier = modifier,
    ) { suggestion ->
        Text(
            suggestion,
            modifier = Modifier
                .fillMaxWidth()
                .keyFocusable(
                    actionProvider = defaultActionProvider(
                        primaryAction = InteractionAction.Invoke {
                            onSuggestionClick(suggestion)
                            true
                        }
                    )
                )
                .padding(horizontal = Dimens.windowPadding, vertical = Dimens.listPadding)
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun <T>TextFieldSuggestions(
    suggestions: ImmutableList<T>,
    key: ((T) -> Any)? = null,
    modifier: Modifier = Modifier,
    entry: @Composable (T) -> Unit,
) {
    LazyColumn(modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
        items(suggestions, key = key) {
            entry(it)
        }
    }
}
