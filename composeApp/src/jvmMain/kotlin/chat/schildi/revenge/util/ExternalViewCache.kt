package chat.schildi.revenge.util

import chat.schildi.revenge.config.ScAppDirs
import java.io.File
import java.nio.file.Files

internal object ExternalViewCache {
    private val baseDir by lazy {
        File(ScAppDirs.getUserCacheDir(), "external-view")
    }

    fun createFile(fileExtension: String): File {
        check(baseDir.isDirectory || baseDir.mkdirs()) {
            "Failed to create external view cache directory"
        }
        return Files.createTempFile(baseDir.toPath(), "schildi-revenge-", fileExtension).toFile().apply {
            deleteOnExit()
        }
    }

    fun clear() {
        baseDir.deleteRecursively()
    }
}
