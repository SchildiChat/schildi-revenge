package chat.schildi.revenge.compose.destination.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.LocalMatrixBodyDrawStyle
import chat.schildi.revenge.LocalMatrixBodyFormatter
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.InteractionAction
import chat.schildi.revenge.actions.actionProvider
import chat.schildi.revenge.compose.composer.ComposerAttachmentContent
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.revenge.matrixBodyDrawStyle
import chat.schildi.revenge.matrixBodyFormatter
import chat.schildi.revenge.model.Attachment
import chat.schildi.revenge.model.IncomingShare
import chat.schildi.revenge.model.PendingShare
import chat.schildi.revenge.model.buildAttachmentForFile
import org.jetbrains.compose.resources.stringResource
import shire.res.generated.resources.Res
import shire.res.generated.resources.action_clear_attachment
import shire.res.generated.resources.incoming_share_pending

@Composable
fun IncomingShareNoticeCard(
    share: PendingShare,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, Dimens.Conversation.messageBubbleShape)
            .border(1.dp, MaterialTheme.colorScheme.outline, Dimens.Conversation.messageBubbleShape)
            .padding(Dimens.Conversation.messageBubbleInnerPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Dimens.horizontalArrangement,
    ) {
        Column(Modifier.weight(1f, fill = false)) {
            Text(
                text = stringResource(Res.string.incoming_share_pending),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            var attachment by remember(share.file) { mutableStateOf<Attachment?>(null) }
            LaunchedEffect(share.file) {
                attachment = share.file?.file?.let { file ->
                    buildAttachmentForFile(
                        file = file,
                        isFileAppOwned = share.file.isAppOwned,
                    )
                }
            }
            attachment?.let { attachment ->
                CompositionLocalProvider(
                    LocalMatrixBodyFormatter provides matrixBodyFormatter(),
                    LocalMatrixBodyDrawStyle provides matrixBodyDrawStyle(),
                ) {
                    ComposerAttachmentContent(
                        attachment = attachment,
                        modifier = Modifier.heightIn(max = Dimens.Inbox.incomingShareAttachmentMaxHeight),
                    )
                }
            }

            share.text?.takeIf { it.isNotBlank() }?.let { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(
            Icons.Default.Clear,
            stringResource(Res.string.action_clear_attachment),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .keyFocusable(
                    FocusRole.AUX_ITEM,
                    actionProvider = actionProvider(
                        primaryAction = InteractionAction.Invoke {
                            IncomingShare.clear()
                            true
                        },
                    ),
                )
                .padding(8.dp)
        )
    }
}
