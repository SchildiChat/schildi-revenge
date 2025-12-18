package chat.schildi.revenge.compose.command

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.CommandSuggestionsState
import chat.schildi.revenge.actions.InteractionAction
import chat.schildi.revenge.actions.actionProvider
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.theme.scExposures
import kotlinx.collections.immutable.ImmutableList

@Composable
fun CommandSuggestions(
    suggestionsState: CommandSuggestionsState,
    currentSelection: String?,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextFieldSuggestions(
        suggestions = suggestionsState.currentSuggestions,
        key = { it },
        currentSelection = suggestionsState.currentSuggestions.find { it.value == currentSelection },
        onSuggestionClick = { onSuggestionClick(it.value) },
        modifier = modifier,
    ) { suggestion, modifier ->
        Row(
            modifier = modifier,
            horizontalArrangement = Dimens.horizontalArrangement,
        ) {
            Text(
                suggestion.value,
                color = if (currentSelection == suggestion.value) {
                    MaterialTheme.scExposures.accentColor
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                style = Dimens.suggestionsTextStyle,
                modifier = Modifier.weight(0.5f, fill = false),
            )
            if (suggestion.hint != null) {
                Text(
                    suggestion.hint.render(),
                    color = MaterialTheme.colorScheme.tertiary,
                    style = Dimens.suggestionsTextStyle,
                    modifier = Modifier.weight(0.5f, fill = false),
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun <T>TextFieldSuggestions(
    suggestions: ImmutableList<T>,
    key: ((T) -> Any)? = null,
    currentSelection: T?,
    onSuggestionClick: (T) -> Unit,
    modifier: Modifier = Modifier,
    entry: @Composable (T, Modifier) -> Unit,
) {
    val state = androidx.compose.runtime.key(suggestions) { rememberLazyListState() }
    LaunchedEffect(suggestions, currentSelection) {
        val index = suggestions.indexOf(currentSelection)
        if (index != -1) {
            state.scrollToItem(index)
        }
    }
    LazyColumn(
        modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        state = state,
    ) {
        items(suggestions, key = key) { suggestion ->
            entry(
                suggestion,
                Modifier
                    .fillMaxWidth()
                    .keyFocusable(
                        actionProvider = actionProvider(
                            primaryAction = InteractionAction.Invoke {
                                onSuggestionClick(suggestion)
                                true
                            }
                        ),
                        highlight = currentSelection == suggestion,
                    )
                    .padding(horizontal = Dimens.windowPadding, vertical = Dimens.listPadding),
            )
        }
    }
}
