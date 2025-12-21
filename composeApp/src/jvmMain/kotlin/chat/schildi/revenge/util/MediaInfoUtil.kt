package chat.schildi.revenge.util

import java.io.File
import javax.imageio.ImageIO
import javax.imageio.stream.ImageInputStream

/**
 * Utilities to probe basic media measures for local files.
 */
object MediaInfoUtil {
    data class MediaMeasures(
        val width: Int?,
        val height: Int?,
        val durationMs: Long?,
    )

    fun probeImage(file: File): MediaMeasures {
        // Try ImageIO readers without decoding full bitmap
        var iis: ImageInputStream? = null
        try {
            iis = ImageIO.createImageInputStream(file)
            val readers = ImageIO.getImageReaders(iis)
            if (readers.hasNext()) {
                val reader = readers.next()
                reader.input = iis
                val width = runCatching { reader.getWidth(0) }.getOrNull()
                val height = runCatching { reader.getHeight(0) }.getOrNull()
                reader.dispose()
                return MediaMeasures(width, height, null)
            }
        } catch (_: Throwable) {
        } finally {
            try { iis?.close() } catch (_: Throwable) {}
        }
        return MediaMeasures(null, null, null)
    }
}
