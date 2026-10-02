package chat.schildi.revenge.compose.destination.conversation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.Dimens.tween
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.InteractionAction
import chat.schildi.revenge.actions.actionProvider
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.revenge.compose.focus.rememberFocusId
import chat.schildi.revenge.config.keybindings.Action

@Composable
fun TimelineJumpFab(
    icon: ImageVector,
    contentDescription: String?,
    action: Action.Conversation,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
) {
    // Always reserve space
    Box(modifier.size(Dimens.Conversation.jumpToMessageFab)) {
        AnimatedVisibility(
            isVisible,
            enter = scaleIn(animationSpec = tween(), initialScale = 0.8f) + fadeIn(animationSpec = tween()),
            exit = scaleOut(animationSpec = tween(), targetScale = 0.8f) + fadeOut(animationSpec = tween()),
        ) {
            val focusId = rememberFocusId()
            Box(
                modifier = Modifier.keyFocusable(
                    id = focusId,
                    role = FocusRole.AUX_ITEM,
                    actionProvider = actionProvider(
                        primaryAction = InteractionAction.HandleAction(focusId, action),
                    )
                )
                    .size(Dimens.Conversation.jumpToMessageFab)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        CircleShape,
                    ).border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription,
                    modifier = Modifier.size(Dimens.Conversation.jumpToMessageFabIcon),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
