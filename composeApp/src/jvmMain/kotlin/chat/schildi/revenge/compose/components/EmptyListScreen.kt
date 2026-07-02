package chat.schildi.revenge.compose.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.compose.destination.SplashScreenContent
import chat.schildi.revenge.compose.search.LocalSearchProvider
import chat.schildi.revenge.compose.search.SearchProvider
import chat.schildi.resources.ComposableStringHolder
import chat.schildi.resources.toStringHolder
import chat.schildi.revenge.model.LoadState
import kotlinx.coroutines.flow.StateFlow
import shire.res.generated.resources.Res
import shire.res.generated.resources.empty_screen_placeholder_search
import shire.res.generated.resources.empty_screen_placeholder_search_in_progress

@Composable
fun EmptyListScreen(
    title: ComposableStringHolder,
    icon: Painter,
    renderedSearchTerm: String?,
    isLoading: Boolean = false,
    searchProvider: SearchProvider = LocalSearchProvider.current ?: run {
        throw IllegalStateException("No search provider for EmptyListScreen with rendered search term detection")
    },
    modifier: Modifier = Modifier,
    loadState: StateFlow<LoadState>? = null,
) {
    val currentSearchTerm = LocalKeyboardActionHandler.current
        .searchQueryForDestination(searchProvider).collectAsState(null).value
    val isSearchInProgress = (currentSearchTerm ?: "") != (renderedSearchTerm ?: "")
    val isSearching = isSearchInProgress || !currentSearchTerm.isNullOrEmpty()
    EmptyListScreen(
        title = title,
        icon = icon,
        isSearching = isSearching,
        isSearchInProgress = isSearchInProgress,
        isLoading = isLoading,
        modifier = modifier,
        loadState = loadState,
    )
}

@Composable
fun EmptyListScreen(
    title: ComposableStringHolder,
    icon: Painter,
    isSearching: Boolean,
    isSearchInProgress: Boolean = false,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
    loadState: StateFlow<LoadState>? = null,
) {
    if (isSearching) {
        EmptySearchListScreen(
            inProgress = isSearchInProgress,
            modifier = modifier,
        )
    } else if (isLoading) {
        Box(modifier, contentAlignment = Alignment.Center) {
            SplashScreenContent(loadState = loadState)
        }
    } else {
        EmptyListScreen(
            title = title,
            icon = icon,
            modifier = modifier,
        )
    }
}

@Composable
fun EmptySearchListScreen(
    modifier: Modifier = Modifier,
    inProgress: Boolean = false,
) {
    EmptyListScreen(
        title = if (inProgress)
            Res.string.empty_screen_placeholder_search_in_progress.toStringHolder()
        else
            Res.string.empty_screen_placeholder_search.toStringHolder(),
        icon = rememberVectorPainter(Icons.Default.Search),
        modifier = modifier,
    )
}

@Composable
fun EmptyListScreen(
    title: ComposableStringHolder,
    icon: Painter,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Dimens.verticalArrangement,
        ) {
            Icon(
                icon,
                null,
                Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                title.render(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
