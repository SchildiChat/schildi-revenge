package chat.schildi.revenge.compose.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.compose.components.TopNavigationIcon
import chat.schildi.revenge.compose.focus.keyFocusable
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.action_clear_search
import shire.composeapp.generated.resources.hint_search

@Composable
fun SearchBar(modifier: Modifier = Modifier, showClearButton: Boolean = false) {
    val handler = LocalKeyboardActionHandler.current
    TextField(
        value = handler.searchQuery.collectAsState("").value,
        onValueChange = {
            handler.onSearchType(it)
        },
        label = { Text(stringResource(Res.string.hint_search)) },
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .keyFocusable(role = FocusRole.SEARCH_BAR),
        maxLines = 1,
        keyboardActions = KeyboardActions {
            handler.onSearchEnter()
        },
        colors = TextFieldDefaults.colors().copy(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
        trailingIcon = if (showClearButton) {{
            TopNavigationIcon(
                Icons.Default.Clear,
                stringResource(Res.string.action_clear_search)
            ) {
                handler.clearSearch()
            }
        }} else null,
    )
}
