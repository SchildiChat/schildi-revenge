package chat.schildi.revenge.config.keybindings

sealed interface ActionArgument {
    val name: String
    val consumesTrailingArgsWithSpace: Boolean
    fun possiblePrimitives(context: CommandArgContext): List<ActionArgumentPrimitive>
    fun canHold(primitive: ActionArgumentPrimitive): Boolean
}

data class ActionArgumentOptional(val argument: ActionArgument) : ActionArgument {
    override val name: String
        get() = "[$argument]"
    override val consumesTrailingArgsWithSpace = argument.consumesTrailingArgsWithSpace
    override fun possiblePrimitives(context: CommandArgContext) = argument.possiblePrimitives(context)
    override fun canHold(primitive: ActionArgumentPrimitive) = argument.canHold(primitive)
}

data class ActionArgumentRepeatable(val argument: ActionArgument) : ActionArgument {
    override val name: String
        get() = "[$argument...]"
    override val consumesTrailingArgsWithSpace = true
    override fun possiblePrimitives(context: CommandArgContext) = argument.possiblePrimitives(context)
    override fun canHold(primitive: ActionArgumentPrimitive) = argument.canHold(primitive)
}

data class ActionArgumentAnyOf(val arguments: List<ActionArgumentPrimitive>) : ActionArgument {
    constructor(vararg arguments: ActionArgumentPrimitive) : this(arguments.toList())
    override val name: String
        get() = "(${arguments.joinToString("|") { it.name }})"
    override val consumesTrailingArgsWithSpace = arguments.any { it.consumesTrailingArgsWithSpace }
    override fun possiblePrimitives(context: CommandArgContext) = arguments.flatMap { it.possiblePrimitives(context) }.distinct()
    override fun canHold(primitive: ActionArgumentPrimitive) = arguments.any { it.canHold(primitive) }
}

sealed interface ActionArgumentContextBased : ActionArgument {
    fun getFor(context: CommandArgContext): ActionArgument
    override fun possiblePrimitives(context: CommandArgContext) = getFor(context).possiblePrimitives(context)
}

enum class ActionRoomNotificationSetting(val aliases: List<String> = emptyList()) {
    Default(aliases = listOf("followDefault")),
    All(aliases = listOf("allMessages")),
    Mentions(aliases = listOf("mentionsAndKeywords")),
    Mute(aliases = listOf("none"));
    companion object {
        fun tryResolve(value: String): ActionRoomNotificationSetting? {
            val lower = value.lowercase()
            return entries.firstOrNull {
                it.name.lowercase() == lower || it.aliases.any { it.lowercase() == lower }
            }
        }
    }
}

enum class ActionArgumentPrimitive(override val consumesTrailingArgsWithSpace: Boolean = false) : ActionArgument {
    Text,
    Reason(consumesTrailingArgsWithSpace = true),
    Boolean,
    Integer,
    PositiveOffset,
    Json(consumesTrailingArgsWithSpace = true),
    PowerLevel,
    Index,
    UserId,
    UserIdInRoom,
    UserIdNotInRoom,
    ExistingDmUserId,
    SessionId,
    SessionIndex,
    RoomId,
    RoomIdNotJoined,
    RoomAlias,
    RoomAliasNotJoined,
    EventId,
    ThreadId,
    Mxc,
    ServerName,
    SettingKey,
    SettingValue,
    SettingCategory,
    NavigatableDestination,
    DestinationName,
    SpaceId,
    PseudoSpaceId,
    ParentSpaceId, // For current room contexts, a space that is currently already parent of that room
    NonParentSpaceId, // For current room contexts, a space that is *not* currently already parent of that room
    SpaceSelectionId,
    SpaceIndex,
    EventType,
    AccountDataType,
    RoomAccountDataType,
    StateEventType,
    NonEmptyStateKey,
    UserName(consumesTrailingArgsWithSpace = true),
    RoomName(consumesTrailingArgsWithSpace = true),
    RoomTopic(consumesTrailingArgsWithSpace = true),
    RoomNotificationSetting,
    MatrixLink,
    MatrixToLink,
    SchildiChatLegacyLink,
    SpaceOrder,
    SpaceCatchAllMode,
    FocusRole,
    Empty;
    override fun possiblePrimitives(context: CommandArgContext) = listOf(this)
    override fun canHold(primitive: ActionArgumentPrimitive) = primitive == this
}

