package chat.schildi.revenge.compose.destination.conversation.event.message

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun TimestampOverlayLayout(
    contentTextLayoutResult: TextLayoutResult?,
    allowTimestampOverlay: Boolean,
    horizontalPadding: Dp,
    verticalPadding: Dp,
    nonTextWidth: Dp = 0.dp,
    content: @Composable () -> Unit,
    overlay: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        !allowTimestampOverlay -> {
            TimestampBelowLayout(
                verticalPadding = verticalPadding,
                content = content,
                overlay = overlay,
                modifier = modifier,
            )
        }
        contentTextLayoutResult == null -> {
            TimestampOverlayLayout(
                content = content,
                overlay = overlay,
                modifier = modifier,
            )
        }
        else -> {
            TimestampOverlayLayout(
                textLayoutResult = contentTextLayoutResult,
                horizontalPadding = horizontalPadding + nonTextWidth,
                verticalPadding = verticalPadding,
                content = content,
                overlay = overlay,
                modifier = modifier,
            )
        }
    }
}

/**
 * Timestamp overlay layout when unconditionally rendering the timestamp below the main content.
 */
@Composable
fun TimestampBelowLayout(
    verticalPadding: Dp,
    content: @Composable () -> Unit,
    overlay: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Box(
            Modifier.align(Alignment.Start),
        ) {
            content()
        }
        Spacer(Modifier.height(verticalPadding))
        Box(
            Modifier.align(Alignment.End),
        ) {
            overlay()
        }
    }
}

/**
 * Timestamp overlay layout that just always overlays, suitable if the main content does not feature any text in the
 * region of the timestamp overlay.
 */
@Composable
fun TimestampOverlayLayout(
    content: @Composable () -> Unit,
    overlay: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        Box(
            Modifier.align(Alignment.TopStart),
        ) {
            content()
        }
        Box(
            Modifier.align(Alignment.BottomEnd),
        ) {
            overlay()
        }
    }
}

/**
 * Timestamp overlay layout when doing a smart placement depending on last line text layout results and available width.
 */
@Composable
fun TimestampOverlayLayout(
    textLayoutResult: TextLayoutResult,
    horizontalPadding: Dp,
    verticalPadding: Dp,
    content: @Composable () -> Unit,
    overlay: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val horizontalPaddingPx = density.run { horizontalPadding.roundToPx() }
    val verticalPaddingPx = density.run { verticalPadding.roundToPx() }
    // TODO RTL?
    Layout(
        modifier = modifier,
        content = {
            content()
            overlay()
        }
    ) { measurables, constraints ->
        val textMeasurable = measurables[0]
        val overlayMeasurable = measurables[1]

        // Measure main text
        val textPlaceable = textMeasurable.measure(constraints)
        val overlayPlaceable = overlayMeasurable.measure(constraints)

        val lastLineWidth = textLayoutResult.getLineRight(textLayoutResult.lineCount-1).roundToInt()
        val maxAvailableWidth = constraints.maxWidth

        val lastLineWidthWithHorizontalOverlay = lastLineWidth + overlayPlaceable.width + horizontalPaddingPx

        if (maxAvailableWidth >= lastLineWidthWithHorizontalOverlay) {
            // Fits into the last line
            val width = max(textPlaceable.width, lastLineWidthWithHorizontalOverlay)
            val height = max(textPlaceable.height, overlayPlaceable.height)
            layout(width, height) {
                textPlaceable.place(0, 0)
                overlayPlaceable.place(
                    x = width - overlayPlaceable.width,
                    y = textPlaceable.height - overlayPlaceable.height
                )
            }
        } else {
            // Needs a new line
            val width = max(textPlaceable.width, overlayPlaceable.width)
            val height = textPlaceable.height + verticalPaddingPx + overlayPlaceable.height
            layout(width, height) {
                textPlaceable.place(0, 0)
                overlayPlaceable.place(
                    x = width - overlayPlaceable.width,
                    y = textPlaceable.height + verticalPaddingPx
                )
            }
        }
    }
}
