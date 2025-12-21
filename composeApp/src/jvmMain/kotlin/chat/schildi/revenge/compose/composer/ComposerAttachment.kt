package chat.schildi.revenge.compose.composer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.compose.destination.conversation.event.message.ImageMessage
import chat.schildi.revenge.compose.destination.conversation.event.message.ImageMessageContent
import chat.schildi.revenge.compose.destination.conversation.event.message.LocalMessageRenderContext
import chat.schildi.revenge.compose.destination.conversation.event.message.MessageRenderContext
import chat.schildi.revenge.model.Attachment
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.action_clear_attachment

@Composable
fun ComposerAttachment(
    attachment: Attachment,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // TODO
    Row(
        modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp))
            .padding(Dimens.Conversation.messageBubbleInnerPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            when (attachment) {
                is Attachment.Audio -> {} // TODO
                is Attachment.Generic -> {} // TODO
                is Attachment.Image -> {
                    ImageMessageContent(
                        model = attachment.file,
                        minWidth = Dimens.Conversation.imageMinWidth,
                        minHeight = Dimens.Conversation.imageMinHeight,
                        maxWidth = Dimens.Conversation.imageMaxWidth,
                        maxHeight = Dimens.Conversation.imageRepliedToMaxHeight,
                    )
                }

                is Attachment.Video -> {} // TODO
            }
            Text(
                attachment.file.absolutePath,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        IconButton(onClick = onRemoveClick) {
            Icon(
                Icons.Default.Clear,
                stringResource(Res.string.action_clear_attachment),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
