package chat.schildi.revenge.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.InteractionAction
import chat.schildi.revenge.actions.defaultActionProvider
import chat.schildi.revenge.compose.focus.keyFocusable
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.action_cancel
import shire.composeapp.generated.resources.action_logout

@Composable
fun IconButtonWithConfirmation(
    icon: ImageVector,
    confirmText: String,
    iconTint: Color = MaterialTheme.colorScheme.error,
    iconTintActive: Color = MaterialTheme.colorScheme.onErrorContainer,
    iconBackground: Color = Color.Transparent,
    iconBackgroundActive: Color = MaterialTheme.colorScheme.errorContainer,
    modifier: Modifier = Modifier,
    cancelText: String = stringResource(Res.string.action_cancel),
    onConfirm: () -> Unit,
) {
    ButtonWithConfirmation(
        confirmText = confirmText,
        onConfirm = onConfirm,
        modifier = modifier,
        cancelText = cancelText,
    ) { innerModifier, confirmationVisible, onClick ->
        val bgColor = animateColorAsState(if (confirmationVisible) iconBackgroundActive else iconBackground)
        val fgColor = animateColorAsState(if (confirmationVisible) iconTintActive else iconTint)
        IconButton(modifier = innerModifier.background(bgColor.value, CircleShape), onClick = onClick) {
            Icon(
                icon,
                contentDescription = stringResource(Res.string.action_logout),
                tint = fgColor.value,
            )
        }
    }
}

@Composable
fun ButtonWithConfirmation(
    confirmText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    cancelText: String = stringResource(Res.string.action_cancel),
    focusRole: FocusRole = FocusRole.NESTED_AUX_ITEM,
    button: @Composable (Modifier, confirmationVisible: Boolean, onClick: () -> Unit) -> Unit,
) {
    var confirmationVisible by remember { mutableStateOf(false) }
    var hasFocus by remember { mutableStateOf(false) }
    LaunchedEffect(confirmationVisible, hasFocus) {
        if (confirmationVisible && !hasFocus) {
            delay(500)
            if (confirmationVisible && !hasFocus) {
                confirmationVisible = false
            }
        }
    }
    Row(
        modifier.onFocusChanged {
            hasFocus = it.hasFocus
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Dimens.horizontalArrangement,
    ) {
        button(
            Modifier
                .keyFocusable(
                    role = focusRole,
                    actionProvider = defaultActionProvider(
                        primaryAction = InteractionAction.Invoke {
                            confirmationVisible = !confirmationVisible
                            true
                        },
                    ),
                    addClickListener = false,
                ),
            confirmationVisible,
        ) {
            confirmationVisible = !confirmationVisible
        }
        AnimatedVisibility(
            visible = confirmationVisible,
        ) {
            val confirmButtonFocusRequester = remember { FocusRequester() }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Dimens.horizontalArrangement,
            ) {
                Button(
                    modifier = Modifier.keyFocusable(
                        focusRequester = confirmButtonFocusRequester,
                        actionProvider = defaultActionProvider(
                            primaryAction = InteractionAction.Invoke {
                                onConfirm()
                                true
                            },
                        ),
                        addMouseFocusable = false,
                        addClickListener = false,
                    ),
                    onClick = onConfirm,
                ) {
                    Text(confirmText)
                }
                Button(
                    modifier = Modifier.keyFocusable(
                        actionProvider = defaultActionProvider(
                            primaryAction = InteractionAction.Invoke {
                                confirmationVisible = false
                                true
                            },
                        ),
                        addMouseFocusable = false,
                        addClickListener = false,
                    ),
                    onClick = {
                        confirmationVisible = false
                    },
                ) {
                    Text(cancelText)
                }
            }
        }
    }
}
