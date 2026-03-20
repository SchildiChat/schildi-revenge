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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.plus
import okio.use
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import kotlin.time.TimeSource

internal class AnimatedSkiaImageDecoder(
    private val source: ImageSource,
    private val coroutineScope: CoroutineScope,
    private val bufferedFramesCount: Int,
    private val timeSource: TimeSource,
) : Decoder {
    override suspend fun decode(): DecodeResult =
        coroutineScope {
            val bytes = source.source().use { it.readByteArray() }
            val codec = Codec.makeFromData(Data.makeFromBytes(bytes))
            DecodeResult(
                image =
                    AnimatedSkiaImage(
                        codec = codec,
                        coroutineScope = coroutineScope + Job(),
                        bufferedFramesCount = bufferedFramesCount,
                        timeSource = timeSource,
                    ),
                isSampled = false,
            )
        }

    internal class Factory(
        private val bufferedFramesCount: Int = DefaultBufferedFramesCount,
        private val timeSource: TimeSource = TimeSource.Monotonic,
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
                coroutineScope = CoroutineScope(imageLoader.defaults.decoderCoroutineContext),
                bufferedFramesCount = bufferedFramesCount,
                timeSource = timeSource,
            )
        }
    }
}

private const val DefaultBufferedFramesCount = 5
