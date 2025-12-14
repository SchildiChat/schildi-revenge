package chat.schildi.revenge.actions

import chat.schildi.revenge.compose.util.ComposableStringHolder
import chat.schildi.revenge.Destination

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

    data class Invoke(
        val invoke: () -> Boolean,
    ) : InteractionAction

}
