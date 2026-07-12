package chat.schildi.revenge.util

import java.io.File

// TODO Android media decoding
actual object MediaInfoUtil {
    actual fun probeImage(file: File) = MediaMeasures(null, null, null)
    actual fun probeImage(bytes: ByteArray) = MediaMeasures(null, null, null)
    actual fun generateImageBlurHash(file: File): String? = null
    actual fun generateImageBlurHash(bytes: ByteArray): String? = null
    actual fun generateVideoThumbnail(file: File): GeneratedVideoThumbnail? = null
    actual fun probeAudio(file: File) = MediaMeasures(null, null, null)
}
