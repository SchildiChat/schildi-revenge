package chat.schildi.revenge.compose.composer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.currentActionContext
import chat.schildi.revenge.compose.destination.conversation.event.message.ReplyContent
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.revenge.model.Attachment
import chat.schildi.revenge.model.ComposerViewModel
import chat.schildi.revenge.model.DraftType
import chat.schildi.theme.scExposures
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.action_add_attachment
import shire.composeapp.generated.resources.action_clear_reply
import shire.composeapp.generated.resources.action_send
import shire.composeapp.generated.resources.hint_composer_audio
import shire.composeapp.generated.resources.hint_composer_caption
import shire.composeapp.generated.resources.hint_composer_edit
import shire.composeapp.generated.resources.hint_composer_edit_caption
import shire.composeapp.generated.resources.hint_composer_emote
import shire.composeapp.generated.resources.hint_composer_file
import shire.composeapp.generated.resources.hint_composer_image
import shire.composeapp.generated.resources.hint_composer_notice
import shire.composeapp.generated.resources.hint_composer_reaction
import shire.composeapp.generated.resources.hint_composer_text
import shire.composeapp.generated.resources.hint_composer_video

// TODO
//  formatted messages: markdown toggle + rendered preview
@Composable
fun ComposerRow(viewModel: ComposerViewModel, modifier: Modifier = Modifier) {
    val draftState = viewModel.composerState.collectAsState().value
    val suggestionsState = viewModel.composerSuggestions.collectAsState().value
    Column(modifier) {
        ComposerSuggestions(suggestionsState, viewModel::onConfirmSuggestion)
        if (draftState.inReplyTo != null) {
            Row(Modifier.padding(horizontal = Dimens.windowPadding), verticalAlignment = Alignment.CenterVertically) {
                ReplyContent(draftState.inReplyTo, Modifier.weight(1f))
                ClearReplyButton(Modifier.padding(start = Dimens.horizontalItemPadding)) {
                    viewModel.onComposerUpdate(draftState.copy(inReplyTo = null))
                }
            }
        }
        if (draftState.attachment != null) {
            ComposerAttachment(draftState.attachment, viewModel::clearAttachment)
        }
        Row(
            verticalAlignment = Alignment.Bottom,
        ) {
            val actionContext = currentActionContext()
            AnimatedVisibility(
                draftState.canAddAttachment(),
            ) {
                IconButton(
                    onClick = { viewModel.launchAttachmentPicker(actionContext) },
                ) {
                    Icon(
                        Icons.Default.Add,
                        stringResource(Res.string.action_add_attachment),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            TextField(
                value = draftState.textFieldValue,
                onValueChange = {
                    viewModel.onComposerUpdate(draftState.copy(textFieldValue = it))
                },
                label = {
                    val hint = when (draftState.type) {
                        DraftType.TEXT -> stringResource(Res.string.hint_composer_text)
                        DraftType.NOTICE -> stringResource(Res.string.hint_composer_notice)
                        DraftType.EMOTE -> stringResource(Res.string.hint_composer_emote)
                        DraftType.EDIT -> stringResource(Res.string.hint_composer_edit)
                        DraftType.EDIT_CAPTION -> stringResource(Res.string.hint_composer_edit_caption)
                        DraftType.REACTION -> stringResource(Res.string.hint_composer_reaction)
                        DraftType.ATTACHMENT -> when (draftState.attachment) {
                            is Attachment.Audio -> stringResource(Res.string.hint_composer_audio)
                            is Attachment.Generic -> stringResource(Res.string.hint_composer_file)
                            is Attachment.Image -> stringResource(Res.string.hint_composer_image)
                            is Attachment.Video -> stringResource(Res.string.hint_composer_video)
                            null -> stringResource(Res.string.hint_composer_caption)
                        }
                    }
                    Text(hint)
                },
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surface)
                    .keyFocusable(role = FocusRole.MESSAGE_COMPOSER),
                colors = TextFieldDefaults.colors().copy(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                )
            )
            SendButton(
                enabled = draftState.canSend(),
                onClick = { viewModel.sendMessage(actionContext) }
            )
        }
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

@Composable
fun ClearReplyButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            Icons.Default.Clear,
            stringResource(Res.string.action_clear_reply),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}
