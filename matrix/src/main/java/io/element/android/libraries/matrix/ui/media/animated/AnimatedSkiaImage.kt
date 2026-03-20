/*
 * Copyright 2024 Coil Contributors
 * Based on: https://github.com/coil-kt/coil/pull/2594
 * by Baptiste Candellier, based on a POC by Colin White.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.element.android.libraries.matrix.ui.media.animated

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import coil3.Canvas
import coil3.Image
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.skia.AnimationFrameInfo
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Codec
import kotlin.time.TimeMark
import kotlin.time.TimeSource

internal class AnimatedSkiaImage(
    private val codec: Codec,
    private val coroutineScope: CoroutineScope,
    bufferedFramesCount: Int,
    private val timeSource: TimeSource,
) : Image {
    override val size: Long
        get() =
            codec.imageInfo.computeMinByteSize().toLong().takeIf { it > 0L }
                ?: (codec.width.toLong() * codec.height * 4L)

    override val width: Int
        get() = codec.width

    override val height: Int
        get() = codec.height

    override val shareable: Boolean = false

    private val lock = Any()
    private val tempBitmap = Bitmap().apply { allocPixels(codec.imageInfo) }
    private val frames = arrayOfNulls<ByteArray>(codec.frameCount)
    private val frameDurationsMs: List<Int> = codec.framesInfo.map(AnimationFrameInfo::safeDurationMillis)
    private val singleIterationDurationMs: Long = frameDurationsMs.sumOf(Int::toLong).coerceAtLeast(1L)
    private val maxIterationCount: Int = codec.repetitionCount.takeIf { it >= 0 }?.plus(1) ?: Int.MAX_VALUE

    private var bufferFramesJob: Job? = null
    private var animationStartTime: TimeMark? = null
    private var invalidateTick by mutableIntStateOf(0)

    init {
        for (index in 0 until minOf(bufferedFramesCount, frames.size)) {
            frames[index] = decodeFrame(index)
        }
    }

    override fun draw(canvas: Canvas) {
        if (codec.frameCount <= 0) return

        @Suppress("UNUSED_VARIABLE")
        val observedInvalidateTick = invalidateTick

        if (codec.frameCount == 1) {
            drawFrame(canvas, 0)
            return
        }

        ensureBackgroundBuffering()

        val startTime = animationStartTime ?: timeSource.markNow().also { animationStartTime = it }
        val elapsedMs = startTime.elapsedNow().inWholeMilliseconds
        val totalDurationMs = singleIterationDurationMs * maxIterationCount.toLong()
        val isAnimationComplete = maxIterationCount > 0 && elapsedMs >= totalDurationMs

        if (isAnimationComplete) {
            drawFrame(canvas, codec.frameCount - 1)
            return
        }

        val currentFrameIndex = frameIndexFor(elapsedMs)
        drawFrame(canvas, currentFrameIndex)
        invalidateTick++
    }

    private fun ensureBackgroundBuffering() {
        if (frames.all { it != null }) return
        if (bufferFramesJob?.isActive == true) return
        bufferFramesJob =
            coroutineScope.launch {
                for (index in frames.indices) {
                    if (frames[index] == null) {
                        frames[index] = decodeFrame(index)
                        invalidateTick++
                    }
                }
                synchronized(lock) {
                    if (!tempBitmap.isClosed) {
                        tempBitmap.close()
                    }
                }
            }
    }

    private fun drawFrame(
        canvas: Canvas,
        frameIndex: Int,
    ) {
        val frame = frames[frameIndex] ?: decodeFrame(frameIndex).also { frames[frameIndex] = it }
        val image =
            org.jetbrains.skia.Image.makeRaster(
                imageInfo = codec.imageInfo,
                bytes = frame,
                rowBytes = codec.imageInfo.minRowBytes,
            )
        try {
            canvas.drawImage(
                image = image,
                left = 0f,
                top = 0f,
            )
        } finally {
            image.close()
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

    private fun decodeFrame(frameIndex: Int): ByteArray =
        synchronized(lock) {
            check(!tempBitmap.isClosed) { "Cannot decode frame after bitmap is closed." }
            codec.readPixels(tempBitmap, frameIndex)
            return tempBitmap.readPixels(
                dstInfo = codec.imageInfo,
                dstRowBytes = codec.imageInfo.minRowBytes,
            ) ?: error("Failed to read pixels for frame $frameIndex.")
        }
}

private fun AnimationFrameInfo.safeDurationMillis(): Int {
    return duration.takeIf { it > 0 } ?: DefaultFrameDurationMillis
}

private const val DefaultFrameDurationMillis = 100
