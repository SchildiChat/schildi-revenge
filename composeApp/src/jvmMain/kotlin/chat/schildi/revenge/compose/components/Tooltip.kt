package chat.schildi.revenge.compose.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

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
            PlainTooltip {
                Text(text())
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
