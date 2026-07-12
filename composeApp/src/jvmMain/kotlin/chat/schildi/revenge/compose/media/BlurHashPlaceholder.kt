package chat.schildi.revenge.compose.media

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.vanniktech.blurhash.BlurHash
import kotlin.math.roundToInt

private const val BLURHASH_PLACEHOLDER_MAX_DIMENSION = 32

@Composable
fun BlurHashPlaceholder(
    blurHash: String?,
    width: Long?,
    height: Long?,
    modifier: Modifier = Modifier,
) {
    val sanitizedBlurHash = blurHash?.takeIf(String::isNotBlank)
    val averageColor = remember(sanitizedBlurHash) {
        sanitizedBlurHash
            ?.let(BlurHash::averageColor)
            ?.let(::Color)
    }
    val decodedImage = remember(sanitizedBlurHash, width, height) {
        sanitizedBlurHash?.let {
            val (targetWidth, targetHeight) = blurHashDecodeSize(width, height)
            decodeBlurHash(it, targetWidth, targetHeight)
        }
    }
    Box(
        modifier = modifier.background(
            averageColor ?: MaterialTheme.colorScheme.surfaceContainerHighest,
        )
    ) {
        decodedImage?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
        }
    }
}

internal expect fun decodeBlurHash(blurHash: String, width: Int, height: Int): ImageBitmap?

private fun blurHashDecodeSize(width: Long?, height: Long?): Pair<Int, Int> {
    val safeWidth = width?.takeIf { it > 0 }
    val safeHeight = height?.takeIf { it > 0 }
    if (safeWidth == null || safeHeight == null) {
        return BLURHASH_PLACEHOLDER_MAX_DIMENSION to BLURHASH_PLACEHOLDER_MAX_DIMENSION
    }
    val aspectRatio = safeWidth.toFloat() / safeHeight.toFloat()
    return if (aspectRatio >= 1f) {
        BLURHASH_PLACEHOLDER_MAX_DIMENSION to (BLURHASH_PLACEHOLDER_MAX_DIMENSION / aspectRatio)
            .roundToInt()
            .coerceAtLeast(1)
    } else {
        (BLURHASH_PLACEHOLDER_MAX_DIMENSION * aspectRatio)
            .roundToInt()
            .coerceAtLeast(1) to BLURHASH_PLACEHOLDER_MAX_DIMENSION
    }
}
