package io.element.android.libraries.matrix.ui.media.animated

import coil3.decode.Decoder
import coil3.gif.AnimatedImageDecoder

internal fun AnimatedImageDecoderFactory(): Decoder.Factory = AnimatedImageDecoder.Factory()
