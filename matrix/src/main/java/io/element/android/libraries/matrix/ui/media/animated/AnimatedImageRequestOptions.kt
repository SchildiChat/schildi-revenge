package io.element.android.libraries.matrix.ui.media.animated

import coil3.Extras
import coil3.getExtra
import coil3.request.ImageRequest
import coil3.request.Options

fun ImageRequest.Builder.allowAnimatedImageDecoding(enable: Boolean): ImageRequest.Builder {
    extras[AllowAnimatedImageDecodingKey] = enable
    return this
}

internal fun Options.allowAnimatedImageDecoding(): Boolean = getExtra(AllowAnimatedImageDecodingKey)

private val AllowAnimatedImageDecodingKey = Extras.Key(default = true)
