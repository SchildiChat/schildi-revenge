package chat.schildi.revenge.compose.media

/**
 * Disclaimer: most of this file was written by AI, to get animated GIF support in without spending too much effort.
 * I'm happy to switch to any library or upstream solution that properly integrates into coil once available.
 */

import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultFilterQuality
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import coil3.compose.AsyncImagePainter
import io.element.android.libraries.matrix.ui.media.animated.AnimatedSkiaImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.jetbrains.skia.AnimationFrameInfo
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import org.jetbrains.skia.FilterMipmap
import org.jetbrains.skia.FilterMode
import org.jetbrains.skia.MipmapMode
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import kotlin.math.roundToInt
import kotlin.time.TimeMark
import kotlin.time.TimeSource

@Composable
fun rememberAnimatedImageTransform(
    filterQuality: FilterQuality = DefaultFilterQuality,
): (AsyncImagePainter.State) -> AsyncImagePainter.State {
    return remember(filterQuality) {
        { state -> state.withAnimatedPainter(filterQuality) }
    }
}

private fun AsyncImagePainter.State.withAnimatedPainter(
    filterQuality: FilterQuality,
): AsyncImagePainter.State = when (this) {
    AsyncImagePainter.State.Empty,
    is AsyncImagePainter.State.Loading,
    is AsyncImagePainter.State.Error,
        -> this
    is AsyncImagePainter.State.Success -> {
        val animatedImage = result.image as? AnimatedSkiaImage ?: return this
        copy(painter = AnimatedSkiaPainter(animatedImage, filterQuality))
    }
}

