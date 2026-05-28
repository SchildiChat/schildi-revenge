/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 * Copyright 2026 SchildiChat
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package chat.schildi.revenge.compose.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.Dp
import chat.schildi.revenge.Dimens
import kotlinx.collections.immutable.ImmutableList
import kotlin.math.max

fun DrawScope.drawWaveform(
    waveformData: ImmutableList<Float>,
    canvasSizePx: Size,
    brush: Brush,
    minimumGraphAmplitude: Float = 2F,
    lineWidth: Dp = Dimens.Conversation.AudioWaveform.lineWidth,
    linePadding: Dp = Dimens.Conversation.AudioWaveform.linePadding,
) {
    val centerY = canvasSizePx.height / 2
    val cornerRadius = lineWidth / 2
    waveformData.forEachIndexed { index, amplitude ->
        val drawingAmplitude = max(minimumGraphAmplitude, amplitude * (canvasSizePx.height - 2))
        drawRoundRect(
            brush = brush,
            topLeft = Offset(
                x = index * (linePadding + lineWidth).toPx(),
                y = centerY - drawingAmplitude / 2
            ),
            size = Size(
                width = lineWidth.toPx(),
                height = drawingAmplitude
            ),
            cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
            style = Fill
        )
    }
}
