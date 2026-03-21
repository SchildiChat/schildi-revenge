package chat.schildi.revenge.config.keybindings

enum class DestinationEnum(
    val destName: String,
    val aliases: List<String> = emptyList(),
) {
    Splash("Splash"),
    AccountManagement("AccountManagement", listOf("accounts", "accountmanagement")),
    Inbox("Inbox", listOf("inbox")),
    Conversation("Conversation", listOf("room", "conversation", "chat")),
    RoomMembers("RoomMembers", listOf("members")),
    MessageReactions("MessageReactions", listOf("reactions")),
    MessageReadReceipts("MessageReadReceipts", listOf("readReceipts", "receipts")),
    ConversationDetailsSplit("ConversationDetailsSplit", listOf("chatDetails", "conversationDetails")),
    Settings("Settings", listOf("settings")),
    SettingsPane("SettingsPane"),
    About("About", listOf("about")),
    SplitHorizontal("SplitHorizontal"),
    SplitVertical("SplitVertical"),
    SplitPlaceholder("SplitPlaceholder"),
    InboxConversationSplit("InboxConversationSplit", listOf("home"));

    fun allDestinationNames() = aliases + destName
    fun matches(destinationName: String): Boolean {
        val destinationCheck = destinationName.lowercase()
        return allDestinationNames().any { it.lowercase() == destinationCheck }
    }
}

val SUGGESTED_DESTINATION_STRINGS = listOf(
    DestinationEnum.Inbox.aliases[0],
    DestinationEnum.AccountManagement.aliases[0],
    DestinationEnum.Conversation.aliases[0],
    DestinationEnum.About.aliases[0],
    DestinationEnum.Settings.aliases[0],
    DestinationEnum.InboxConversationSplit.aliases[0],
    DestinationEnum.ConversationDetailsSplit.aliases[0],
)

val ALLOWED_DESTINATION_STRINGS = listOf(
    DestinationEnum.Inbox.allDestinationNames(),
    DestinationEnum.AccountManagement.allDestinationNames(),
    DestinationEnum.Conversation.allDestinationNames(),
    DestinationEnum.About.allDestinationNames(),
    DestinationEnum.Settings.allDestinationNames(),
    DestinationEnum.RoomMembers.allDestinationNames(),
    DestinationEnum.MessageReactions.allDestinationNames(),
    DestinationEnum.MessageReadReceipts.allDestinationNames(),
    DestinationEnum.InboxConversationSplit.allDestinationNames(),
    DestinationEnum.ConversationDetailsSplit.allDestinationNames(),
).flatten()

fun String.destinationRequiresSessionId() = this in listOf("chat", "conversation", "room", "members", "reactions", "chatDetails", "conversationDetails")
fun String.destinationRequiresResolvableRoom() = this in listOf("chat", "conversation", "room", "members", "reactions", "chatDetails", "conversationDetails")
fun String.destinationRequiresEventId() = this in listOf("reactions")

data object NavigationDestinationSessionId : ActionArgumentContextBased {
    override val name: String = javaClass.simpleName
    override val consumesTrailingArgsWithSpace = false
    override fun canHold(primitive: ActionArgumentPrimitive) = primitive == ActionArgumentPrimitive.SessionId
    override fun getFor(context: CommandArgContext): ActionArgument {
        val destinations = context.findAll(ActionArgumentPrimitive.NavigatableDestination)
        val enabled = destinations.any { it.destinationRequiresSessionId() }
        return if (enabled) {
            ActionArgumentPrimitive.SessionId
        } else {
            ActionArgumentPrimitive.Empty
        }
    }
}

data object NavigationDestinationResolvableRoom : ActionArgumentContextBased {
    override val name: String = javaClass.simpleName
    override val consumesTrailingArgsWithSpace = false
    override fun canHold(primitive: ActionArgumentPrimitive) = primitive == ActionArgumentPrimitive.RoomId ||
            primitive == ActionArgumentPrimitive.ExistingDmUserId ||
            primitive == ActionArgumentPrimitive.SessionId
    override fun getFor(context: CommandArgContext): ActionArgument {
        val destinations = context.findAll(ActionArgumentPrimitive.NavigatableDestination)
        val enabled = destinations.any { it.destinationRequiresResolvableRoom() }
        return if (enabled) {
            ResolvableRoom
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
        val destinations = context.findAll(ActionArgumentPrimitive.NavigatableDestination)
        val enabled = destinations.any { it.destinationRequiresEventId() }
        return if (enabled) {
            ActionArgumentPrimitive.EventId
        } else {
            ActionArgumentPrimitive.Empty
        }
    }
}
