package chat.schildi.revenge.actions

import chat.schildi.revenge.Destination
import chat.schildi.revenge.util.tryOrNull
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId

// See also NavigationActionDestination for ALLOWED_DESTINATION_STRINGS
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
