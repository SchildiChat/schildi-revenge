package chat.schildi.revenge.compose.composer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CrueltyFree
import androidx.compose.material.icons.filled.NoEncryption
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.minimumInteractiveComponentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import chat.schildi.preferences.ScPrefs
import chat.schildi.preferences.value
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.MessageFormatDefaults
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.currentActionContext
import chat.schildi.revenge.compose.components.ScIconButton
import chat.schildi.revenge.compose.components.WithTooltip
import chat.schildi.revenge.compose.destination.conversation.event.message.ReplyContent
import chat.schildi.revenge.compose.destination.conversation.event.message.TextLikeMessageContent
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.revenge.model.Attachment
import chat.schildi.revenge.model.ComposerFormat
import chat.schildi.revenge.model.ComposerRoomInfo
import chat.schildi.revenge.model.ComposerState
import chat.schildi.revenge.model.ComposerViewModel
import chat.schildi.revenge.model.DraftType
import chat.schildi.revenge.model.DraftValue
import chat.schildi.theme.scExposures
import com.beeper.android.messageformat.MatrixBodyParseResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import org.matrix.rustcomponents.sdk.markdownToHtml
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
import shire.composeapp.generated.resources.hint_composer_format_html
import shire.composeapp.generated.resources.hint_composer_format_markdown
import shire.composeapp.generated.resources.hint_composer_format_plain
import shire.composeapp.generated.resources.hint_composer_image
import shire.composeapp.generated.resources.hint_composer_missing_send_permission
import shire.composeapp.generated.resources.hint_composer_notice
import shire.composeapp.generated.resources.hint_composer_reaction
import shire.composeapp.generated.resources.hint_composer_sticker
import shire.composeapp.generated.resources.hint_composer_sticker_shortcode
import shire.composeapp.generated.resources.hint_composer_text
import shire.composeapp.generated.resources.hint_composer_video
import shire.composeapp.generated.resources.hint_not_encrypted
import shire.composeapp.generated.resources.hint_public_room
import kotlin.math.min

@Composable
fun ComposerRow(viewModel: ComposerViewModel, modifier: Modifier = Modifier) {
    when (val draftState = viewModel.composerState.collectAsState().value) {
        is DraftValue -> ComposerRow(draftState, viewModel, modifier)
        is ComposerState.ComposerLessTimeline -> {}
        is ComposerState.NoSendPermission -> BlockedComposer(stringResource(Res.string.hint_composer_missing_send_permission))
    }
}

