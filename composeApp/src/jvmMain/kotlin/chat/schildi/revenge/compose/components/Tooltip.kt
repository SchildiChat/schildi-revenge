package chat.schildi.revenge.compose.components

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithTooltip(
    text: @Composable () -> String,
    modifier: Modifier = Modifier,
    isPersistent: Boolean = false,
    hoverOnly: Boolean = false,
    content: @Composable () -> Unit
) {
    val state = rememberTooltipState(isPersistent = isPersistent)
    TooltipBox(
        tooltip = {
            Box(Modifier
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHighest,
                    CircleShape,
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text(),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        },
        state = state,
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        enableUserInput = !hoverOnly,
        modifier = modifier.thenIf(hoverOnly) {
            // Custom hover tracking, since we cannot opt out just of longpress gestures
            pointerInput(state) {
                coroutineScope {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            if (event.changes.firstOrNull()?.type == PointerType.Mouse) {
                                when (event.type) {
                                    PointerEventType.Enter ->
                                        this@coroutineScope.launch { state.show(MutatePriority.UserInput) }
                                    PointerEventType.Exit ->
                                        state.dismiss()
                                }
                            }
                        }
                    }
                }
            }
        },
        content = content,
    )
}

@Composable
fun WithTooltip(
    text: String?,
    modifier: Modifier = Modifier,
    hoverOnly: Boolean = false,
    content: @Composable () -> Unit
) {
    if (text == null) {
        Box(modifier) {
            content()
        }
        return
    }
    WithTooltip(
        text = { text },
        modifier = modifier,
        hoverOnly = hoverOnly,
        content = content,
    )
}
