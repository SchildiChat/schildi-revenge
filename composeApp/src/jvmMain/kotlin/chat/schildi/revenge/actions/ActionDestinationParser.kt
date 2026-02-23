package chat.schildi.revenge.actions

import chat.schildi.revenge.Destination
import chat.schildi.revenge.config.keybindings.ActionArgumentPrimitive
import chat.schildi.revenge.config.keybindings.CommandArgContext
import chat.schildi.revenge.config.keybindings.getParameter
import chat.schildi.revenge.util.tryOrNull
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId

// See also NavigationActionDestination for ALLOWED_DESTINATION_STRINGS
internal fun String.toDestinationOrNull(
    args: List<String>,
    context: CommandArgContext?,
) = when (lowercase()) {
    "inbox" -> Destination.Inbox
    "accountmanagement",
    "accounts" -> Destination.AccountManagement
    "chat",
    "conversation",
    "room" -> tryOrNull {
        val sessionId = args.getOrNull(0)?.let(::SessionId) ?: context!!.ensureSessionId()
        val roomId = args.getOrNull(1)?.let(::RoomId) ?: context!!.ensureRoomId()
        Destination.Conversation(sessionId, roomId)
    }
    "members" -> tryOrNull {
        val sessionId = args.getOrNull(0)?.let(::SessionId) ?: context!!.ensureSessionId()
        val roomId = args.getOrNull(1)?.let(::RoomId) ?: context!!.ensureRoomId()
        Destination.RoomMembers(sessionId, roomId)
    }
    "about" -> Destination.About
    "settings" -> Destination.Settings
    else -> null
}

private fun CommandArgContext.ensureSessionId() =
    SessionId(getParameter(ActionArgumentPrimitive.SessionId)!!)
private fun CommandArgContext.ensureRoomId() =
    RoomId(getParameter(ActionArgumentPrimitive.RoomId)!!)
