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
    AllSilent(aliases = listOf("allMessagesSilent")),
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
    BugDescription(consumesTrailingArgsWithSpace = true),
    Ignored,
    Reason(consumesTrailingArgsWithSpace = true),
    Boolean,
    Integer,
    PositiveOffset,
    Float,
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
    PollAnswerId(consumesTrailingArgsWithSpace = true),
    MatrixLink,
    MatrixToLink,
    SchildiChatLegacyLink,
    FileUri,
    AbsoluteFilePath,
    OAuthCallbackPath,
    SpaceOrder,
    SpaceCatchAllMode,
    SpaceCatchAllInviteMode,
    FocusRole,
    DensityPixelsSigned,
    Factor,
    Empty;
    override fun possiblePrimitives(context: CommandArgContext) = listOf(this)
    override fun canHold(primitive: ActionArgumentPrimitive) = primitive == this
}

internal val DeepLink = ActionArgumentAnyOf(
    ActionArgumentPrimitive.MatrixLink,
    ActionArgumentPrimitive.MatrixToLink,
    ActionArgumentPrimitive.SchildiChatLegacyLink,
)

internal val AttachmentUri = ActionArgumentAnyOf(
    ActionArgumentPrimitive.FileUri,
    ActionArgumentPrimitive.AbsoluteFilePath,
)

internal val MxcOrAttachmentUri = ActionArgumentAnyOf(
    ActionArgumentPrimitive.Mxc,
    ActionArgumentPrimitive.FileUri,
    ActionArgumentPrimitive.AbsoluteFilePath,
)

internal val SessionIdOrIndex =
    ActionArgumentAnyOf(ActionArgumentPrimitive.SessionId, ActionArgumentPrimitive.SessionIndex)
internal val SpaceIdSelectable =
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
internal val OptionalBoolean = ActionArgumentOptional(ActionArgumentPrimitive.Boolean)
internal val OptionalSettingValue = ActionArgumentOptional(ActionArgumentPrimitive.SettingValue)
internal val OptionalReason = ActionArgumentOptional(ActionArgumentPrimitive.Reason)
internal val StateKey = ActionArgumentOptional(ActionArgumentPrimitive.NonEmptyStateKey)
internal val ViaServerVararg = ActionArgumentRepeatable(ActionArgumentPrimitive.ServerName)
internal val RoomIdRelaxed = ActionArgumentAnyOf(ActionArgumentPrimitive.RoomId, ActionArgumentPrimitive.SpaceId)

internal val navigationDestinationArgs = listOf(
    ActionArgumentOptional(NavigationDestinationSessionId),
    ActionArgumentOptional(NavigationDestinationResolvableRoom),
    ActionArgumentOptional(NavigationDestinationEventId),
)

internal val navigationArgs = listOf(
    ActionArgumentPrimitive.NavigatableDestination,
) + navigationDestinationArgs

internal val optionalNavigationArgs = listOf(
    ActionArgumentOptional(ActionArgumentPrimitive.NavigatableDestination),
) + navigationDestinationArgs