class AnimatedSkiaPainter(
    private val sourceImage: AnimatedSkiaImage,
    private val filterQuality: FilterQuality,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) : Painter(), RememberObserver {
    override val intrinsicSize: Size = Size(sourceImage.width.toFloat(), sourceImage.height.toFloat())

    private val lock = Any()
    private val bufferedFramesCount = sourceImage.bufferedFramesCount.coerceAtLeast(1)
    private val frames = LinkedHashMap<Int, ByteArray>(bufferedFramesCount, 1f, true)

    private var scope: CoroutineScope? = null
    private var codec: Codec? = null
    private var tempBitmap: Bitmap? = null
    private var frameDurationsMs: List<Int> = emptyList()
    private var singleIterationDurationMs: Long = 1L
    private var maxIterationCount: Int = Int.MAX_VALUE
    private var bufferFramesJob: Job? = null
    private var animationStartTime: TimeMark? = null
    private var lastBufferedFrameIndex: Int? = null
    private var invalidateTick by mutableIntStateOf(0)
    private var alpha: Float = DefaultAlpha

    override fun onRemembered() {
        if (scope != null) return
        scope = CoroutineScope(sourceImage.decoderCoroutineContext + SupervisorJob())
        initializeRuntime()
    }

    override fun onForgotten() {
        disposeRuntime()
    }

    override fun onAbandoned() {
        disposeRuntime()
    }

    override fun applyAlpha(alpha: Float): Boolean {
        this.alpha = alpha
        return true
    }

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        return false
    }

    override fun DrawScope.onDraw() {
        @Suppress("UNUSED_VARIABLE")
        val observedInvalidateTick = invalidateTick
        initializeRuntime()
        val codec = codec ?: return
        if (codec.frameCount <= 1) {
            drawFrame(0)
            return
        }

        val startTime = animationStartTime ?: timeSource.markNow().also { animationStartTime = it }
        val elapsedMs = startTime.elapsedNow().inWholeMilliseconds
        val totalDurationMs = singleIterationDurationMs * maxIterationCount.toLong()
        val isAnimationComplete = maxIterationCount > 0 && elapsedMs >= totalDurationMs
        val frameIndex = if (isAnimationComplete) codec.frameCount - 1 else frameIndexFor(elapsedMs)

        drawFrame(frameIndex)
        if (!isAnimationComplete) {
            ensureBackgroundBuffering(codec, frameIndex)
            invalidateTick++
        }
    }

    private fun DrawScope.drawFrame(frameIndex: Int) {
        val codec = codec ?: return
        val frame = synchronized(lock) { frames[frameIndex] }
            ?: decodeFrame(codec, frameIndex)?.also { cacheFrame(frameIndex, it) }
            ?: return
        val image = org.jetbrains.skia.Image.makeRaster(codec.imageInfo, frame, codec.imageInfo.minRowBytes)
        try {
            val paint = Paint().apply {
                alpha = (this@AnimatedSkiaPainter.alpha.coerceIn(0f, 1f) * 255).roundToInt()
            }
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawImageRect(
                    image,
                    Rect.makeWH(codec.width.toFloat(), codec.height.toFloat()),
                    Rect.makeWH(size.width, size.height),
                    filterQuality.toSamplingMode(),
                    paint,
                    true,
                )
            }
        } finally {
            image.close()
        }
    }

    private fun initializeRuntime() {
        if (codec != null) return
        val runtimeCodec = Codec.makeFromData(Data.makeFromBytes(sourceImage.encodedBytes))
        codec = runtimeCodec
        tempBitmap = Bitmap().apply { allocPixels(runtimeCodec.imageInfo) }
        frameDurationsMs = runtimeCodec.framesInfo.map(AnimationFrameInfo::safeDurationMillis)
        singleIterationDurationMs = frameDurationsMs.sumOf(Int::toLong).coerceAtLeast(1L)
        maxIterationCount = runtimeCodec.repetitionCount.takeIf { it >= 0 }?.plus(1) ?: Int.MAX_VALUE
        synchronized(lock) {
            frames.clear()
            frames[0] = sourceImage.firstFrame
        }
    }

    private fun ensureBackgroundBuffering(codec: Codec, frameIndex: Int) {
        if (codec.frameCount <= 1) return
        if (bufferFramesJob?.isActive == true) return
        if (lastBufferedFrameIndex == frameIndex && cachedFramesCount() >= bufferedFramesCount) return
        lastBufferedFrameIndex = frameIndex
        bufferFramesJob = scope?.launch {
            try {
                for (offset in 1..bufferedFramesCount) {
                    val index = (frameIndex + offset) % codec.frameCount
                    if (hasCachedFrame(index)) continue
                    val frame = decodeFrame(codec, index) ?: break
                    cacheFrame(index, frame)
                }
                invalidateTick++
            } catch (_: CancellationException) {
                // The painter was disposed while buffering.
            }
        }
    }

    private fun decodeFrame(codec: Codec, frameIndex: Int): ByteArray? =
        synchronized(lock) {
            val tempBitmap = tempBitmap ?: return null
            if (this.codec !== codec || tempBitmap.isClosed || codec.isClosed) {
                return null
            }
            try {
                codec.readPixels(tempBitmap, frameIndex)
                tempBitmap.readPixels(
                    dstInfo = codec.imageInfo,
                    dstRowBytes = codec.imageInfo.minRowBytes,
                )
            } catch (_: Exception) {
                null
            }
        }

    private fun frameIndexFor(elapsedMs: Long): Int {
        val elapsedInIterationMs = elapsedMs % singleIterationDurationMs
        var accumulatedMs = 0L
        for ((index, durationMs) in frameDurationsMs.withIndex()) {
            accumulatedMs += durationMs
            if (elapsedInIterationMs < accumulatedMs) {
                return index
            }
        }
        return frameDurationsMs.lastIndex
    }

    private fun hasCachedFrame(frameIndex: Int): Boolean = synchronized(lock) { frames.containsKey(frameIndex) }

    private fun cachedFramesCount(): Int = synchronized(lock) { frames.size }

    private fun cacheFrame(frameIndex: Int, frame: ByteArray) {
        synchronized(lock) {
            frames[frameIndex] = frame
            while (frames.size > bufferedFramesCount) {
                val eldestKey = frames.entries.firstOrNull()?.key ?: break
                frames.remove(eldestKey)
            }
        }
    }

    private fun disposeRuntime() {
        bufferFramesJob?.cancel()
        bufferFramesJob = null
        scope?.cancel()
        scope = null
        animationStartTime = null
        lastBufferedFrameIndex = null
        synchronized(lock) {
            frames.clear()
            tempBitmap?.takeUnless { it.isClosed }?.close()
            tempBitmap = null
            codec?.takeUnless { it.isClosed }?.close()
            codec = null
        }
    }
}

private fun FilterQuality.toSamplingMode(): SamplingMode = when (this) {
    FilterQuality.None -> SamplingMode.DEFAULT
    FilterQuality.Low -> SamplingMode.LINEAR
    FilterQuality.Medium -> FilterMipmap(FilterMode.LINEAR, MipmapMode.NEAREST)
    FilterQuality.High -> FilterMipmap(FilterMode.LINEAR, MipmapMode.LINEAR)
    else -> SamplingMode.LINEAR
}

private fun AnimationFrameInfo.safeDurationMillis(): Int {
    return duration.takeIf { it > 0 } ?: DefaultFrameDurationMillis
}

private const val DefaultFrameDurationMillis = 100
