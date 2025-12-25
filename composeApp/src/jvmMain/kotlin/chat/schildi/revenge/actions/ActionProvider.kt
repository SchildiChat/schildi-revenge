package chat.schildi.revenge.actions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import chat.schildi.revenge.compose.search.LocalSearchProvider
import chat.schildi.revenge.compose.search.SearchProvider
import chat.schildi.revenge.compose.util.ComposableStringHolder
import chat.schildi.revenge.Destination

data class ActionProvider(
    val searchProvider: SearchProvider?,
    val primaryAction: InteractionAction? = null,
    val secondaryAction: InteractionAction? = null,
    val tertiaryAction: InteractionAction? = null,
    val listActions: ListAction? = null,
    val keyActions: KeyboardActionProvider<*>? = null,
)

@Composable
fun defaultActionProvider(
    searchProvider: SearchProvider? = LocalSearchProvider.current,
    primaryAction: InteractionAction? = null,
    secondaryAction: InteractionAction? = null,
    tertiaryAction: InteractionAction? = null,
    listActions: ListAction? = LocalListActionProvider.current,
    keyActions: KeyboardActionProvider<*>? = LocalKeyboardActionProvider.current,
): ActionProvider {
    return ActionProvider(
        searchProvider = searchProvider,
        primaryAction = primaryAction,
        secondaryAction = secondaryAction,
        tertiaryAction = tertiaryAction,
        listActions = listActions,
        keyActions = keyActions,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun buildNavigationActionProvider(
    initialTitle: ComposableStringHolder? = null,
    searchProvider: SearchProvider? = LocalSearchProvider.current,
    listActions: ListAction? = LocalListActionProvider.current,
    keyActions: KeyboardActionProvider<*>? = LocalKeyboardActionProvider.current,
    buildDestination: () -> Destination,
): ActionProvider {
    return ActionProvider(
        searchProvider = searchProvider,
        primaryAction = InteractionAction.NavigateCurrent(
            initialTitle = initialTitle,
            buildDestination = buildDestination,
        ),
        tertiaryAction = InteractionAction.OpenWindow(
            initialTitle = initialTitle,
            buildDestination = buildDestination,
        ),
        listActions = listActions,
        keyActions = keyActions,
    )
}
