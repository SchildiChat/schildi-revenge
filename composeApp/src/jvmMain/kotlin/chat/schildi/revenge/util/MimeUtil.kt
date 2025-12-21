package chat.schildi.revenge.util

import java.io.BufferedInputStream
import java.io.File
import java.net.URLConnection
import java.nio.file.Files

object MimeUtil {
    enum class AttachmentKind { IMAGE, VIDEO, AUDIO, OTHER }

    // Minimal extension mapping to cover common formats and OS gaps
    private val extensionToMime: Map<String, String> = mapOf(
        // Images
        "png" to "image/png",
        "jpg" to "image/jpeg",
        "jpeg" to "image/jpeg",
        "gif" to "image/gif",
        "webp" to "image/webp",
        "bmp" to "image/bmp",
        "heic" to "image/heic",
        "heif" to "image/heif",

        // Video
        "mp4" to "video/mp4",
        "m4v" to "video/x-m4v",
        "mov" to "video/quicktime",
        "mkv" to "video/x-matroska",
        "webm" to "video/webm",
        "avi" to "video/x-msvideo",
        "wmv" to "video/x-ms-wmv",
        "flv" to "video/x-flv",
        "ogv" to "video/ogg",

        // Audio
        "mp3" to "audio/mpeg",
        "aac" to "audio/aac",
        "m4a" to "audio/mp4",
        "wav" to "audio/wav",
        "flac" to "audio/flac",
        "ogg" to "audio/ogg",
        "oga" to "audio/ogg",
        "opus" to "audio/opus",
        "amr" to "audio/amr"
    )

    fun detectMimeType(file: File): String {
        // 1) OS/JDK probe
        try {
            val probed = Files.probeContentType(file.toPath())
            if (!probed.isNullOrBlank()) return probed
        } catch (_: Throwable) {
        }

        // 2) Magic bytes via URLConnection
        try {
            BufferedInputStream(file.inputStream()).use { bis ->
                bis.mark(24 * 1024)
                val guessed = URLConnection.guessContentTypeFromStream(bis)
                bis.reset()
                if (!guessed.isNullOrBlank()) return guessed
            }
        } catch (_: Throwable) {
        }

        // 3) Extension map fallback
        val ext = file.extension.lowercase()
        extensionToMime[ext]?.let { return it }

        // 4) Final fallback
        return "application/octet-stream"
    }

    fun classifyFromMime(mime: String): AttachmentKind = when {
        mime.startsWith("image/") -> AttachmentKind.IMAGE
        mime.startsWith("video/") -> AttachmentKind.VIDEO
        mime.startsWith("audio/") -> AttachmentKind.AUDIO
        mime == "application/ogg" -> AttachmentKind.AUDIO // ambiguous container; treat as audio by default
        else -> AttachmentKind.OTHER
    }
}
