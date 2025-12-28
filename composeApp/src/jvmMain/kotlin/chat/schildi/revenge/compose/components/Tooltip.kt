package chat.schildi.revenge.compose.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WithTooltip(text: String, content: @Composable () -> Unit) {
    TooltipBox(
        tooltip = {
            PlainTooltip {
                Text(
                    text,
                )
            }
        },
        state = rememberTooltipState(),
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        content = content,
    )
}
