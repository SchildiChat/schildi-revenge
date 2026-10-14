package chat.schildi.revenge.database.push

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/** Individual pushes tracked for stats, including duplicates and event-less payloads. */
@Entity(tableName = "PushReceipt", indices = [Index("timestamp")])
data class PushReceiptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val pushSource: String,
    val timestamp: Long,
    val roomId: String?,
    val payload: String?,
)
