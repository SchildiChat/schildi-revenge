package chat.schildi.revenge.database.push

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PushNotificationDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPush(pushEvent: PushNotificationEventEntity)

    @Update
    suspend fun updatePushes(pushEvents: List<PushNotificationEventEntity>)

    @Query("""
        SELECT *
        FROM PushNotificationEvent
        WHERE sessionId = :sessionId AND roomId = :roomId
          AND failureCount <= :maxFailures AND resolvedTimestamp IS NULL
        ORDER BY timestamp
    """)
    suspend fun getPendingPushes(sessionId: String, roomId: String, maxFailures: Int = Int.MAX_VALUE): List<PushNotificationEventEntity>

    @Query("""
        UPDATE PushNotificationEvent
        SET resolvedTimestamp = :timestamp
        WHERE sessionId = :sessionId
          AND roomId = :roomId
          AND eventId = :eventId
    """)
    suspend fun markPushResolved(
        sessionId: String,
        roomId: String,
        eventId: String,
        timestamp: Long = System.currentTimeMillis(),
    )

    @Upsert
    suspend fun insertPushRegistration(registration: PushRegistrationEntity)

    @Delete
    suspend fun deletePushRegistration(registration: PushRegistrationEntity)

    @Query("DELETE FROM PushRegistration")
    suspend fun deleteAllPushRegistrations()

    @Query("""
        UPDATE PushRegistration
        SET endpoint = :endpoint, homeserverRegistered = 0
        WHERE clientSecret = :instance AND (endpoint IS NULL OR endpoint != :endpoint)
    """)
    suspend fun updateEndpoint(instance: String, endpoint: String)

    @Query("SELECT * FROM PushRegistration")
    suspend fun getPushRegistrations(): List<PushRegistrationEntity>

    @Query("SELECT * FROM PushRegistration")
    fun followPushRegistrations(): Flow<List<PushRegistrationEntity>>

    @Query("SELECT sessionId FROM PushRegistration WHERE clientSecret = :instance LIMIT 1")
    suspend fun getSessionId(instance: String): String?

    @Query("""
        SELECT * FROM PushRegistration
        WHERE clientSecret = :instance AND homeserverRegistered = 0 AND endpoint IS NOT NULL
        LIMIT 1
    """)
    suspend fun getPendingPushRegistration(instance: String): PushRegistrationEntity?

    @Query("""
        SELECT * FROM PushRegistration
        WHERE clientSecret = :instance
        LIMIT 1
    """)
    suspend fun getPushRegistration(instance: String): PushRegistrationEntity?

    @Insert
    suspend fun insertReceipt(receipt: PushReceiptEntity)

    @Insert
    suspend fun insertExecution(execution: PushExecutionEntity)

    @Query("DELETE FROM PushReceipt WHERE timestamp < :before")
    suspend fun deleteOldReceipts(before: Long)

    @Query("DELETE FROM PushExecution WHERE timestamp < :before")
    suspend fun deleteOldExecutions(before: Long)

    @Query("DELETE FROM PushNotificationEvent WHERE timestamp < :before")
    suspend fun deleteOldPushEvents(before: Long)

    @Transaction
    suspend fun deleteOldPushHistory() {
        val cutoff = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        deleteOldReceipts(cutoff)
        deleteOldExecutions(cutoff)
        deleteOldPushEvents(cutoff)
    }

    @Query("""
        SELECT registration.sessionId,
            (SELECT COUNT(*) FROM PushReceipt
             WHERE sessionId = registration.sessionId
               AND timestamp >= :since) AS pushCount,
            COALESCE((SELECT SUM(duration) FROM PushExecution
                      WHERE sessionId = registration.sessionId
                        AND timestamp >= :since), 0) AS duration
        FROM PushRegistration AS registration
    """)
    fun followPushStats(since: Long): Flow<List<PushStats>>

}
