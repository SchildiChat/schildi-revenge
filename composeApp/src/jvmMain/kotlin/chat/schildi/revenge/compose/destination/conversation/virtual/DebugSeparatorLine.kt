package chat.schildi.revenge.compose.destination.conversation.virtual

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import chat.schildi.revenge.Dimens

enum class DebugSeparatorLineInstance {
    LiveTimeline,
}

@Composable
fun DebugSeparatorLine(
    instance: DebugSeparatorLineInstance,
    modifier: Modifier = Modifier,
) {
    val color = when (instance) {
        DebugSeparatorLineInstance.LiveTimeline -> MaterialTheme.colorScheme.outline
    }
    DebugSeparatorLine(
        color,
        modifier
            .padding(
                vertical = Dimens.Conversation.unreadLinePadding,
                horizontal = Dimens.windowPadding,
            )
    )
}

@Composable
fun DebugSeparatorLine(color: Color, modifier: Modifier = Modifier) {
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
