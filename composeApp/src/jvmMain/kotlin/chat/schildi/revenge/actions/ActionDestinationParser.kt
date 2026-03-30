package chat.schildi.revenge.actions

import chat.schildi.preferences.ScPrefs
import chat.schildi.revenge.Destination
import chat.schildi.revenge.UiState
import chat.schildi.revenge.config.keybindings.ActionArgumentPrimitive
import chat.schildi.revenge.config.keybindings.CommandArgContext
import chat.schildi.revenge.config.keybindings.DestinationEnum
import chat.schildi.revenge.config.keybindings.getParameter
import chat.schildi.revenge.util.tryOrNull
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.MatrixPatterns
import io.element.android.libraries.matrix.api.core.RoomAlias
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.core.UserId
import kotlin.jvm.optionals.getOrNull

fun String.toDestinationEnum(): DestinationEnum? {
    val destinationCheck = lowercase()
    return DestinationEnum.entries.firstOrNull {
        it.matches(destinationCheck)
    }
}

private suspend fun resolveRoomId(sessionId: SessionId, arg: String): RoomId? {
    return when {
        MatrixPatterns.isRoomId(arg) -> RoomId(arg)
        MatrixPatterns.isRoomAlias(arg) -> UiState.currentClientFor(sessionId)?.resolveRoomAlias(RoomAlias(arg))?.getOrNull()?.getOrNull()?.roomId
        MatrixPatterns.isUserId(arg) -> UiState.currentClientFor(sessionId)?.findDM(UserId(arg))?.getOrNull()
        else -> null
    }
}

private const val FAKE_ROOM_ID = "!fake_room_id"
private fun resolveFakableRoomId(sessionId: SessionId, arg: String): RoomId? {
    return when {
        MatrixPatterns.isRoomId(arg) -> RoomId(arg)
        // Fallback to "assume we'll be able to resolve these in a coroutine with client access" or sth.
        MatrixPatterns.isRoomAlias(arg) -> RoomId(FAKE_ROOM_ID)
        MatrixPatterns.isUserId(arg) -> RoomId(FAKE_ROOM_ID)
        else -> null
    }
}

fun String.verifyConstructableDestination(
    args: List<String>,
    context: CommandArgContext?,
): Destination? = toDestinationOrNull(
    args = args,
    context = context,
    resolveRoomId = ::resolveFakableRoomId,
)

suspend fun String.toDestinationOrNull(
    args: List<String>,
    context: CommandArgContext?,
): Destination? = toDestinationOrNull(
    args = args,
    context = context,
) { sessionId, arg ->
    resolveRoomId(sessionId, arg)
}

/**
 * Inline so we can do suspend and non-suspend versions of this, where the non-suspend one skips possible room ID
 * lookups but just assumes they'll be fine for destination validity verification purposes.
 */
private inline fun String.toDestinationOrNull(
    args: List<String>,
    context: CommandArgContext?,
    resolveRoomId: (SessionId, String) -> RoomId?,
): Destination? {
    return when (this.toDestinationEnum()) {
        DestinationEnum.Splash -> Destination.Splash
        DestinationEnum.AccountManagement -> Destination.AccountManagement
        DestinationEnum.Inbox -> Destination.Inbox
        DestinationEnum.Conversation -> {
            tryOrNull {
                val sessionId = args.getOrNull(0)?.let(::SessionId) ?: context!!.ensureSessionId()
                val roomId = args.getOrNull(1)?.let { resolveRoomId(sessionId, it) } ?: context!!.ensureRoomId()
                Destination.Conversation(sessionId, roomId)
            }
        }
        DestinationEnum.ConversationDetailsSplit -> {
            tryOrNull {
                val sessionId = args.getOrNull(0)?.let(::SessionId) ?: context!!.ensureSessionId()
                val roomId = args.getOrNull(1)?.let { resolveRoomId(sessionId, it) } ?: context!!.ensureRoomId()
                Destination.ConversationDetailsMultiPane(Destination.Conversation(sessionId, roomId))
            }
        }
        DestinationEnum.RoomMembers -> {
            tryOrNull {
                val sessionId = args.getOrNull(0)?.let(::SessionId) ?: context!!.ensureSessionId()
                val roomId = args.getOrNull(1)?.let { resolveRoomId(sessionId, it) } ?: context!!.ensureRoomId()
                Destination.RoomMembers(sessionId, roomId)
            }
        }
        DestinationEnum.MessageReactions -> {
            tryOrNull {
                val sessionId = args.getOrNull(0)?.let(::SessionId) ?: context!!.ensureSessionId()
                val roomId = args.getOrNull(1)?.let { resolveRoomId(sessionId, it) } ?: context!!.ensureRoomId()
                val eventId = args.getOrNull(2)?.let(::EventId) ?: context!!.ensureEventId()
                Destination.MessageReactions(sessionId, roomId, eventId)
            }
        }
        DestinationEnum.MessageReadReceipts -> {
            tryOrNull {
                val sessionId = args.getOrNull(0)?.let(::SessionId) ?: context!!.ensureSessionId()
                val roomId = args.getOrNull(1)?.let { resolveRoomId(sessionId, it) } ?: context!!.ensureRoomId()
                val eventId = args.getOrNull(2)?.let(::EventId) ?: context!!.ensureEventId()
                Destination.MessageReadReceipts(sessionId, roomId, eventId)
            }
        }
        DestinationEnum.UserDetails -> {
            tryOrNull {
                val sessionId = args.getOrNull(0)?.let(::SessionId) ?: context!!.ensureSessionId()
                val userId = args.getOrNull(1)?.let(::UserId) ?: context!!.ensureUserId()
                Destination.UserDetails(sessionId, userId)
            }
        }
        DestinationEnum.Settings -> Destination.Settings()
        DestinationEnum.SettingsRoot -> Destination.SettingsPane()
        DestinationEnum.SettingsDetails -> {
            val rootPref = args.getOrNull(0)
            if (rootPref == null) {
                null
            } else if (ScPrefs.validCategoryKeys.contains(rootPref)) {
                Destination.SettingsPane(rootPref)
            } else {
                null
            }
        }
        DestinationEnum.Diagnostics -> Destination.Diagnostics
        DestinationEnum.About -> Destination.About
        DestinationEnum.InboxConversationSplit -> Destination.InboxConversationMultiPane()
        // Destinations not reachable via "navigate" action
        DestinationEnum.SplitConversationPlaceholder,
        DestinationEnum.SplitRoomDetailsPlaceholder,
        DestinationEnum.SplitSettingsDetailsPlaceholder,
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
private fun CommandArgContext.ensureUserId() =
    UserId(getParameter(ActionArgumentPrimitive.UserId)!!)
