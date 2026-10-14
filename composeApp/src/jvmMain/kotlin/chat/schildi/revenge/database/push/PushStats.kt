package chat.schildi.revenge.database.push

data class PushStats(
    val sessionId: String,
    val pushCount: Long,
    val duration: Long,
)