private val DeepLink = ActionArgumentAnyOf(
    ActionArgumentPrimitive.MatrixLink,
    ActionArgumentPrimitive.MatrixToLink,
    ActionArgumentPrimitive.SchildiChatLegacyLink,
)

private val SessionIdOrIndex =
    ActionArgumentAnyOf(ActionArgumentPrimitive.SessionId, ActionArgumentPrimitive.SessionIndex)
private val SpaceIdSelectable =
    ActionArgumentAnyOf(
        ActionArgumentPrimitive.SpaceId,
        ActionArgumentPrimitive.PseudoSpaceId,
        ActionArgumentPrimitive.SpaceSelectionId,
        ActionArgumentPrimitive.SpaceIndex
    )
internal val ResolvableRoom = ActionArgumentAnyOf(
    ActionArgumentPrimitive.RoomId,
    ActionArgumentPrimitive.RoomAlias,
    ActionArgumentPrimitive.ExistingDmUserId,
)
internal val UnjoinedRoom = ActionArgumentAnyOf(
    ActionArgumentPrimitive.RoomIdNotJoined,
    ActionArgumentPrimitive.RoomAliasNotJoined,
)
private val OptionalBoolean = ActionArgumentOptional(ActionArgumentPrimitive.Boolean)
private val OptionalSettingValue = ActionArgumentOptional(ActionArgumentPrimitive.SettingValue)
private val OptionalReason = ActionArgumentOptional(ActionArgumentPrimitive.Reason)
private val StateKey = ActionArgumentOptional(ActionArgumentPrimitive.NonEmptyStateKey)
private val ViaServerVararg = ActionArgumentRepeatable(ActionArgumentPrimitive.ServerName)

private val navigationDestinationArgs = listOf(
    ActionArgumentOptional(NavigationDestinationSessionId),
    ActionArgumentOptional(NavigationDestinationResolvableRoom),
    ActionArgumentOptional(NavigationDestinationEventId),
)

private val navigationArgs = listOf(
    ActionArgumentPrimitive.NavigatableDestination,
) + navigationDestinationArgs

private val optionalNavigationArgs = listOf(
    ActionArgumentOptional(ActionArgumentPrimitive.NavigatableDestination),
) + navigationDestinationArgs

fun Action.handlesCommand(command: String): Boolean {
    val lowerCommand = command.lowercase()
    return lowerCommand == name.lowercase() || lowerCommand in aliases.map { it.lowercase() }
}

