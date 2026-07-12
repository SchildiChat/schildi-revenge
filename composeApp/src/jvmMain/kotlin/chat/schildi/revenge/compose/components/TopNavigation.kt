package chat.schildi.revenge.compose.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import chat.schildi.lib.preferences.ScPrefs
import chat.schildi.revenge.preferences.value
import chat.schildi.revenge.Anim
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.NavigationPreference
import chat.schildi.revenge.UiState
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.compose.search.LocalSearchProvider
import chat.schildi.revenge.compose.search.SearchBar
import chat.schildi.revenge.compose.search.SearchProvider
import org.jetbrains.compose.resources.stringResource
import shire.res.generated.resources.Res
import shire.res.generated.resources.action_close
import shire.res.generated.resources.action_open_inbox
import shire.res.generated.resources.hint_search
import kotlin.uuid.Uuid

@Composable
fun TopNavigation(content: @Composable RowScope.() -> Unit) {
    val visible = !ScPrefs.MINIMAL_MODE.value()
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(tween(Anim.DURATION)) { -it } +
                expandVertically(tween(Anim.DURATION), expandFrom = Alignment.Top),
        exit = slideOutVertically(tween(Anim.DURATION)) { -it } +
                shrinkVertically(tween(Anim.DURATION), shrinkTowards = Alignment.Top),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            content()
        }
    }
}

@Composable
fun TopNavigationIcon(
    imageVector: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    TopNavigationIcon(rememberVectorPainter(imageVector), contentDescription, modifier, onClick)
}

@Composable
fun TopNavigationIcon(
    painter: Painter,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    WithTooltip(contentDescription, modifier) {
        IconButton(
            onClick = onClick,
        ) {
            Icon(painter, contentDescription)
        }
    }
}

@Composable
fun TopNavigationCloseOrNavigateToInboxIcon(modifier: Modifier = Modifier) {
    val hasInboxOpen = UiState.hasInboxOpen.collectAsState().value
    val destinationState = LocalDestinationState.current
    val keyHandler = LocalKeyboardActionHandler.current
    val showInboxIcon = destinationState != null && !hasInboxOpen
    AnimatedContent(showInboxIcon) {
        if (it) {
            TopNavigationIcon(
                Icons.Default.Inbox,
                stringResource(Res.string.action_open_inbox),
                modifier,
            ) {
                destinationState?.navigate(UiState.getInboxDestination(), NavigationPreference.REPLACE)
            }
        } else {
            TopNavigationIcon(
                Icons.Default.Close,
                stringResource(Res.string.action_close),
                modifier,
            ) {
                destinationState?.closeScreen(keyHandler)
            }
        }
    }
}

@Composable
fun RowScope.TopNavigationTitle(title: String, modifier: Modifier = Modifier) {
    AnimatedContent(
        title,
        modifier.weight(1f),
        transitionSpec = {
            fadeIn(
                animationSpec = Dimens.tweenSmooth()
            ) togetherWith fadeOut(
                animationSpec = Dimens.tweenSmooth()
            )
        },
    ) { title ->
        Text(
            title,
            Modifier.padding(Dimens.windowPadding),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
fun RowScope.TopNavigationSearchOrTitle(
    title: String,
    modifier: Modifier = Modifier,
    searchProvider: SearchProvider = LocalSearchProvider.current ?: run {
        throw IllegalStateException("Called TopNavigationSearchOrTitle without search provider")
    },
    searchFocusContainer: Uuid? = null,
) {
    val keyHandler = LocalKeyboardActionHandler.current
    val searchBarVisible = keyHandler.needsKeyboardSearchBar(searchProvider).collectAsState().value
    AnimatedContent(
        searchBarVisible,
        modifier.weight(1f),
        transitionSpec = { fadeIn(Dimens.tween()) togetherWith fadeOut(Dimens.tween()) }
    ) { searchBarVisible ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (searchBarVisible) {
                SearchBar(
                    searchProvider,
                    searchFocusContainer,
                    Modifier.weight(1f),
                    showClearButton = true,
                )
            } else {
                TopNavigationTitle(title, Modifier.weight(1f))
                TopNavigationIcon(
                    Icons.Default.Search,
                    stringResource(Res.string.hint_search)
                ) {
                    keyHandler.onSearchEnter(searchProvider, searchFocusContainer, "")
                }
            }
        }
    }
}
