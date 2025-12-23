package chat.schildi.revenge.config.keybindings

sealed interface ActionArgument {
    val name: String
}

data class ActionArgumentOptional(val argument: ActionArgument) : ActionArgument {
    override val name: String
        get() = "[$argument]"
}
data class ActionArgumentAnyOf(val arguments: List<ActionArgumentPrimitive>) : ActionArgument {
    constructor(vararg arguments: ActionArgumentPrimitive) : this(arguments.toList())
    override val name: String
        get() = "any of: ${arguments.joinToString { it.name }}"
}

enum class ActionArgumentPrimitive : ActionArgument {
    Text,
    Boolean,
    Integer,
    Mxid,
    SessionId,
    SessionIndex,
    RoomId,
    EventId,
    SettingKey,
    NavigatableDestinationName,
    SpaceId,
    SpaceSelectionId,
    SpaceIndex,
}

private val SessionIdOrIndex =
    ActionArgumentAnyOf(ActionArgumentPrimitive.SessionId, ActionArgumentPrimitive.SessionIndex)
private val SpaceIdSelectable =
    ActionArgumentAnyOf(
        ActionArgumentPrimitive.SpaceId,
        ActionArgumentPrimitive.SpaceSelectionId,
        ActionArgumentPrimitive.SpaceIndex
    )
private val OptionalBoolean = ActionArgumentOptional(ActionArgumentPrimitive.Boolean)

private val navigationArgs = listOf(
    ActionArgumentPrimitive.NavigatableDestinationName,
    ActionArgumentOptional(ActionArgumentPrimitive.SessionId),
    ActionArgumentOptional(ActionArgumentPrimitive.RoomId)
)

sealed interface Action {
    val name: String
    val aliases: kotlin.collections.List<String>
    val args: kotlin.collections.List<ActionArgument>
    enum class Global(
        override val aliases: kotlin.collections.List<String> = emptyList(),
        override val args: kotlin.collections.List<ActionArgument> = emptyList()
    ) : Action {
        Search,
        SetSetting(args = listOf(ActionArgumentPrimitive.SettingKey, ActionArgumentPrimitive.Text)),
        ToggleSetting(args = listOf(ActionArgumentPrimitive.SettingKey)),
        ClearAppMessages,
        ConfirmActionAppMessage,
    }
    enum class Navigation(
        override val aliases: kotlin.collections.List<String> = emptyList(),
        override val args: kotlin.collections.List<ActionArgument> = emptyList()
    ) : Action {
        NavigateCurrent(aliases = listOf("navigate"), args = navigationArgs),
        NavigateInNewWindow(args = navigationArgs),
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
        SetAccountHidden(args = listOf(SessionIdOrIndex, OptionalBoolean)),
        SetAccountSelected(args = listOf(SessionIdOrIndex, OptionalBoolean)),
        SetAccountExclusivelySelected(args = listOf(SessionIdOrIndex, OptionalBoolean)),
        ToggleAccountHidden(args = listOf(SessionIdOrIndex)),
        ToggleAccountSelected(args = listOf(SessionIdOrIndex)),
        ToggleAccountExclusivelySelected(args = listOf(SessionIdOrIndex)),
        NavigateSpaceRelative(args = listOf(ActionArgumentPrimitive.Integer)),
        SelectSpace(args = listOf(SpaceIdSelectable)),
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
        ComposerInsertAtCursor(args = listOf(ActionArgumentPrimitive.Text)),
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
        Redact(aliases = listOf("rm", "del", "delete")),
    }
}
