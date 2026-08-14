package chat.schildi.revenge.push

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val pushJson = Json {
    ignoreUnknownKeys = true
}

@Serializable
data class UnifiedPushPayload(
    val notification: UnifiedPushNotification? = null,
)

@Serializable
data class UnifiedPushNotification(
    @SerialName("event_id") val eventId: String? = null,
    @SerialName("room_id") val roomId: String? = null,
    val counts: UnifiedPushCounts? = null,
    val prio: String? = null,
)

@Serializable
data class UnifiedPushCounts(
    val unread: Int? = null,
)

@Serializable
private data class UnifiedPushDefaultPayload(
    @SerialName("cs") val clientSecret: String,
)

fun deserializeUnifiedPushPayload(content: ByteArray): UnifiedPushPayload? = runCatching {
    pushJson.decodeFromString<UnifiedPushPayload>(content.decodeToString())
}.getOrNull()

fun serializeUnifiedPushDefaultPayload(clientSecret: String): String =
    pushJson.encodeToString(UnifiedPushDefaultPayload(clientSecret))
