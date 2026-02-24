package chat.schildi.revenge.config.keybindings

val SUGGESTED_DESTINATION_STRINGS = listOf(
    "inbox",
    "accounts",
    "room",
    "about",
    "settings",
)

// See also ActionDestinationParser for mappings
val ALLOWED_DESTINATION_STRINGS = listOf(
    "inbox",
    "accountmanagement",
    "accounts",
    "chat",
    "conversation",
    "room",
    "about",
    "settings",
    "members",
    "reactions",
)

fun String.destinationRequiresSessionId() = this in listOf("chat", "conversation", "room", "members", "reactions")
fun String.destinationRequiresRoomId() = this in listOf("chat", "conversation", "room", "members", "reactions")
fun String.destinationRequiresEventId() = this in listOf("reactions")

data object NavigationDestinationSessionId : ActionArgumentContextBased {
    override val name: String = javaClass.simpleName
    override val consumesTrailingArgsWithSpace = false
    override fun canHold(primitive: ActionArgumentPrimitive) = primitive == ActionArgumentPrimitive.SessionId
    override fun getFor(context: CommandArgContext): ActionArgument {
        val destinations = context.findAll(ActionArgumentPrimitive.NavigatableDestinationName)
        val enabled = destinations.any { it.destinationRequiresSessionId() }
        return if (enabled) {
            ActionArgumentPrimitive.SessionId
        } else {
            ActionArgumentPrimitive.Empty
        }
    }
}

data object NavigationDestinationRoomId : ActionArgumentContextBased {
    override val name: String = javaClass.simpleName
    override val consumesTrailingArgsWithSpace = false
    override fun canHold(primitive: ActionArgumentPrimitive) = primitive == ActionArgumentPrimitive.RoomId
    override fun getFor(context: CommandArgContext): ActionArgument {
        val destinations = context.findAll(ActionArgumentPrimitive.NavigatableDestinationName)
        val enabled = destinations.any { it.destinationRequiresRoomId() }
        return if (enabled) {
            ActionArgumentPrimitive.RoomId
        } else {
            ActionArgumentPrimitive.Empty
        }
    }
}

data object NavigationDestinationEventId : ActionArgumentContextBased {
    override val name: String = javaClass.simpleName
    override val consumesTrailingArgsWithSpace = false
    override fun canHold(primitive: ActionArgumentPrimitive) = primitive == ActionArgumentPrimitive.EventId
    override fun getFor(context: CommandArgContext): ActionArgument {
        val destinations = context.findAll(ActionArgumentPrimitive.NavigatableDestinationName)
        val enabled = destinations.any { it.destinationRequiresEventId() }
        return if (enabled) {
            ActionArgumentPrimitive.EventId
        } else {
            ActionArgumentPrimitive.Empty
        }
    }
}
