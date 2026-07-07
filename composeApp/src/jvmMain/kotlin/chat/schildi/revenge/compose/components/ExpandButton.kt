package chat.schildi.revenge.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.InteractionAction
import chat.schildi.revenge.actions.actionProvider
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.theme.scExposures

@Composable
fun ExpandButton(
    text: String,
    expanded: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    enabledColor: Color = MaterialTheme.scExposures.accentColor,
    onClick: () -> Unit,
) {
    val color = if (enabled) {
        enabledColor
    } else {
        MaterialTheme.colorScheme.tertiary
    }
    Row(
        modifier
            .keyFocusable(
                role = FocusRole.LIST_ITEM,
                actionProvider = actionProvider(
                    primaryAction = if (enabled) {
                        InteractionAction.Invoke {
                            onClick()
                            true
                        }
                    } else {
                        null
                    },
                ),
                enableClicks = enabled,
            )
            .padding(Dimens.listPaddingSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            Dimens.horizontalItemPaddingSmall,
            Alignment.CenterHorizontally,
        ),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
        )
        Icon(
            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = color,
        )
    }
}
