/*
 * Copyright 2024 Coil Contributors
 * Based on: https://github.com/coil-kt/coil/pull/2594
 * by Baptiste Candellier, based on a POC by Colin White.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.element.android.libraries.matrix.ui.media.animated

import okio.BufferedSource
import okio.ByteString.Companion.encodeUtf8
import kotlin.experimental.and

private val GifHeader87a = "GIF87a".encodeUtf8()
private val GifHeader89a = "GIF89a".encodeUtf8()
private val WebpHeaderRiff = "RIFF".encodeUtf8()
private val WebpHeaderWebp = "WEBP".encodeUtf8()
private val WebpHeaderVp8x = "VP8X".encodeUtf8()

internal fun BufferedSource.isGif(): Boolean {
    return rangeEquals(0, GifHeader87a) || rangeEquals(0, GifHeader89a)
}

internal fun BufferedSource.isAnimatedWebP(): Boolean {
    return rangeEquals(0, WebpHeaderRiff) &&
        rangeEquals(8, WebpHeaderWebp) &&
        rangeEquals(12, WebpHeaderVp8x) &&
        request(21) &&
        (buffer[20] and 0b00000010) > 0
}

internal fun BufferedSource.isAnimatedImage(): Boolean = isGif() || isAnimatedWebP()
