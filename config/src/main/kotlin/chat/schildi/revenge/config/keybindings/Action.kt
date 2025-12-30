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
    EventType,
    StateEventType,
    NonEmptyStateKey,
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
private val StateKey = ActionArgumentOptional(ActionArgumentPrimitive.NonEmptyStateKey)

private val navigationArgs = listOf(
    ActionArgumentPrimitive.NavigatableDestinationName,
    ActionArgumentOptional(NavigationDestinationSessionId),
    ActionArgumentOptional(NavigationDestinationRoomId)
)

fun Action.handlesCommand(command: String): Boolean {
    val lowerCommand = command.lowercase()
    return lowerCommand == name.lowercase() || lowerCommand in aliases.map { it.lowercase() }
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
        Exit(aliases = listOf("quit"))
    }
    enum class AppMessage(
        override val aliases: kotlin.collections.List<String> = emptyList(),
        override val args: kotlin.collections.List<ActionArgument> = emptyList()
    ) : Action {
        ClearAppMessages,
        ConfirmActionAppMessage,
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
        OpenContextMenu,
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
        SetAccountMuted(args = listOf(SessionIdOrIndex, OptionalBoolean)),
        ToggleAccountHidden(args = listOf(SessionIdOrIndex)),
        ToggleAccountSelected(args = listOf(SessionIdOrIndex)),
        ToggleAccountExclusivelySelected(args = listOf(SessionIdOrIndex)),
        ToggleAccountMuted(args = listOf(SessionIdOrIndex)),
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
        ComposeCustomEvent(args = listOf(ActionArgumentPrimitive.EventType), aliases = listOf("sendEvent")),
        ComposeCustomStateEvent(args = listOf(ActionArgumentPrimitive.StateEventType, StateKey), aliases = listOf("sendState")),
        ComposerSend,
        ComposerInsertAtCursor(args = listOf(ActionArgumentPrimitive.Text)),
        ComposerPasteAttachment,
        ComposerAddAttachment,
        ComposerSuggestionFocusNext,
        ComposerSuggestionFocusPrevious,
        ComposerSuggestionApplySelected,
        JumpToOwnReadReceipt,
        JumpToFullyRead,
        JumpToBottom,
        MarkRead,
        MarkReadPrivate,
        MarkFullyRead,
        KickUser(aliases = listOf("kick"), args = listOf(ActionArgumentPrimitive.UserIdInRoom, OptionalReason)),
        InviteUser(aliases = listOf("invite"), args = listOf(ActionArgumentPrimitive.UserIdNotInRoom)),
        BanUser(aliases = listOf("ban"), args = listOf(ActionArgumentPrimitive.UserIdInRoom, OptionalReason)),
        UnbanUser(aliases = listOf("unban"), args = listOf(ActionArgumentPrimitive.UserIdNotInRoom, OptionalReason)),
        CopyFullRoomState(aliases = listOf("roomState")),
    }
    enum class Room(
        override val aliases: kotlin.collections.List<String> = emptyList(),
        override val args: kotlin.collections.List<ActionArgument> = emptyList()
    ) : Action {
        MarkFavorite(aliases = listOf("favorite", "SetIsFavorite"), args = listOf(OptionalBoolean)),
        MarkLowPriority(aliases = listOf("lowprio", "SetIsLowPriority"), args = listOf(OptionalBoolean)),
        ToggleIsFavorite,
        ToggleIsLowPriority,
        MarkRoomUnread(aliases = listOf("SetUnread", "MarkUnread"), args = listOf(OptionalBoolean)),
        ClearUnreadFlag,
        MarkRoomRead, // Different than timeline-based MarkRead
        MarkRoomReadPrivate, // Different than timeline-based MarkRead
        MarkRoomFullyRead, // Different than timeline-based MarkRead
        Leave(aliases = listOf("part")),
    }
    enum class Event(
        override val aliases: kotlin.collections.List<String> = emptyList(),
        override val args: kotlin.collections.List<ActionArgument> = emptyList()
    ) : Action {
        MarkRead,
        MarkReadPrivate,
        MarkFullyRead,
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