fun Action.minArgsSize() = args.count { it !is ActionArgumentOptional && it !is ActionArgumentRepeatable }
fun Action.maxArgsSize() = if (args.lastOrNull()?.consumesTrailingArgsWithSpace == true) Int.MAX_VALUE else args.size

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
        SetSetting(args = listOf(ActionArgumentPrimitive.SettingKey, OptionalSettingValue), aliases = listOf("set")),
        ResetSetting(args = listOf(ActionArgumentPrimitive.SettingKey), aliases = listOf("reset")),
        ToggleSetting(args = listOf(ActionArgumentPrimitive.SettingKey, OptionalSettingValue, OptionalSettingValue, OptionalSettingValue, OptionalSettingValue, OptionalSettingValue, OptionalSettingValue, OptionalSettingValue, OptionalSettingValue), aliases = listOf("toggle")),
        Exit(aliases = listOf("quit")),
        SetMinimized(args = listOf(ActionArgumentOptional(ActionArgumentPrimitive.Boolean)), aliases = listOf("minimize")),
        ToggleMinimized,
        RecreateUi,
        RecreateWindow,
        ClearSessionCache(args = listOf(ActionArgumentPrimitive.SessionId)),
        CopyGlobalAccountData(args = listOf(ActionArgumentPrimitive.SessionId), aliases = listOf("copyAccountData", "copyGlobalAccountData")),
        ViewGlobalAccountData(args = listOf(ActionArgumentPrimitive.SessionId), aliases = listOf("viewAccountData", "viewGlobalAccountData")),
        SetGlobalAccountData(args = listOf(ActionArgumentPrimitive.SessionId, ActionArgumentPrimitive.AccountDataType, ActionArgumentOptional(ActionArgumentPrimitive.Json)), aliases = listOf("setAccountData")),
        VacuumDatabase(args = listOf(ActionArgumentOptional(ActionArgumentPrimitive.SessionId))),
        CreateRoom(args = listOf(ActionArgumentPrimitive.SessionId, ActionArgumentPrimitive.RoomName), aliases = listOf("createRoom")),
        CreateUnencryptedRoom(args = listOf(ActionArgumentPrimitive.SessionId, ActionArgumentPrimitive.RoomName), aliases = listOf("createUnencryptedRoom", "createRoomUnencrypted")),
        CreateDm(args = listOf(ActionArgumentPrimitive.SessionId, ActionArgumentPrimitive.UserId), aliases = listOf("createDm", "startDm")),
        CreateSpace(args = listOf(ActionArgumentPrimitive.SessionId, ActionArgumentPrimitive.RoomName), aliases = listOf("createSpace")),
        AutoSubscribeNotifiableRooms, // Experimental feature
        InspectFocusable(aliases = listOf("inspect")),
        Join(args = listOf(ActionArgumentPrimitive.SessionId, UnjoinedRoom, ViaServerVararg)),
        VerifyUser(args = listOf(ActionArgumentPrimitive.SessionId, ActionArgumentPrimitive.UserId)),
        ConsumeLink(args = listOf(DeepLink)),
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
        NavigateAuto(args = navigationArgs),
        NavigateInNewWindow(aliases = listOf("open", "window"), args = navigationArgs),
        SplitHorizontal(aliases = listOf("split", "vsplit"), args = optionalNavigationArgs),
        SplitVertical(aliases = listOf("hsplit"), args = optionalNavigationArgs),
        CloseWindow(aliases = listOf("close")),
        CloseWindowUnlessLast,
    }
    enum class NavigationItem(
        override val aliases: kotlin.collections.List<String> = emptyList(),
        override val args: kotlin.collections.List<ActionArgument> = emptyList()
    ) : Action {
        NavigateCurrent(aliases = listOf("navigate", "nav")),
        NavigateInNewWindow(aliases = listOf("open", "window")),
    }
    enum class CopyAble(
        override val aliases: kotlin.collections.List<String> = emptyList(),
        override val args: kotlin.collections.List<ActionArgument> = emptyList()
    ) : Action {
        CopyPlaintext(aliases = listOf("copy")),
        ViewPlaintext(aliases = listOf("viewPlaintext")),
        CopyUserId(aliases = listOf("copyUserId", "copyMXID")),
        CopyMxcUrl(aliases = listOf("copyMxc")),
        CopyFilePath(aliases = listOf("copyPath")),
    }
    enum class PlaintextEditAble(
        override val aliases: kotlin.collections.List<String> = emptyList(),
        override val args: kotlin.collections.List<ActionArgument> = emptyList()
    ) : Action {
        LaunchEdit(aliases = listOf("edit")),
        DiscardEdit(aliases = listOf("discard")),
        SaveEdit(aliases = listOf("save")),
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
        FocusNextSplit,
        FocusVisibleListStart,
        FocusVisibleListEnd,
        FocusVisibleListTop,
        FocusVisibleListBottom,
        FocusByRole(args = listOf(ActionArgumentPrimitive.FocusRole)),
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
        UnsplitDestination(args = listOf(ActionArgumentPrimitive.DestinationName)),
        SwapSplit(aliases = listOf("swap")),
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
        SelectSpaceIfNotHidden(args = listOf(
            ActionArgumentAnyOf(
                ActionArgumentPrimitive.SpaceId,
                ActionArgumentPrimitive.PseudoSpaceId,
                ActionArgumentPrimitive.SpaceSelectionId,
            )
        )),
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
        ComposerPasteText,
        ComposerPasteAttachment,
        ComposerAddAttachment,
        ComposerSuggestionFocusNext,
        ComposerSuggestionFocusPrevious,
        ComposerSuggestionApplySelected,
        JumpToOwnReadReceipt,
        JumpToFullyRead,
        JumpToBottom,
        MarkTimelineRead,
        MarkTimelineReadPrivate,
        MarkTimelineFullyRead,
        KickUser(aliases = listOf("kick"), args = listOf(ActionArgumentPrimitive.UserIdInRoom, OptionalReason)),
        InviteUser(aliases = listOf("invite"), args = listOf(ActionArgumentPrimitive.UserIdNotInRoom)),
        BanUser(aliases = listOf("ban"), args = listOf(ActionArgumentPrimitive.UserIdInRoom, OptionalReason)),
        UnbanUser(aliases = listOf("unban"), args = listOf(ActionArgumentPrimitive.UserIdNotInRoom, OptionalReason)),
        InviteOrKickUser(aliases = listOf("kickOrInvite"), args = listOf(ActionArgumentPrimitive.UserId, OptionalReason)),
        CopyFullRoomState(aliases = listOf("roomState", "copyRoomState")),
        ViewFullRoomState(aliases = listOf("viewRoomState")),
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
        Join,
        Leave(aliases = listOf("part")),
        CopyRoomId,
        CopyRoomMatrixToLink(aliases = listOf("copyRoomLink", "copyMatrixToLink")),
        ClearEventCache(aliases = listOf("ClearRoomCache")),
        SetRoomUserDisplayName(aliases = listOf("myroomnick"), args = listOf(ActionArgumentOptional(ActionArgumentPrimitive.UserName))),
        SetRoomNotifications(args = listOf(ActionArgumentPrimitive.RoomNotificationSetting)),
        AddToSpace(args = listOf(ActionArgumentPrimitive.NonParentSpaceId), aliases = listOf("assignToSpace")),
        RemoveFromSpace(args = listOf(ActionArgumentPrimitive.ParentSpaceId), aliases = listOf("unassignFromSpace")),
        ToggleRoomInSpace(args = listOf(ActionArgumentPrimitive.SpaceId)),
        SetRoomName(args = listOf(ActionArgumentOptional(ActionArgumentPrimitive.RoomName))),
        SetRoomTopic(args = listOf(ActionArgumentOptional(ActionArgumentPrimitive.RoomTopic))),
        SetRoomAvatar(args = listOf(ActionArgumentOptional(ActionArgumentPrimitive.Mxc))),
        CopyFullRoomAccountData(aliases = listOf("roomAccountData", "copyRoomAccountData")),
        ViewFullRoomAccountData(aliases = listOf("viewRoomAccountData")),
        SetRoomAccountData(args = listOf(ActionArgumentPrimitive.RoomAccountDataType, ActionArgumentOptional(ActionArgumentPrimitive.Json)), aliases = listOf("setRoomAccountData")),
        ConvertToDm,
        ConvertToGroup,
        SetPowerLevel(args = listOf(ActionArgumentPrimitive.UserIdInRoom, ActionArgumentPrimitive.PowerLevel)),
        SetPrivateRoomName(args = listOf(ActionArgumentOptional(ActionArgumentPrimitive.RoomName))),
    }
    enum class Space(
        override val aliases: kotlin.collections.List<String> = emptyList(),
        override val args: kotlin.collections.List<ActionArgument> = emptyList()
    ) : Action {
        CopySortOrder(aliases = listOf("CopySpaceOrder")),
        SetSortOrder(args = listOf(ActionArgumentOptional(ActionArgumentPrimitive.SpaceOrder))),
        SetCatchAll(args = listOf(ActionArgumentOptional(ActionArgumentAnyOf(ActionArgumentPrimitive.SpaceCatchAllMode, ActionArgumentPrimitive.Boolean)))),
    }
    enum class Event(
        override val aliases: kotlin.collections.List<String> = emptyList(),
        override val args: kotlin.collections.List<ActionArgument> = emptyList()
    ) : Action {
        MarkEventRead,
        MarkEventReadPrivate,
        MarkEventFullyRead,
        ComposeEdit,
        ComposeReply,
        ComposeReaction,
        CopyContent,
        CopyFormattedBody,
        CopyEventSource,
        ViewEventSource(aliases = listOf("viewEventSource")),
        CopyEventId,
        CopyMxc,
        CopyContentLink,
        OpenContentLinks,
        CopyEventMatrixToLink(aliases = listOf("copyMessageLink")),
        FollowMatrixToLink(args = listOf(ActionArgumentOptional(ActionArgumentPrimitive.PositiveOffset))),
        Redact(aliases = listOf("rm", "del", "delete")),
        JumpToRepliedTo,
        DownloadFile,
        DownloadFileAndOpen,
        ToggleReactionKey(args = listOf(ActionArgumentPrimitive.Text), aliases = listOf("react")),
        ToggleReactionIndex(args = listOf(ActionArgumentPrimitive.Index)),
        RetrySend,
        ExpandDetails(aliases = listOf("revealDetails", "expand", "revealSpoiler", "showSpoiler")),
        CollapseDetails(aliases = listOf("collapseDetails", "collapse", "hideSpoiler")),
        ToggleDetails(aliases = listOf("toggleDetails", "toggleSpoiler")),
        Pin,
        Unpin,
    }
    enum class User(
        override val aliases: kotlin.collections.List<String> = emptyList(),
        override val args: kotlin.collections.List<ActionArgument> = emptyList()
    ) : Action {
        CopyUserMatrixToLink(aliases = listOf("copyUserLink")),
    }
}
