package chat.schildi.revenge.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import com.vanniktech.blurhash.BlurHash
import io.element.android.libraries.matrix.api.timeline.InMemoryMediaThumbnail
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import kotlin.math.roundToInt
import androidx.core.graphics.scale

actual object MediaInfoUtil {
    actual fun probeImage(file: File): MediaMeasures = probeImage { file.inputStream() }

    actual fun probeImage(bytes: ByteArray): MediaMeasures = probeImage { bytes.inputStream() }

    actual fun generateImageBlurHash(file: File): String? = generateImageBlurHash { file.inputStream() }

    actual fun generateImageBlurHash(bytes: ByteArray): String? = generateImageBlurHash { bytes.inputStream() }

    actual fun generateVideoThumbnail(file: File): GeneratedVideoThumbnail? = withMediaRetriever(file) { retriever ->
        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            ?.toIntOrNull()?.takeIf { it > 0 }
        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            ?.toIntOrNull()?.takeIf { it > 0 }
        val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull()
        val swapDimensions = rotation == 90 || rotation == 270
        val videoMeasures = MediaMeasures(
            width = if (swapDimensions) height else width,
            height = if (swapDimensions) width else height,
            durationMs = retriever.durationMs(),
        )
        val frame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 &&
            videoMeasures.width != null && videoMeasures.height != null
        ) {
            val targetWidth = minOf(videoMeasures.width, VIDEO_THUMBNAIL_MAX_WIDTH)
            val targetHeight = (videoMeasures.height * targetWidth.toDouble() / videoMeasures.width)
                .roundToInt().coerceAtLeast(1)
            retriever.getScaledFrameAtTime(-1, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, targetWidth, targetHeight)
        } else {
            retriever.getFrameAtTime(-1, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } ?: return@withMediaRetriever null
        try {
            val targetWidth = minOf(frame.width, VIDEO_THUMBNAIL_MAX_WIDTH)
            val targetHeight = (frame.height * targetWidth.toDouble() / frame.width).roundToInt().coerceAtLeast(1)
            val thumbnail = frame.scale(targetWidth, targetHeight)
            try {
                val bytes = ByteArrayOutputStream().use { output ->
                    if (!thumbnail.compress(Bitmap.CompressFormat.JPEG, 85, output)) {
                        return@withMediaRetriever null
                    }
                    output.toByteArray()
                }
                GeneratedVideoThumbnail(
                    thumbnail = InMemoryMediaThumbnail(
                        data = bytes,
                        filename = "${file.nameWithoutExtension}-thumbnail.jpg",
                        mimeType = "image/jpeg",
                    ),
                    videoMeasures = videoMeasures,
                    thumbnailMeasures = MediaMeasures(thumbnail.width, thumbnail.height, null),
                )
            } finally {
                if (thumbnail !== frame) thumbnail.recycle()
            }
        } finally {
            frame.recycle()
        }
    }

    actual fun probeAudio(file: File): MediaMeasures = withMediaRetriever(file) { retriever ->
        MediaMeasures(width = null, height = null, durationMs = retriever.durationMs())
    } ?: MediaMeasures(null, null, null)

    private fun probeImage(openStream: () -> InputStream): MediaMeasures = runCatching {
        val options = readImageBounds(openStream)
        val width = options.outWidth.takeIf { it > 0 }
        val height = options.outHeight.takeIf { it > 0 }
        val swapDimensions = when (readImageOrientation(openStream)) {
            ExifInterface.ORIENTATION_TRANSPOSE,
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_TRANSVERSE,
            ExifInterface.ORIENTATION_ROTATE_270 -> true
            else -> false
        }
        MediaMeasures(
            width = if (swapDimensions) height else width,
            height = if (swapDimensions) width else height,
            durationMs = null,
        )
    }.getOrDefault(MediaMeasures(null, null, null))

    private fun generateImageBlurHash(openStream: () -> InputStream): String? = runCatching {
        val options = readImageBounds(openStream)
        if (options.outWidth <= 0 || options.outHeight <= 0) return@runCatching null
        options.inJustDecodeBounds = false
        options.inSampleSize = 1
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        val maxDimension = maxOf(options.outWidth, options.outHeight)
        while (maxDimension / options.inSampleSize / 2 >= BLURHASH_MAX_DIMENSION) {
            options.inSampleSize *= 2
        }
        val bitmap = openStream().use { BitmapFactory.decodeStream(it, null, options) }
            ?: return@runCatching null
        try {
            val matrix = imageOrientationMatrix(readImageOrientation(openStream))
            val scale = minOf(1f, BLURHASH_MAX_DIMENSION.toFloat() / maxOf(bitmap.width, bitmap.height))
            val targetWidth = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
            val targetHeight = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
            matrix.preScale(targetWidth.toFloat() / bitmap.width, targetHeight.toFloat() / bitmap.height)
            val scaledImage = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            try {
                BlurHash.encode(
                    bitmap = scaledImage,
                    componentX = BLURHASH_COMPONENT_X,
                    componentY = BLURHASH_COMPONENT_Y,
                )
            } finally {
                if (scaledImage !== bitmap) scaledImage.recycle()
            }
        } finally {
            bitmap.recycle()
        }
    }.getOrNull()

    private fun readImageBounds(openStream: () -> InputStream): BitmapFactory.Options {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openStream().use { BitmapFactory.decodeStream(it, null, options) }
        return options
    }

    private fun readImageOrientation(openStream: () -> InputStream): Int = runCatching {
        openStream().use {
            ExifInterface(it)
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

    private fun imageOrientationMatrix(orientation: Int): Matrix = Matrix().apply {
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                setRotate(90f)
                postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                setRotate(270f)
                postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(270f)
        }
    }

    private fun MediaMetadataRetriever.durationMs(): Long? =
        extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.takeIf { it >= 0 }

    private fun <T> withMediaRetriever(file: File, block: (MediaMetadataRetriever) -> T): T? = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            block(retriever)
        } finally {
            retriever.release()
        }
    }.getOrNull()
}
