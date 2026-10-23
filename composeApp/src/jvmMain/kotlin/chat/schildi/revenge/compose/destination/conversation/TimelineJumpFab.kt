package chat.schildi.revenge.compose.destination.conversation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.Dimens.tween
import chat.schildi.revenge.actions.InteractionAction
import chat.schildi.revenge.actions.actionProvider
import chat.schildi.revenge.compose.components.OverlayIconButton
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
            OverlayIconButton(
                icon,
                contentDescription,
                focusId = focusId,
                actionProvider = actionProvider(
                    primaryAction = InteractionAction.HandleAction(focusId, action),
                ),
                iconSize = Dimens.Conversation.jumpToMessageFabIcon,
                buttonSize = Dimens.Conversation.jumpToMessageFab
            )
        }
    }
}
