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
)

@Composable
fun defaultActionProvider(): ActionProvider {
    val searchProvider = LocalSearchProvider.current
    val listActions = LocalListActionProvider.current
    return ActionProvider(
        searchProvider = searchProvider,
        listActions = listActions,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun buildNavigationActionProvider(
    initialTitle: ComposableStringHolder? = null,
    buildDestination: () -> Destination,
): ActionProvider {
    val searchProvider = LocalSearchProvider.current
    val listActions = LocalListActionProvider.current
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
    )
}
