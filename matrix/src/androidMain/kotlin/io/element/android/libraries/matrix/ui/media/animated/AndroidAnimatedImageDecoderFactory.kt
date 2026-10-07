package io.element.android.libraries.matrix.ui.media.animated

import android.os.Build
import coil3.decode.Decoder
import coil3.gif.AnimatedImageDecoder

internal fun AnimatedImageDecoderFactory(): Decoder.Factory? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    AnimatedImageDecoder.Factory()
} else {
    null
}
