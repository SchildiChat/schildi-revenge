package chat.schildi.revenge.database.push

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "PushRegistration",
    indices = [
        Index("clientSecret")
    ]
)
data class PushRegistrationEntity(
    @PrimaryKey
    val sessionId: String,
    // Secret to tell apart pushes for different sessions, without telling the UP endpoint the session ID in plain
    val clientSecret: String,
    val endpoint: String? = null,
    val gateway: String? = null,
    val homeserverRegistered: Boolean = false,
)
