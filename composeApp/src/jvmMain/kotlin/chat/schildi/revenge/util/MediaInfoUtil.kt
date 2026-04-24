package chat.schildi.revenge.util

import com.vanniktech.blurhash.BlurHash
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.imageio.stream.ImageInputStream
import kotlin.math.roundToInt

/**
 * Utilities to probe basic media measures for local files.
 */
object MediaInfoUtil {
    private const val BLURHASH_MAX_DIMENSION = 128
    private const val BLURHASH_COMPONENT_X = 4
    private const val BLURHASH_COMPONENT_Y = 3

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

    fun generateImageBlurHash(file: File): String? {
        val image = runCatching { ImageIO.read(file) }.getOrNull() ?: return null
        val scaledImage = image.scaleDownForBlurHash()
        return runCatching {
            BlurHash.encode(
                bufferedImage = scaledImage,
                componentX = BLURHASH_COMPONENT_X,
                componentY = BLURHASH_COMPONENT_Y,
            )
        }.getOrNull()
    }

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
