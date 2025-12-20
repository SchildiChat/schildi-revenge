package chat.schildi.revenge.config.keybindings

enum class ActionArgument {
    Text,
    Boolean,
    Integer,
    Mxid,
    SessionId,
    SessionIdOrIndex,
    RoomId,
    EventId,
    SettingKey,
    NavigatableDestinationName,
}

sealed interface Action {
    val name: String
    val aliases: kotlin.collections.List<String>
    val args: kotlin.collections.List<ActionArgument>
    enum class Global(
        override val aliases: kotlin.collections.List<String> = emptyList(),
        override val args: kotlin.collections.List<ActionArgument> = emptyList()
    ) : Action {
        Search,
        ToggleTheme,
        AutomaticTheme,
        ToggleHiddenItems,
        SetSetting(args = listOf(ActionArgument.SettingKey, ActionArgument.Text)),
        ToggleSetting(args = listOf(ActionArgument.SettingKey)),
        ClearAppMessages,
    }
    enum class Navigation(
        override val aliases: kotlin.collections.List<String> = emptyList(),
        override val args: kotlin.collections.List<ActionArgument> = emptyList()
    ) : Action {
        NavigateCurrent(aliases = listOf("navigate"), args = listOf(ActionArgument.NavigatableDestinationName)),
        NavigateInNewWindow(args = listOf(ActionArgument.NavigatableDestinationName)),
        SplitHorizontal(aliases = listOf("vsplit")),
        SplitVertical(aliases = listOf("split")),
        CloseWindow,
    }
    enum class NavigationItem(
        override val aliases: kotlin.collections.List<String> = emptyList(),
        override val args: kotlin.collections.List<ActionArgument> = emptyList()
    ) : Action {
        NavigateCurrent,
        NavigateInNewWindow,
    }
    enum class Focus(
        override val aliases: kotlin.collections.List<String> = emptyList(),
        override val args: kotlin.collections.List<ActionArgument> = emptyList()
    ) : Action {
        FocusUp,
        FocusDown,
        FocusLeft,
        FocusRight,
        FocusTop,
        FocusCenter,
        FocusBottom,
        FocusParent,
        FocusEnterContainer,
    }
    enum class List(
        override val aliases: kotlin.collections.List<String> = emptyList(),
        override val args: kotlin.collections.List<ActionArgument> = emptyList()
    ) : Action {
        ScrollToTop,
        ScrollToBottom,
        ScrollToStart,
        ScrollToEnd,
    }
    enum class Split(
        override val aliases: kotlin.collections.List<String> = emptyList(),
        override val args: kotlin.collections.List<ActionArgument> = emptyList()
    ) : Action {
        Close,
    }
    enum class Inbox(
        override val aliases: kotlin.collections.List<String> = emptyList(),
        override val args: kotlin.collections.List<ActionArgument> = emptyList()
    ) : Action {
        SetAccountHidden(args = listOf(ActionArgument.SessionIdOrIndex, ActionArgument.Boolean)),
        SetAccountSelected(args = listOf(ActionArgument.SessionIdOrIndex, ActionArgument.Boolean)),
        SetAccountExclusivelySelected(args = listOf(ActionArgument.SessionIdOrIndex, ActionArgument.Boolean)),
        ToggleAccountHidden(args = listOf(ActionArgument.SessionIdOrIndex)),
        ToggleAccountSelected(args = listOf(ActionArgument.SessionIdOrIndex)),
        ToggleAccountExclusivelySelected(args = listOf(ActionArgument.SessionIdOrIndex)),
        NavigateSpaceRelative(args = listOf(ActionArgument.Integer)),
        SelectSpace(args = listOf(ActionArgument.Text)),
    }
    enum class Conversation(
        override val aliases: kotlin.collections.List<String> = emptyList(),
        override val args: kotlin.collections.List<ActionArgument> = emptyList()
    ) : Action {
        FocusComposer,
        HideComposerIfEmpty,
        ComposeMessage,
        ComposeNotice,
        ComposeEmote,
        ComposerSend,
        ComposerInsertAtCursor(args = listOf(ActionArgument.Text)),
        JumpToOwnReadReceipt,
        JumpToFullyRead,
        JumpToBottom,
        MarkUnread,
        MarkRead,
        MarkReadPrivate,
    }
    enum class Event(
        override val aliases: kotlin.collections.List<String> = emptyList(),
        override val args: kotlin.collections.List<ActionArgument> = emptyList()
    ) : Action {
        MarkRead,
        MarkReadPrivate,
        ComposeEdit,
        ComposeReply,
        ComposeReaction,
        CopyContent,
        CopyEventSource,
        CopyEventId,
        CopyMxId,
        CopyContentLink,
        OpenContentLinks,
    }
}
