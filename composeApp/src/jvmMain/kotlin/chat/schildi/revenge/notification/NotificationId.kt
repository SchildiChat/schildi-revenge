package chat.schildi.revenge.notification

import chat.schildi.revenge.model.verification.ScIncomingVerificationRequest
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.FlowId
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId

sealed interface NotificationId {
    val sessionId: SessionId?
    sealed interface ForRoom {
        val roomId: RoomId
    }
    data class Event(
        override val sessionId: SessionId,
        override val roomId: RoomId,
        val eventId: EventId,
    ) : NotificationId, ForRoom
    data class Room(
        override val sessionId: SessionId,
        override val roomId: RoomId,
    ) : NotificationId, ForRoom
    data class VerificationRequest(
        override val sessionId: SessionId,
        val flowId: FlowId,
    ) : NotificationId
    data object DebugMessage : NotificationId {
        override val sessionId = null
    }
}

fun ScIncomingVerificationRequest.toNotificationId() = NotificationId.VerificationRequest(
    sessionId = sessionId,
    flowId = request.details.flowId,
)
