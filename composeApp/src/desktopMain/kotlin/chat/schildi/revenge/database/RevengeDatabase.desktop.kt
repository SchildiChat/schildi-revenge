package chat.schildi.revenge.database

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import chat.schildi.revenge.config.ScAppDirs
import java.io.File

private val dataDir = File(ScAppDirs.getUserDataDir()).also {
    it.mkdirs()
}
private val databaseFile = File(dataDir, "revenge.db")

actual fun getRevengeDatabaseBuilder() = Room.databaseBuilder<RevengeDatabase>(
    name = databaseFile.absolutePath,
).setDriver(BundledSQLiteDriver())
