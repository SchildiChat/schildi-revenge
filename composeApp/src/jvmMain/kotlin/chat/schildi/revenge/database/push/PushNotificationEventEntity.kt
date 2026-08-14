package chat.schildi.revenge.database.push

import androidx.room3.Entity

@Entity(
    tableName = "PushNotificationEvent",
    primaryKeys = [
        "sessionId",
        "roomId",
        "eventId",
    ],
)
data class PushNotificationEventEntity(
    val sessionId: String,
    val roomId: String,
    val eventId: String,
    val failureCount: Int = 0,
    val lastFailure: String? = null,
    val pushSource: String,
    val timestamp: Long,
    val resolvedTimestamp: Long? = null,
)
