package chat.schildi.revenge.actions

import chat.schildi.revenge.Destination
import chat.schildi.revenge.util.tryOrNull
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId

internal val SUGGESTED_DESTINATION_STRINGS = listOf(
    "inbox",
    "accounts",
    "room",
)

internal val ALLOWED_DESTINATION_STRINGS = listOf(
    "inbox",
    "accountmanagement",
    "accounts",
    "chat",
    "conversation",
    "room",
)

internal fun String.toDestinationOrNull(args: List<String>) = when (lowercase()) {
    "inbox" -> Destination.Inbox
    "accountmanagement",
    "accounts" -> Destination.AccountManagement
    "chat",
    "conversation",
    "room" -> if (args.size == 2) tryOrNull {
        Destination.Conversation(SessionId(args[0]), RoomId(args[1]))
    } else null
    else -> null
}
