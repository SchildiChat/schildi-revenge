package chat.schildi.revenge.actions

import chat.schildi.resources.ComposableStringHolder
import chat.schildi.revenge.Destination
import chat.schildi.revenge.compose.components.ContextMenuEntry
import chat.schildi.revenge.config.keybindings.Action
import kotlinx.collections.immutable.ImmutableList
import java.util.UUID

sealed interface InteractionAction {

    sealed interface NavigationAction : InteractionAction {
        val initialTitle: () -> ComposableStringHolder?
        val buildDestination: () -> Destination
    }

    data class Navigate(
        override val initialTitle: () -> ComposableStringHolder? = { null },
        override val buildDestination: () -> Destination,
    ) : NavigationAction

    data class OpenWindow(
        override val initialTitle: () -> ComposableStringHolder? = { null },
        override val buildDestination: () -> Destination,
    ) : NavigationAction

    data class Invoke(
        val invoke: () -> Boolean,
    ) : InteractionAction

    data class HandleAction(
        val focusId: UUID,
        val action: Action,
        val args: List<String> = emptyList(),
    ) : InteractionAction

    data class CopyToClipboard(
        val text: String,
    ) : InteractionAction

    data class OpenInBrowser(
        val url: String,
    ) : InteractionAction

    data class ContextMenu(
        val focusId: UUID,
        val entries: ImmutableList<ContextMenuEntry>?, // null for custom data types and rendering in the popup
        val menuId: UUID = focusId,
        val parentMenuId: UUID? = null,
    ) : InteractionAction

}
