package chat.schildi.revenge.database

import androidx.room3.AutoMigration
import androidx.room3.Database
import androidx.room3.RoomDatabase
import chat.schildi.revenge.database.push.PushExecutionEntity
import chat.schildi.revenge.database.push.PushNotificationDao
import chat.schildi.revenge.database.push.PushNotificationEventEntity
import chat.schildi.revenge.database.push.PushReceiptEntity
import chat.schildi.revenge.database.push.PushRegistrationEntity

@Database(
    entities = [
        PushRegistrationEntity::class,
        PushNotificationEventEntity::class,
        PushReceiptEntity::class,
        PushExecutionEntity::class,
    ],
    version = 2,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class RevengeDatabase : RoomDatabase() {
    abstract fun pushNotificationDao(): PushNotificationDao
}

expect fun getRevengeDatabaseBuilder(): RoomDatabase.Builder<RevengeDatabase>

val revengeDatabase by lazy {
    getRevengeDatabaseBuilder().build()
}
