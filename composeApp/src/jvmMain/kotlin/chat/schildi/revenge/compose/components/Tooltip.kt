package chat.schildi.revenge.compose.components

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WithTooltip(
    text: @Composable () -> String,
    modifier: Modifier = Modifier,
    isPersistent: Boolean = false,
    content: @Composable () -> Unit
) {
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
        state = rememberTooltipState(isPersistent = isPersistent),
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        modifier = modifier,
        content = content,
    )
}

@Composable
fun WithTooltip(
    text: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    WithTooltip(
        text = { text },
        modifier = modifier,
        content = content,
    )
}
