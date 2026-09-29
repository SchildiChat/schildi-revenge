package chat.schildi.revenge.compose.destination.conversation.virtual

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import chat.schildi.revenge.Dimens
import chat.schildi.theme.ScColors
import chat.schildi.theme.scExposures
import kotlin.math.min

enum class DebugLinePosition {
    Above,
    Start,
}

enum class TimelineItemDebugLineInstance(
    val position: DebugLinePosition,
    val color: @Composable () -> Color,
) {
    LiveTimeline(DebugLinePosition.Above, { MaterialTheme.colorScheme.outline }),
    PendingTrackedRead(DebugLinePosition.Start, { ScColors.colorAccentLime }),
    TrackedRead(DebugLinePosition.Start, { MaterialTheme.scExposures.accentColor }),
}

@Composable
fun TimelineItemDebugLine(
    instance: TimelineItemDebugLineInstance,
    modifier: Modifier = Modifier,
) {
    when (instance.position) {
        DebugLinePosition.Above -> {
            HorizontalTimelineItemDebugLine(
                instance.color(),
                modifier
                    .padding(
                        vertical = Dimens.Conversation.unreadLinePadding,
                        horizontal = Dimens.windowPadding,
                    )
            )
        }
        DebugLinePosition.Start -> return
    }
}

@Composable
fun Modifier.drawTimelineItemDebugLineBehind(
    instance: TimelineItemDebugLineInstance,
): Modifier {
    val color = instance.color()
    val paddingPx = LocalDensity.current.run { Dimens.horizontalItemPaddingSmall.toPx() }
    val widthPx = LocalDensity.current.run { Dimens.Conversation.newMessagesLineHeight.toPx() }
    return drawBehind {
        when (instance.position) {
            DebugLinePosition.Above -> return@drawBehind
            DebugLinePosition.Start -> {
                val x = min(size.width/2, paddingPx)
                drawLine(
                    color = color,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = min(widthPx, size.width),
                    //pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )
            }
        }
    }
}

@Composable
private fun HorizontalTimelineItemDebugLine(color: Color, modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .fillMaxWidth()
            .height(Dimens.Conversation.newMessagesLineHeight)
    ) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = size.height,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
        )
    }
}
