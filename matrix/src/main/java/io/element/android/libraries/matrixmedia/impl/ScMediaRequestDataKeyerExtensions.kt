package io.element.android.libraries.matrixmedia.impl

import coil3.request.Options
import io.element.android.libraries.matrix.ui.media.animated.allowAnimatedImageDecoding
import io.element.android.libraries.matrixmedia.api.MediaRequestData

internal fun MediaRequestData.toKey(options: Options): String? {
    return source?.let { "${it.safeUrl}_${kind}_${options.allowAnimatedImageDecoding()}" }
}
