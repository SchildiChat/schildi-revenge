package chat.schildi.notifications

import io.element.android.libraries.core.bool.orFalse
import io.element.android.libraries.core.extensions.runCatchingExceptions
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.core.ThreadId
import io.element.android.libraries.matrix.impl.notification.NotificationContentMapper
import io.element.android.libraries.matrix.impl.notification.senderId
import io.element.android.libraries.matrix.impl.room.join.map
import org.matrix.rustcomponents.sdk.Action
import org.matrix.rustcomponents.sdk.NotificationEvent
import org.matrix.rustcomponents.sdk.NotificationItem
import org.matrix.rustcomponents.sdk.Tweak
import org.matrix.rustcomponents.sdk.use

class SyncNotificationMapper(
    private val sessionId: SessionId,
) {
    private val notificationContentMapper = NotificationContentMapper()

    fun map(
        notificationItem: NotificationItem,
        roomId: String,
    ): Result<SyncNotification> {
        return runCatchingExceptions {
            notificationItem.use { item ->
                SyncNotification(
                    sessionId = sessionId,
                    roomId = RoomId(roomId),
                    eventId = item.event.eventIdOrNull(),
                    senderId = item.event.senderId(),
                    threadId = item.threadId?.let(::ThreadId),
                    rawEvent = item.rawEvent,
                    content = notificationContentMapper.map(item.event).getOrThrow(),
                    senderInfo =
                        SyncNotification.SenderInfo(
                            displayName = item.senderInfo.displayName,
                            avatarUrl = item.senderInfo.avatarUrl,
                            isNameAmbiguous = item.senderInfo.isNameAmbiguous,
                        ),
                    roomInfo =
                        SyncNotification.RoomInfo(
                            displayName = item.roomInfo.displayName,
                            avatarUrl = item.roomInfo.avatarUrl,
                            canonicalAlias = item.roomInfo.canonicalAlias,
                            topic = item.roomInfo.topic,
                            joinRule = item.roomInfo.joinRule?.map(),
                            joinedMembersCount = item.roomInfo.joinedMembersCount.toLong(),
                            isEncrypted = item.roomInfo.isEncrypted.orFalse(),
                            isDirect = item.roomInfo.isDirect,
                            isSpace = item.roomInfo.isSpace,
                        ),
                    isNoisy = item.isNoisy.orFalse(),
                    hasMention = item.hasMention.orFalse(),
                    actions = item.actions.orEmpty().map(Action::map),
                )
            }
        }
    }
}

private fun NotificationEvent.eventIdOrNull(): EventId? =
    when (this) {
        is NotificationEvent.Timeline -> EventId(event.eventId())
        is NotificationEvent.Invite -> null
    }

private fun Action.map(): SyncNotification.Action =
    when (this) {
        Action.Notify -> SyncNotification.Action.Notify
        is Action.SetTweak -> SyncNotification.Action.SetTweak(value.map())
    }

private fun Tweak.map(): SyncNotification.Tweak =
    when (this) {
        is Tweak.Sound -> SyncNotification.Tweak.Sound(value)
        is Tweak.Highlight -> SyncNotification.Tweak.Highlight(value)
        is Tweak.Custom -> SyncNotification.Tweak.Custom(name = name, value = value)
    }
