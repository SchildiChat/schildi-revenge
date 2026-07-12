package chat.schildi.revenge.util

import com.vanniktech.blurhash.BlurHash
import io.element.android.libraries.matrix.api.timeline.InMemoryMediaThumbnail
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO
import javax.imageio.stream.ImageInputStream
import kotlin.math.roundToInt
import kotlin.math.roundToLong

actual object MediaInfoUtil {
    private const val BLURHASH_MAX_DIMENSION = 128
    private const val BLURHASH_COMPONENT_X = 4
    private const val BLURHASH_COMPONENT_Y = 3
    private const val VIDEO_THUMBNAIL_MAX_WIDTH = 800
    private val json = Json { ignoreUnknownKeys = true }

    actual fun probeImage(file: File): MediaMeasures {
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

    actual fun probeImage(bytes: ByteArray): MediaMeasures {
        val image = decodeImage(bytes) ?: return MediaMeasures(null, null, null)
        return MediaMeasures(image.width, image.height, null)
    }

    actual fun generateImageBlurHash(file: File): String? {
        val image = runCatching { ImageIO.read(file) }.getOrNull() ?: return null
        return generateImageBlurHash(image)
    }

    actual fun generateImageBlurHash(bytes: ByteArray): String? {
        val image = decodeImage(bytes) ?: return null
        return generateImageBlurHash(image)
    }

    actual fun generateVideoThumbnail(file: File): GeneratedVideoThumbnail? {
        val bytes = runCommand(
            listOf(
                ffmpegExecutable(),
                "-v", "error",
                "-i", file.absolutePath,
                "-frames:v", "1",
                "-vf", "thumbnail,scale=$VIDEO_THUMBNAIL_MAX_WIDTH:-2:force_original_aspect_ratio=decrease",
                "-f", "image2pipe",
                "-vcodec", "mjpeg",
                "pipe:1",
            )
        ) ?: return null
        val thumbnailMeasures = probeImage(bytes)
        val videoMeasures = probeVideo(file) ?: thumbnailMeasures
        return GeneratedVideoThumbnail(
            thumbnail = InMemoryMediaThumbnail(
                data = bytes,
                filename = "${file.nameWithoutExtension}-thumbnail.jpg",
                mimeType = "image/jpeg",
            ),
            videoMeasures = videoMeasures,
            thumbnailMeasures = thumbnailMeasures,
        )
    }

    actual fun probeAudio(file: File): MediaMeasures {
        val durationMs = probeMediaDuration(file)
        return MediaMeasures(width = null, height = null, durationMs = durationMs)
    }

    private fun probeVideo(file: File): MediaMeasures? {
        val output = runCommand(
            listOf(
                ffprobeExecutable(),
                "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=width,height:format=duration",
                "-of", "json",
                file.absolutePath,
            )
        )?.decodeToString()?.trim().orEmpty()
        if (output.isBlank()) return null
        val root = runCatching { json.parseToJsonElement(output).jsonObject }.getOrNull() ?: return null
        val stream = root["streams"]?.jsonArray?.firstOrNull()?.jsonObject
        val format = root["format"]?.jsonObject
        return MediaMeasures(
            width = stream?.get("width")?.jsonPrimitive?.content?.toIntOrNull(),
            height = stream?.get("height")?.jsonPrimitive?.content?.toIntOrNull(),
            durationMs = format?.durationMs(),
        )
    }

    private fun probeMediaDuration(file: File): Long? {
        val output = runCommand(
            listOf(
                ffprobeExecutable(),
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "json",
                file.absolutePath,
            )
        )?.decodeToString()?.trim().orEmpty()
        if (output.isBlank()) return null
        val root = runCatching { json.parseToJsonElement(output).jsonObject }.getOrNull() ?: return null
        return root["format"]?.jsonObject?.durationMs()
    }

    private fun kotlinx.serialization.json.JsonObject.durationMs(): Long? {
        return get("duration")
            ?.jsonPrimitive
            ?.content
            ?.toDoubleOrNull()
            ?.times(1000)
            ?.roundToLong()
    }

    private fun runCommand(command: List<String>): ByteArray? {
        val process = try {
            ProcessBuilder(command)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
        } catch (_: Throwable) {
            return null
        }
        val output = process.inputStream.use { it.readBytes() }
        return if (process.waitFor() == 0) output else null
    }

    private fun decodeImage(bytes: ByteArray): BufferedImage? {
        return runCatching {
            ByteArrayInputStream(bytes).use(ImageIO::read)
        }.getOrNull()
    }

    private fun generateImageBlurHash(image: BufferedImage): String? {
        val scaledImage = image.scaleDownForBlurHash()
        return runCatching {
            BlurHash.encode(
                bufferedImage = scaledImage,
                componentX = BLURHASH_COMPONENT_X,
                componentY = BLURHASH_COMPONENT_Y,
            )
        }.getOrNull()
    }

    private fun ffmpegExecutable(): String = if (SystemInfo.isWindows()) "ffmpeg.exe" else "ffmpeg"

    private fun ffprobeExecutable(): String = if (SystemInfo.isWindows()) "ffprobe.exe" else "ffprobe"

    private fun BufferedImage.scaleDownForBlurHash(): BufferedImage {
        if (width <= BLURHASH_MAX_DIMENSION && height <= BLURHASH_MAX_DIMENSION) {
            return this
        }
        val scale = minOf(
            BLURHASH_MAX_DIMENSION / width.toDouble(),
            BLURHASH_MAX_DIMENSION / height.toDouble(),
        )
        val targetWidth = (width * scale).roundToInt().coerceAtLeast(1)
        val targetHeight = (height * scale).roundToInt().coerceAtLeast(1)
        val scaled = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB)
        val graphics = scaled.createGraphics()
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            graphics.drawImage(this, 0, 0, targetWidth, targetHeight, null)
        } finally {
            graphics.dispose()
        }
        return scaled
    }
}
