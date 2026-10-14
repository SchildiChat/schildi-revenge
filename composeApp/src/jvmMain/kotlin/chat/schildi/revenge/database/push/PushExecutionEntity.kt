package chat.schildi.revenge.database.push

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/** Background worker runs tracked for stats. */
@Entity(tableName = "PushExecution", indices = [Index("timestamp")])
data class PushExecutionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val roomId: String,
    val timestamp: Long,
    val duration: Long,
)
