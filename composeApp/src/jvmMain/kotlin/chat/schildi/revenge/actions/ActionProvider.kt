package chat.schildi.revenge.actions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import chat.schildi.revenge.compose.search.LocalSearchProvider
import chat.schildi.revenge.compose.search.SearchProvider
import chat.schildi.resources.ComposableStringHolder
import chat.schildi.revenge.Destination

data class ActionProvider(
    val searchProvider: SearchProvider?,
    val primaryAction: InteractionAction? = null,
    val secondaryAction: InteractionAction? = null,
    val tertiaryAction: InteractionAction? = null,
    val copyActions: CopyActions? = null,
    val editActions: EditActions? = null,
    val listActions: ListActions? = null,
    val keyActions: KeyboardActionProvider<*>? = null,
    val userIdSuggestionsProvider: UserIdSuggestionsProvider? = null,
    val roomContextSuggestionsProvider: RoomContextSuggestionsProvider? = null,
) {
    inline fun <reified T : InteractionAction>findInteractionAction(condition: (T) -> Boolean = { true }): T? {
        return when {
            (primaryAction as? T)?.let(condition) == true -> primaryAction
            (secondaryAction as? T)?.let(condition) == true -> secondaryAction
            (tertiaryAction as? T)?.let(condition) == true -> tertiaryAction
            else -> null
        }
    }
}

@Composable
fun actionProvider(
    searchProvider: SearchProvider? = LocalSearchProvider.current,
    primaryAction: InteractionAction? = null,
    secondaryAction: InteractionAction? = null,
    tertiaryAction: InteractionAction? = (primaryAction as? InteractionAction.NavigationAction)?.let {
        InteractionAction.OpenWindow(it.initialTitle, it.buildDestination)
    },
    copyActions: CopyActions? = null,
    editActions: EditActions? = null,
    listActions: ListActions? = LocalListActionProvider.current,
    keyActions: KeyboardActionProvider<*>? = LocalKeyboardActionProvider.current,
    userIdSuggestionsProvider: UserIdSuggestionsProvider? = LocalUserIdSuggestionsProvider.current,
    roomContextSuggestionsProvider: RoomContextSuggestionsProvider? = LocalRoomContextSuggestionsProvider.current,
): ActionProvider = remember(
    searchProvider,
    primaryAction,
    secondaryAction,
    tertiaryAction,
    copyActions,
    editActions,
    listActions,
    keyActions,
    userIdSuggestionsProvider,
    roomContextSuggestionsProvider,
) {
    ActionProvider(
        searchProvider = searchProvider,
        primaryAction = primaryAction,
        secondaryAction = secondaryAction,
        tertiaryAction = tertiaryAction,
        copyActions = copyActions,
        editActions = editActions,
        listActions = listActions,
        keyActions = keyActions,
        userIdSuggestionsProvider = userIdSuggestionsProvider,
        roomContextSuggestionsProvider = roomContextSuggestionsProvider,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun buildNavigationActionProvider(
    initialTitle: () -> ComposableStringHolder? = { null },
    searchProvider: SearchProvider? = LocalSearchProvider.current,
    copyActions: CopyActions? = null,
    editActions: EditActions? = null,
    listActions: ListActions? = LocalListActionProvider.current,
    keyActions: KeyboardActionProvider<*>? = LocalKeyboardActionProvider.current,
    userIdSuggestionsProvider: UserIdSuggestionsProvider? = LocalUserIdSuggestionsProvider.current,
    roomContextSuggestionsProvider: RoomContextSuggestionsProvider? = LocalRoomContextSuggestionsProvider.current,
    secondaryAction: InteractionAction? = null,
    buildDestination: () -> Destination,
): ActionProvider {
    return ActionProvider(
        searchProvider = searchProvider,
        primaryAction = InteractionAction.Navigate(
            initialTitle = initialTitle,
            buildDestination = buildDestination,
        ),
        secondaryAction = secondaryAction,
        tertiaryAction = InteractionAction.OpenWindow(
            initialTitle = initialTitle,
            buildDestination = buildDestination,
        ),
        copyActions = copyActions,
        editActions = editActions,
        listActions = listActions,
        keyActions = keyActions,
        userIdSuggestionsProvider = userIdSuggestionsProvider,
        roomContextSuggestionsProvider = roomContextSuggestionsProvider,
    )
}