@Composable
fun ComposerRow(
    draftState: DraftValue,
    viewModel: ComposerViewModel,
    modifier: Modifier = Modifier
) {
    val suggestionsState = viewModel.composerSuggestions.collectAsState().value
    val composerInfo = viewModel.composerRoomInfo.collectAsState().value
    Column(modifier) {
        ComposerSuggestions(
            suggestionsState,
            viewModel::onConfirmSuggestion,
            modifier = Modifier.heightIn(max = 200.dp),
        )
        if (draftState.inReplyTo != null) {
            Row(Modifier.padding(horizontal = Dimens.windowPadding), verticalAlignment = Alignment.CenterVertically) {
                ReplyContent(draftState.inReplyTo, null, Modifier.weight(1f))
                ClearReplyButton(Modifier.padding(start = Dimens.horizontalItemPadding)) {
                    viewModel.onComposerUpdate(draftState.copy(inReplyTo = null))
                }
            }
        }
        if (draftState.attachment != null) {
            ComposerAttachment(draftState.attachment, viewModel::clearAttachment)
        }
        val bodyValidationError = remember(draftState.rawBody) { draftState.bodyValidationError() }
        if (bodyValidationError != null) {
            Text(
                bodyValidationError,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Row(
            verticalAlignment = Alignment.Bottom,
        ) {
            val actionContext = currentActionContext()
            AnimatedVisibility(
                draftState.canAddAttachment(),
            ) {
                WithTooltip(stringResource(Res.string.action_add_attachment)) {
                    ScIconButton(
                        onClick = { viewModel.launchAttachmentPicker(actionContext) },
                        enabled = !draftState.isSendInProgress,
                        minWidth = Dimens.Conversation.Composer.buttonWidth,
                        minHeight = Dimens.Conversation.Composer.buttonHeight,
                    ) {
                        val color = animateColorAsState(
                            if (draftState.isSendInProgress)
                                MaterialTheme.colorScheme.tertiary
                            else
                                MaterialTheme.colorScheme.onSurface,
                        ).value
                        Icon(
                            Icons.Default.Add,
                            stringResource(Res.string.action_add_attachment),
                            tint = color,
                        )
                    }
                }
            }
            if (ScPrefs.FORMATTED_COMPOSER_PREVIEW.value() && draftState.format != ComposerFormat.PLAIN) {
                val content = formattedMessagePreview(draftState)
                TextLikeMessageContent(
                    content,
                    Modifier
                        .fillMaxWidth(fraction = 0.4f)
                        .padding(horizontal = Dimens.horizontalItemPadding)
                        .border(1.dp, MaterialTheme.scExposures.accentColor, Dimens.Conversation.messageBubbleShape)
                        .padding(Dimens.Conversation.messageBubbleInnerPadding),
                )
            }
            var isFocused by remember { mutableStateOf(false) }
            val maxLinesFocused = ScPrefs.COMPOSER_MAX_LINES.value()
            val maxLinesUnfocused = ScPrefs.COMPOSER_MAX_LINES_UNFOCUSED.value()
            TextField(
                value = draftState.textFieldValue,
                onValueChange = {
                    viewModel.onComposerUpdate(draftState.copy(textFieldValue = it))
                },
                label = {
                    val hint = when (draftState.type) {
                        DraftType.TEXT -> stringResource(Res.string.hint_composer_text).appendComposerFormat(draftState)
                        DraftType.NOTICE -> stringResource(Res.string.hint_composer_notice).appendComposerFormat(draftState)
                        DraftType.EMOTE -> stringResource(Res.string.hint_composer_emote).appendComposerFormat(draftState)
                        DraftType.EDIT -> stringResource(Res.string.hint_composer_edit).appendComposerFormat(draftState)
                        DraftType.EDIT_CAPTION -> stringResource(Res.string.hint_composer_edit_caption).appendComposerFormat(draftState)
                        DraftType.REACTION -> stringResource(Res.string.hint_composer_reaction)
                        DraftType.STICKER -> stringResource(Res.string.hint_composer_sticker_shortcode)
                        DraftType.ATTACHMENT -> when (draftState.attachment) {
                            is Attachment.Audio -> stringResource(Res.string.hint_composer_audio)
                            is Attachment.Generic -> stringResource(Res.string.hint_composer_file)
                            is Attachment.Image -> stringResource(Res.string.hint_composer_image)
                            is Attachment.Video -> stringResource(Res.string.hint_composer_video)
                            null -> stringResource(Res.string.hint_composer_caption)
                        }.appendComposerFormat(draftState)
                        DraftType.CUSTOM_EVENT -> draftState.customEventType ?: ""
                        DraftType.CUSTOM_STATE_EVENT -> {
                            if (draftState.stateKey == null) {
                                draftState.customEventType ?: ""
                            } else {
                                buildString {
                                    append(draftState.customEventType ?: "")
                                    append(" ")
                                    append(draftState.stateKey)
                                }
                            }
                        }
                    }
                    val hintColor = animateColorAsState(
                        when (draftState.type) {
                            DraftType.TEXT,
                            DraftType.EDIT,
                            DraftType.EDIT_CAPTION,
                            DraftType.ATTACHMENT -> Color.Unspecified
                            DraftType.STICKER,
                            DraftType.REACTION -> MaterialTheme.scExposures.reactHint
                            DraftType.NOTICE,
                            DraftType.EMOTE,
                            DraftType.CUSTOM_EVENT,
                            DraftType.CUSTOM_STATE_EVENT -> MaterialTheme.scExposures.customEventHint
                        }
                    ).value
                    Row(
                        horizontalArrangement = Dimens.horizontalArrangement,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        when (draftState.type) {
                            DraftType.TEXT,
                            DraftType.NOTICE,
                            DraftType.EMOTE,
                            DraftType.ATTACHMENT -> RoomPrivacyIndicator(composerInfo, hintColor)
                            DraftType.EDIT,
                            DraftType.EDIT_CAPTION,
                            DraftType.REACTION,
                            DraftType.STICKER,
                            DraftType.CUSTOM_EVENT,
                            DraftType.CUSTOM_STATE_EVENT -> {}
                        }
                        Text(hint, color = hintColor)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surface)
                    .keyFocusable(role = FocusRole.MESSAGE_COMPOSER)
                    .onFocusChanged {
                        isFocused = it.isFocused
                    },
                colors = TextFieldDefaults.colors().copy(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                ),
                maxLines = if (isFocused) maxLinesFocused else min(maxLinesUnfocused, maxLinesFocused),
            )
            AnimatedVisibility(
                draftState.type == DraftType.STICKER ||
                        draftState.type == DraftType.TEXT && composerInfo?.canSendStickers == true &&
                                (draftState.isEmpty() || draftState.isValidSticker)
            ) {
                WithTooltip(stringResource(Res.string.hint_composer_sticker)) {
                    ScIconButton(
                        onClick = { viewModel.toggleStickerMode() },
                        enabled = !draftState.isSendInProgress,
                        minWidth = Dimens.Conversation.Composer.buttonWidth,
                        minHeight = Dimens.Conversation.Composer.buttonHeight,
                    ) {
                        val color = animateColorAsState(
                            if (draftState.type == DraftType.STICKER)
                                MaterialTheme.scExposures.accentColor
                            else
                                MaterialTheme.colorScheme.onSurface,
                        ).value
                        Icon(
                            Icons.Default.CrueltyFree,
                            stringResource(Res.string.hint_composer_sticker),
                            tint = color,
                        )
                    }
                }
            }
            AnimatedContent(draftState.isSendInProgress) { isSendInProgress ->
                if (isSendInProgress) {
                    Box(Modifier.minimumInteractiveComponentSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = MaterialTheme.scExposures.accentColor,
                        )
                    }
                } else {
                    SendButton(
                        enabled = draftState.canSend() && bodyValidationError == null,
                        onClick = { viewModel.sendMessage(actionContext) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BlockedComposer(reason: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = Dimens.horizontalItemPaddingBig, vertical = Dimens.listPaddingBig),
        contentAlignment = Alignment.Center,
    ) {
        Text(reason)
    }
}

@Composable
private fun String.appendComposerFormat(draftState: DraftValue): String {
    val host = this
    return buildString {
        append(host)
        append(" (")
        append(
            when (draftState.format) {
                ComposerFormat.PLAIN -> stringResource(Res.string.hint_composer_format_plain)
                ComposerFormat.MARKDOWN -> stringResource(Res.string.hint_composer_format_markdown)
                ComposerFormat.HTML -> stringResource(Res.string.hint_composer_format_html)
            }
        )
        append(")")
    }
}

@Composable
fun RoomPrivacyIndicator(
    composerInfo: ComposerRoomInfo?,
    hintColor: Color,
) {
    val tint = if (hintColor == Color.Unspecified) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        hintColor
    }
    if (composerInfo?.isPublic == true) {
        Icon(
            Icons.Outlined.Public,
            stringResource(Res.string.hint_public_room),
            Modifier.size(12.dp),
            tint = tint,
        )
    } else if (composerInfo?.isEncrypted != true) {
        Icon(
            Icons.Default.NoEncryption,
            stringResource(Res.string.hint_not_encrypted),
            Modifier.size(12.dp),
            tint = tint,
        )
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

@Composable
fun formattedMessagePreview(draft: DraftValue): MatrixBodyParseResult {
    val preview = remember { mutableStateOf<MatrixBodyParseResult?>(null) }
    LaunchedEffect(draft.rawBody, draft.format) {
        withContext(Dispatchers.IO) {
            val parser = MessageFormatDefaults.parser
            val parseStyle = MessageFormatDefaults.parseStyle
            val allowRoomMention = draft.hasRoomMention
            val messageContent = when (draft.format) {
                ComposerFormat.HTML -> parser.parseHtml(draft.htmlBody ?: "", parseStyle, allowRoomMention = allowRoomMention)
                ComposerFormat.PLAIN -> parser.parsePlaintext(draft.body, parseStyle, allowRoomMention = allowRoomMention)
                ComposerFormat.MARKDOWN -> {
                    val body = draft.body
                    val html = markdownToHtml(body) ?: body
                    parser.parseHtml(html, parseStyle, allowRoomMention = allowRoomMention)
                }
            }
            preview.value = messageContent
        }
    }
    return preview.value ?: MatrixBodyParseResult(draft.rawBody)
}
