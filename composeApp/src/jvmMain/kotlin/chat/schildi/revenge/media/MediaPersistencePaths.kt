package chat.schildi.revenge.media

import chat.schildi.revenge.config.ScAppDirs
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import java.io.File
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

object MediaPersistencePaths {

    // Note, we still want to use the user cache directory - when persisting files from the Rust SDK,
    // which live in the cache directory, we're doing so by a rename operation under the hood,
    // which will not work if the user has their SDK cache directory on another file system than we're
    // having the persistent file.
    val attachmentBaseDir = File(ScAppDirs.getUserCacheDir(), "attachments")

    private val timePrefixFormat = LocalDateTime.Format {
        year()
        char('-')
        monthNumber()
        char('-')
        day()
        char('_')
        hour()
        char('-')
        minute()
        char('-')
        second()
    }

    @OptIn(ExperimentalTime::class)
    private fun formatTimePrefix(timestamp: Long): String {
        val instant = Instant.fromEpochMilliseconds(timestamp)
        val utcDateTime = instant.toLocalDateTime(TimeZone.UTC)
        return utcDateTime.format(timePrefixFormat)
    }

    fun getPersistentAttachmentFile(
        sessionId: SessionId,
        roomId: RoomId?,
        timestamp: Long,
        mxcUrl: String,
        filename: String?
    ): File {
        val sessionDir = File(attachmentBaseDir, sessionId.value.toSafeFilename())
        val dir = if (roomId == null) {
            sessionDir
        } else {
            File(sessionDir, roomId.value.toSafeFilename())
        }
        dir.mkdirs()
        val filenameAppend = filename?.let { "_$it" } ?: ""
        val safeUrl = mxcUrl.removePrefix("mxc://").toSafeFilename()
        val filename = "${formatTimePrefix(timestamp)}_${timestamp}_$safeUrl$filenameAppend"
        return File(dir, filename)
    }

    private val safeFilenameRegex = Regex("[^A-Za-z0-9._-]+")
    private fun String.toSafeFilename(
        replacement: String = "_",
        maxLength: Int = 255
    ): String {
        // Replace any run of disallowed chars with replacement
        var normalized = this.replace(safeFilenameRegex, replacement)

        // Trim replacement chars from ends
        normalized = normalized.trim(replacement.first())

        // Use fallback if everything was removed
        if (normalized.isEmpty()) {
            normalized = "file"
        }

        // Trim to max length
        if (normalized.length > maxLength) {
            normalized = normalized.take(maxLength)
        }

        return normalized
    }
}
