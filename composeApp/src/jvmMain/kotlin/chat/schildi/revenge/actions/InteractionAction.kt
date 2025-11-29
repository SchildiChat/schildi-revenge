package chat.schildi.revenge.actions

import chat.schildi.revenge.navigation.ComposableStringHolder
import chat.schildi.revenge.navigation.Destination

sealed interface InteractionAction {

    sealed interface NavigationAction : InteractionAction {
        val initialTitle: ComposableStringHolder?
        val buildDestination: () -> Destination
    }

    data class NavigateCurrent(
        override val initialTitle: ComposableStringHolder? = null,
        override val buildDestination: () -> Destination,
    ) : NavigationAction

    data class OpenWindow(
        override val initialTitle: ComposableStringHolder? = null,
        override val buildDestination: () -> Destination,
    ) : NavigationAction

}
