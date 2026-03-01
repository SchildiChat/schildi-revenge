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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.compose.destination.SplashScreenContent
import chat.schildi.revenge.compose.util.ComposableStringHolder
import chat.schildi.revenge.compose.util.toStringHolder
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.empty_screen_placeholder_search
import shire.composeapp.generated.resources.empty_screen_placeholder_search_in_progress


@Composable
fun EmptyListScreen(
    title: ComposableStringHolder,
    icon: Painter,
    currentSearchTerm: String?,
    renderedSearchTerm: String?,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val isSearchInProgress = (currentSearchTerm ?: "") != (renderedSearchTerm ?: "")
    val isSearching = isSearchInProgress || !currentSearchTerm.isNullOrEmpty()
    EmptyListScreen(
        title = title,
        icon = icon,
        isSearching = isSearching,
        isSearchInProgress = isSearchInProgress,
        isLoading = isLoading,
        modifier = modifier,
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
) {
    if (isSearching) {
        EmptySearchListScreen(
            inProgress = isSearchInProgress,
            modifier = modifier,
        )
    } else if (isLoading) {
        Box(modifier, contentAlignment = Alignment.Center) {
            SplashScreenContent()
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
