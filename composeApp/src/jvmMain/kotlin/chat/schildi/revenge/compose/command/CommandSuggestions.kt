package chat.schildi.revenge.compose.command

import androidx.compose.animation.animateColorAsState
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
import chat.schildi.revenge.actions.defaultActionProvider
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
        modifier = modifier,
    ) { suggestion ->
        Row(Modifier
            .fillMaxWidth()
            .keyFocusable(
                actionProvider = defaultActionProvider(
                    primaryAction = InteractionAction.Invoke {
                        onSuggestionClick(suggestion.value)
                        true
                    }
                )
            )
            .padding(horizontal = Dimens.windowPadding, vertical = Dimens.listPadding),
            horizontalArrangement = Dimens.horizontalArrangement,
        ) {
            Text(
                suggestion.value,
                color = if (currentSelection == suggestion.value) {
                    MaterialTheme.scExposures.accentColor
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(0.5f, fill = false),
            )
            if (suggestion.hint != null) {
                Text(
                    suggestion.hint.render(),
                    color = MaterialTheme.colorScheme.tertiary,
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
    modifier: Modifier = Modifier,
    entry: @Composable (T) -> Unit,
) {
    val state = rememberLazyListState()
    LaunchedEffect(currentSelection) {
        val index = suggestions.indexOf(currentSelection)
        if (index != -1) {
            state.scrollToItem(index)
        }
    }
    LazyColumn(
        modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
        state = state,
    ) {
        items(suggestions, key = key) {
            entry(it)
        }
    }
}
