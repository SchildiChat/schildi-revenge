package chat.schildi.revenge.config.keybindings

sealed interface ActionArgument {
    val name: String
    fun canHold(primitive: ActionArgumentPrimitive): Boolean
}

data class ActionArgumentOptional(val argument: ActionArgument) : ActionArgument {
    override val name: String
        get() = "[$argument]"
    override fun canHold(primitive: ActionArgumentPrimitive) = argument == primitive
}
data class ActionArgumentAnyOf(val arguments: List<ActionArgumentPrimitive>) : ActionArgument {
    constructor(vararg arguments: ActionArgumentPrimitive) : this(arguments.toList())
    override val name: String
        get() = "any of: ${arguments.joinToString { it.name }}"
    override fun canHold(primitive: ActionArgumentPrimitive) = arguments.contains(primitive)
}
sealed interface ActionArgumentContextBased : ActionArgument {
    fun getFor(context: CommandArgContext): ActionArgument
}

enum class ActionArgumentPrimitive : ActionArgument {
    Text,
    Reason,
    Boolean,
    Integer,
    UserId,
    UserIdInRoom,
    UserIdNotInRoom,
    SessionId,
    SessionIndex,
    RoomId,
    EventId,
    SettingKey,
    SettingValue,
    NavigatableDestinationName,
    SpaceId,
    SpaceSelectionId,
    SpaceIndex,
    Empty;
    override fun canHold(primitive: ActionArgumentPrimitive) = primitive == this
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
private val OptionalSettingValue = ActionArgumentOptional(ActionArgumentPrimitive.SettingValue)
private val OptionalReason = ActionArgumentOptional(ActionArgumentPrimitive.Reason)

private val navigationArgs = listOf(
    ActionArgumentPrimitive.NavigatableDestinationName,
    ActionArgumentOptional(NavigationDestinationSessionId),
    ActionArgumentOptional(NavigationDestinationRoomId)
)

fun Action.handlesCommand(command: String): Boolean {
    return command == name || command == name.lowercase() || command in aliases
}

fun Action.minArgsSize() = args.count { it !is ActionArgumentOptional }

sealed interface Action {
    val name: String
    val aliases: kotlin.collections.List<String>
    val args: kotlin.collections.List<ActionArgument>
    enum class Global(
        override val aliases: kotlin.collections.List<String> = emptyList(),
        override val args: kotlin.collections.List<ActionArgument> = emptyList()
    ) : Action {
        Search,
        Command,
        SetSetting(args = listOf(ActionArgumentPrimitive.SettingKey, OptionalSettingValue)),
        ToggleSetting(args = listOf(ActionArgumentPrimitive.SettingKey, OptionalSettingValue, OptionalSettingValue)),
        ClearAppMessages,
        ConfirmActionAppMessage,
        Exit(aliases = listOf("quit"))
    }
    enum class Navigation(
        override val aliases: kotlin.collections.List<String> = emptyList(),
        override val args: kotlin.collections.List<ActionArgument> = emptyList()
    ) : Action {
        NavigateCurrent(aliases = listOf("navigate", "nav"), args = navigationArgs),
        NavigateInNewWindow(aliases = listOf("open-new", "window"), args = navigationArgs),
        SplitHorizontal(aliases = listOf("vsplit")),
        SplitVertical(aliases = listOf("split")),
        CloseWindow(aliases = listOf("close")),
    }
    enum class NavigationItem(
        override val aliases: kotlin.collections.List<String> = emptyList(),
        override val args: kotlin.collections.List<ActionArgument> = emptyList()
    ) : Action {
        NavigateCurrent(aliases = listOf("follow", "open")),
        NavigateInNewWindow(aliases = listOf("open-new")),
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
        Unsplit,
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
        ClearComposer,
        ComposeMessage,
        ComposeNotice,
        ComposeEmote,
        ComposerSend,
        ComposerInsertAtCursor(args = listOf(ActionArgumentPrimitive.Text)),
        ComposerPasteAttachment,
        ComposerAddAttachment,
        JumpToOwnReadReceipt,
        JumpToFullyRead,
        JumpToBottom,
        MarkUnread,
        MarkRead,
        MarkReadPrivate,
        KickUser(aliases = listOf("kick"), args = listOf(ActionArgumentPrimitive.UserIdInRoom, OptionalReason)),
        InviteUser(aliases = listOf("invite"), args = listOf(ActionArgumentPrimitive.UserIdNotInRoom)),
        BanUser(aliases = listOf("ban"), args = listOf(ActionArgumentPrimitive.UserIdInRoom, OptionalReason)),
        UnbanUser(aliases = listOf("unban"), args = listOf(ActionArgumentPrimitive.UserIdNotInRoom, OptionalReason)),
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
