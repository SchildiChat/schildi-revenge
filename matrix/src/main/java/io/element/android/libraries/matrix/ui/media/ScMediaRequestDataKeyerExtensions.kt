package io.element.android.libraries.matrix.ui.media

import coil3.request.Options
import io.element.android.libraries.matrix.ui.media.animated.allowAnimatedImageDecoding

internal fun MediaRequestData.toKey(options: Options): String? {
    return source?.let { "${it.safeUrl}_${kind}_${options.allowAnimatedImageDecoding()}" }
}
