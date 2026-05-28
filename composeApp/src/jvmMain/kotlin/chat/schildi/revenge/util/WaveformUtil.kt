/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 * Copyright 2023-2026 SchildiChat
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package chat.schildi.revenge.util

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.math.roundToInt

fun ImmutableList<Float>.normalisedWaveform(maxSamplesCount: Int): ImmutableList<Float> {
    if (maxSamplesCount <= 0) {
        return persistentListOf()
    }

    // Filter the data to keep only the expected number of samples
    val result = if (this.size > maxSamplesCount) {
        (0..<maxSamplesCount)
            .map { index ->
                val targetIndex = (index.toDouble() * (this.count().toDouble() / maxSamplesCount.toDouble())).roundToInt()
                this[targetIndex]
            }
    } else {
        this
    }

    return result.toImmutableList()
}
