/*
 * Copyright 2024 Coil Contributors
 * Based on: https://github.com/coil-kt/coil/pull/2594
 * by Baptiste Candellier, based on a POC by Colin White.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.element.android.libraries.matrix.ui.media.animated

import coil3.ImageLoader
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import okio.use
import kotlin.coroutines.CoroutineContext
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data

internal class AnimatedSkiaImageDecoder(
    private val source: ImageSource,
    private val bufferedFramesCount: Int,
    private val decoderCoroutineContext: CoroutineContext,
) : Decoder {
    override suspend fun decode(): DecodeResult {
        val bytes = source.source().use { it.readByteArray() }
        val codec = Codec.makeFromData(Data.makeFromBytes(bytes))
        val firstFrame = decodeFrame(codec, 0)
        val imageInfo = codec.imageInfo
        codec.close()
        return DecodeResult(
            image =
                AnimatedSkiaImage(
                    encodedBytes = bytes,
                    imageInfo = imageInfo,
                    firstFrame = firstFrame,
                    bufferedFramesCount = bufferedFramesCount,
                    decoderCoroutineContext = decoderCoroutineContext,
                ),
            isSampled = false,
        )
    }

    internal class Factory(
        private val bufferedFramesCount: Int = DefaultBufferedFramesCount,
    ) : Decoder.Factory {
        init {
            require(bufferedFramesCount >= 0) { "bufferedFramesCount must be >= 0." }
        }

        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            if (!result.source.source().isAnimatedImage()) return null
            return AnimatedSkiaImageDecoder(
                source = result.source,
                bufferedFramesCount = bufferedFramesCount,
                decoderCoroutineContext = imageLoader.defaults.decoderCoroutineContext,
            )
        }
    }
}

private fun decodeFrame(codec: Codec, frameIndex: Int): ByteArray {
    val tempBitmap = Bitmap().apply { allocPixels(codec.imageInfo) }
    return try {
        codec.readPixels(tempBitmap, frameIndex)
        tempBitmap.readPixels(
            dstInfo = codec.imageInfo,
            dstRowBytes = codec.imageInfo.minRowBytes,
        ) ?: error("Failed to read pixels for frame $frameIndex.")
    } finally {
        tempBitmap.close()
    }
}

private const val DefaultBufferedFramesCount = 5
