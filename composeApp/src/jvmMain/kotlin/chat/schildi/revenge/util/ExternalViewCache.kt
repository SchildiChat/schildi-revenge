package chat.schildi.revenge.util

import chat.schildi.revenge.config.ScAppDirs
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope

internal object ExternalViewCache {
    private val baseDir by lazy {
        File(ScAppDirs.getUserCacheDir(), "external-view")
    }
    private val startupCleaner = StartupCacheCleaner {
        baseDir.deleteRecursively()
    }

    suspend fun createFile(fileExtension: String): File {
        startupCleaner.awaitClear()
        check(baseDir.isDirectory || baseDir.mkdirs() || baseDir.isDirectory) {
            "Failed to create external view cache directory"
        }
        return Files.createTempFile(baseDir.toPath(), "schildi-revenge-", fileExtension).toFile().apply {
            deleteOnExit()
        }
    }

    fun clearAsync(scope: CoroutineScope) {
        startupCleaner.clearAsync(scope)
    }
}
