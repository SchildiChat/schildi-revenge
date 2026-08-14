package chat.schildi.revenge.database

import androidx.room3.Room
import chat.schildi.revenge.RevengeApplication

actual fun getRevengeDatabaseBuilder() = RevengeApplication.instance.let { context ->
    Room.databaseBuilder<RevengeDatabase>(
        context = context,
        name = context.getDatabasePath("revenge.db").absolutePath,
    )
}
