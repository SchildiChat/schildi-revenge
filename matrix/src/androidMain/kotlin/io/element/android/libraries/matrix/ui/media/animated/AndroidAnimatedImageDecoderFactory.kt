package io.element.android.libraries.matrix.ui.media.animated

import android.os.Build
import coil3.ImageLoader
import coil3.decode.BitmapFactoryDecoder
import coil3.decode.Decoder
import coil3.fetch.SourceFetchResult
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.request.Options

internal fun platformAnimatedImageDecoderFactory(): Decoder.Factory =
    object : Decoder.Factory {
        private val animatedFactory: Decoder.Factory = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            AnimatedImageDecoder.Factory()
        } else {
            GifDecoder.Factory()
        }
        private val staticFactory = BitmapFactoryDecoder.Factory()

        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            // Create animated decoder also if animation is disabled, as check if this is animated in the first place.
            val animatedDecoder = animatedFactory.create(result, options, imageLoader) ?: return null
            if (options.allowAnimatedImageDecoding()) return animatedDecoder
            // Need to be explicit about a static image here to not fall back to animated decoders from coil.
            return staticFactory.create(result, options, imageLoader)
        }
    }
