package chat.schildi.revenge.util

import io.element.android.libraries.matrix.api.timeline.InMemoryMediaThumbnail
import java.io.File

data class MediaMeasures(
    val width: Int?,
    val height: Int?,
    val durationMs: Long?,
)

data class GeneratedVideoThumbnail(
    val thumbnail: InMemoryMediaThumbnail,
    val videoMeasures: MediaMeasures,
    val thumbnailMeasures: MediaMeasures,
)

/**
 * Utilities to probe basic media measures for local files.
 */
expect object MediaInfoUtil {
    fun probeImage(file: File): MediaMeasures
    fun probeImage(bytes: ByteArray): MediaMeasures
    fun generateImageBlurHash(file: File): String?
    fun generateImageBlurHash(bytes: ByteArray): String?
    fun generateVideoThumbnail(file: File): GeneratedVideoThumbnail?
    fun probeAudio(file: File): MediaMeasures
}
