package chat.schildi.revenge.config.keybindings

import kotlinx.serialization.Serializable

@Serializable
data class KeybindingConfig(
    // TODO sensible defaults?
    val global: List<Binding<Action.Global>> = emptyList(),
    val appMessage: List<Binding<Action.AppMessage>> = emptyList(),
    val navigation: List<Binding<Action.Navigation>> = emptyList(),
    val navigationItem: List<Binding<Action.NavigationItem>> = emptyList(),
    val focus: List<Binding<Action.Focus>> = emptyList(),
    val list: List<Binding<Action.List>> = emptyList(),
    val split: List<Binding<Action.Split>> = emptyList(),
    val inbox: List<Binding<Action.Inbox>> = emptyList(),
    val room: List<Binding<Action.Room>> = emptyList(),
    val conversation: List<Binding<Action.Conversation>> = emptyList(),
    val event: List<Binding<Action.Event>> = emptyList(),
)
