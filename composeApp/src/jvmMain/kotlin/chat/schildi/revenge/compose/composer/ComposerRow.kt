package chat.schildi.revenge.compose.composer

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.revenge.model.ComposerViewModel
import chat.schildi.revenge.model.DraftType
import chat.schildi.theme.scExposures
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.action_send
import shire.composeapp.generated.resources.hint_composer_emote
import shire.composeapp.generated.resources.hint_composer_notice
import shire.composeapp.generated.resources.hint_composer_text

// TODO
//  formatted messages: markdown toggle + rendered preview
//  intentional mentions: requires auto-completion suggestions etc.
//  attachments (drag&drop? keeps text functionality for captions)
@Composable
fun ComposerRow(viewModel: ComposerViewModel, modifier: Modifier = Modifier) {
    val draftState = viewModel.composerState.collectAsState().value
    Row(modifier) {
        TextField(
            value = draftState.body,
            onValueChange = {
                viewModel.onComposerUpdate(draftState.copy(body = it))
            },
            label = {
                val hint = when (draftState.type) {
                    DraftType.TEXT -> stringResource(Res.string.hint_composer_text)
                    DraftType.NOTICE -> stringResource(Res.string.hint_composer_notice)
                    DraftType.EMOTE -> stringResource(Res.string.hint_composer_emote)
                }
                Text(hint)
            },
            modifier = Modifier
                .weight(1f)
                .background(MaterialTheme.colorScheme.surface)
                .keyFocusable(role = FocusRole.MESSAGE_COMPOSER, isTextField = true),
            colors = TextFieldDefaults.colors().copy(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            )
        )
        SendButton(enabled = draftState.canSend(), onClick = viewModel::sendMessage)
    }
}

@Composable
fun SendButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = animateColorAsState(
        if (enabled)
            MaterialTheme.scExposures.accentColor
        else
            MaterialTheme.colorScheme.onSurfaceVariant
    )
    IconButton(onClick = onClick, enabled = enabled, modifier = modifier) {
        Icon(
            Icons.AutoMirrored.Default.Send,
            stringResource(Res.string.action_send),
            tint = color.value,
        )
    }
}
