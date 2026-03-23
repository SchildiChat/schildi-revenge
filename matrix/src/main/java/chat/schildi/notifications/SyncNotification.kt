package chat.schildi.notifications

import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.core.ThreadId
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.notification.NotificationContent
import io.element.android.libraries.matrix.api.room.join.JoinRule

data class SyncNotification(
    val sessionId: SessionId,
    val roomId: RoomId,
    val eventId: EventId?,
    val senderId: UserId,
    val threadId: ThreadId?,
    val rawEvent: String,
    val content: NotificationContent,
    val senderInfo: SenderInfo,
    val roomInfo: RoomInfo,
    val isNoisy: Boolean,
    val hasMention: Boolean,
    val roomAppearedUnreadAfterSync: Boolean?,
    val actions: List<Action>,
) {
    data class SenderInfo(
        val displayName: String?,
        val avatarUrl: String?,
        val isNameAmbiguous: Boolean,
    )

    data class RoomInfo(
        val displayName: String,
        val avatarUrl: String?,
        val canonicalAlias: String?,
        val topic: String?,
        val joinRule: JoinRule?,
        val joinedMembersCount: Long,
        val isEncrypted: Boolean,
        val isDirect: Boolean,
        val isSpace: Boolean,
    )

    sealed interface Action {
        data object Notify : Action

        data class SetTweak(val value: Tweak) : Action
    }

    sealed interface Tweak {
        data class Sound(val value: String) : Tweak

        data class Highlight(val value: Boolean) : Tweak

        data class Custom(
            val name: String,
            val value: String,
        ) : Tweak
    }
}
