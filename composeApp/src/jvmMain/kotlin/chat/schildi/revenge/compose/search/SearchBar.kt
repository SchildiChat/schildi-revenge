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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.compose.components.PlatformBackHandler
import chat.schildi.revenge.compose.components.TopNavigationIcon
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.theme.scExposures
import org.jetbrains.compose.resources.stringResource
import shire.res.generated.resources.Res
import shire.res.generated.resources.action_clear_search
import shire.res.generated.resources.hint_search
import kotlin.uuid.Uuid

@Composable
fun SearchBar(
    searchProvider: SearchProvider?,
    searchFocusContainer: Uuid?,
    modifier: Modifier = Modifier,
    showClearButton: Boolean = false,
) {
    val handler = LocalKeyboardActionHandler.current
    val isFocused = remember { mutableStateOf(false) }
    val searchValue = handler.globalSearchQuery.collectAsState("").value
    val focusManager = LocalFocusManager.current
    PlatformBackHandler(enabled = searchValue.isNotEmpty() || isFocused.value) {
        handler.clearSearch()
        focusManager.clearFocus()
    }
    TextField(
        value = searchValue,
        onValueChange = {
            handler.onSearchType(it, searchProvider, searchFocusContainer)
        },
        label = {
            Text(
                stringResource(Res.string.hint_search),
                color = if (showClearButton) Color.Unspecified else MaterialTheme.scExposures.searchHint,
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .onFocusChanged {
                isFocused.value = it.isFocused
            }
            .keyFocusable(role = FocusRole.SEARCH_BAR),
        singleLine = true,
        keyboardActions = KeyboardActions {
            handler.onSearchEnter(searchProvider, searchFocusContainer)
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
