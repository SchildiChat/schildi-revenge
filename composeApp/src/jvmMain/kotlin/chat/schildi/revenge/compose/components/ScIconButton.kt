package chat.schildi.revenge.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * [androidx.compose.material.IconButton] but without hardcoded [androidx.compose.material.MinimumInteractiveModifier]
 */
@Composable
fun ScIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    rippleRadius: Dp = 24.dp,
    minWidth: Dp = 48.dp,
    minHeight: Dp = 48.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            modifier
                .sizeIn(minWidth = minWidth, minHeight = minHeight)
                .clickable(
                    onClick = onClick,
                    enabled = enabled,
                    role = Role.Button,
                    interactionSource = interactionSource,
                    indication = ripple(bounded = false, radius = rippleRadius),
                ),
        contentAlignment = Alignment.Center,
    ) {
        val contentAlpha = if (enabled) LocalContentAlpha.current else ContentAlpha.disabled
        CompositionLocalProvider(LocalContentAlpha provides contentAlpha, content = content)
    }
}
