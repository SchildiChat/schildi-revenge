package chat.schildi.revenge.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.ActionProvider
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.revenge.compose.focus.rememberFocusId
import kotlin.uuid.Uuid

@Composable
fun OverlayIconButton(
    icon: ImageVector,
    contentDescription: String?,
    actionProvider: ActionProvider,
    iconSize: Dp,
    buttonSize: Dp,
    modifier: Modifier = Modifier,
    focusId: Uuid = rememberFocusId(),
    role: FocusRole = FocusRole.AUX_ITEM,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    outlineColor: Color = MaterialTheme.colorScheme.outlineVariant,
) {
    Box(
        modifier = modifier.keyFocusable(
            id = focusId,
            role = role,
            actionProvider = actionProvider,
            shape = CircleShape,
        )
            .size(buttonSize)
            .background(
                backgroundColor,
                CircleShape,
            ).border(
                1.dp,
                outlineColor,
                CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription,
            modifier = Modifier.size(iconSize),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
