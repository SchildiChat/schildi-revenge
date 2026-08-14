package chat.schildi.revenge.database

import androidx.room3.Database
import androidx.room3.RoomDatabase
import chat.schildi.revenge.database.push.PushNotificationDao
import chat.schildi.revenge.database.push.PushNotificationEventEntity
import chat.schildi.revenge.database.push.PushRegistrationEntity

@Database(
    entities = [
        PushRegistrationEntity::class,
        PushNotificationEventEntity::class,
    ],
    version = 1,
)
abstract class RevengeDatabase : RoomDatabase() {
    abstract fun pushNotificationDao(): PushNotificationDao
}

expect fun getRevengeDatabaseBuilder(): RoomDatabase.Builder<RevengeDatabase>

val revengeDatabase by lazy {
    getRevengeDatabaseBuilder().build()
}

