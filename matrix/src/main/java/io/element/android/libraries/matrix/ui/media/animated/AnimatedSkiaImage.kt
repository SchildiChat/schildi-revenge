/*
 * Copyright 2024 Coil Contributors
 * Based on: https://github.com/coil-kt/coil/pull/2594
 * by Baptiste Candellier, based on a POC by Colin White.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.element.android.libraries.matrix.ui.media.animated

import coil3.Canvas
import coil3.Image
import kotlin.coroutines.CoroutineContext
import org.jetbrains.skia.ImageInfo

class AnimatedSkiaImage(
    val encodedBytes: ByteArray,
    private val imageInfo: ImageInfo,
    val firstFrame: ByteArray,
    val bufferedFramesCount: Int,
    val decoderCoroutineContext: CoroutineContext,
) : Image {
    override val size: Long
        get() = encodedBytes.size.toLong() + firstFrame.size

    override val width: Int
        get() = imageInfo.width

    override val height: Int
        get() = imageInfo.height

    override val shareable: Boolean = true

    override fun draw(canvas: Canvas) {
        val image =
            org.jetbrains.skia.Image.makeRaster(
                imageInfo = imageInfo,
                bytes = firstFrame,
                rowBytes = imageInfo.minRowBytes,
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
}
