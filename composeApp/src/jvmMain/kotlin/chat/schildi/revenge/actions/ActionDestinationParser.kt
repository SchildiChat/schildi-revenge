package chat.schildi.revenge.actions

import chat.schildi.revenge.Destination
import chat.schildi.revenge.config.keybindings.ActionArgumentPrimitive
import chat.schildi.revenge.config.keybindings.CommandArgContext
import chat.schildi.revenge.config.keybindings.DestinationEnum
import chat.schildi.revenge.config.keybindings.getParameter
import chat.schildi.revenge.util.tryOrNull
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId

fun String.toDestinationEnum(): DestinationEnum? {
    val destinationCheck = lowercase()
    return DestinationEnum.entries.firstOrNull {
        it.matches(destinationCheck)
    }
}

internal fun String.toDestinationOrNull(
    args: List<String>,
    context: CommandArgContext?,
): Destination? {
    return when (this.toDestinationEnum()) {
        DestinationEnum.Splash -> Destination.Splash
        DestinationEnum.AccountManagement -> Destination.AccountManagement
        DestinationEnum.Inbox -> Destination.Inbox
        DestinationEnum.Conversation -> {
            tryOrNull {
                val sessionId = args.getOrNull(0)?.let(::SessionId) ?: context!!.ensureSessionId()
                val roomId = args.getOrNull(1)?.let(::RoomId) ?: context!!.ensureRoomId()
                Destination.Conversation(sessionId, roomId)
            }
        }
        DestinationEnum.RoomMembers -> {
            tryOrNull {
                val sessionId = args.getOrNull(0)?.let(::SessionId) ?: context!!.ensureSessionId()
                val roomId = args.getOrNull(1)?.let(::RoomId) ?: context!!.ensureRoomId()
                Destination.RoomMembers(sessionId, roomId)
            }
        }
        DestinationEnum.MessageReactions -> {
            tryOrNull {
                val sessionId = args.getOrNull(0)?.let(::SessionId) ?: context!!.ensureSessionId()
                val roomId = args.getOrNull(1)?.let(::RoomId) ?: context!!.ensureRoomId()
                val eventId = args.getOrNull(2)?.let(::EventId) ?: context!!.ensureEventId()
                Destination.MessageReactions(sessionId, roomId, eventId)
            }
        }
        DestinationEnum.Settings -> Destination.Settings
        DestinationEnum.About -> Destination.About
        // Not navigatable destinations
        DestinationEnum.SplitHorizontal,
        DestinationEnum.SplitVertical,
        null -> null
    }
}

private fun CommandArgContext.ensureSessionId() =
    SessionId(getParameter(ActionArgumentPrimitive.SessionId)!!)
private fun CommandArgContext.ensureRoomId() =
    RoomId(getParameter(ActionArgumentPrimitive.RoomId)!!)
private fun CommandArgContext.ensureEventId() =
    EventId(getParameter(ActionArgumentPrimitive.EventId)!!)
