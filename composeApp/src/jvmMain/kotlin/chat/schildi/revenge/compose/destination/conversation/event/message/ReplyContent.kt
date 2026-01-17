package chat.schildi.revenge.compose.destination.conversation.event.message

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.compose.components.LocalSessionId
import chat.schildi.revenge.compose.destination.conversation.event.EventContentLayout
import chat.schildi.revenge.MessageFormatDefaults
import chat.schildi.revenge.model.conversation.messageMetadata
import chat.schildi.theme.scExposures
import io.element.android.libraries.matrix.api.timeline.item.event.InReplyTo
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.reply_failed_to_load

@Composable
fun ReplyContent(
    inReplyTo: InReplyTo,
    modifier: Modifier = Modifier
) {
    // TODO jump to message action
    Box(
        modifier
            .padding(bottom = Dimens.Conversation.replyItemPadding)
            .border(1.dp, MaterialTheme.scExposures.accentColor, Dimens.Conversation.replyContentShape)
            .background(MaterialTheme.colorScheme.surfaceVariant, Dimens.Conversation.replyContentShape)
            .clip(Dimens.Conversation.replyContentShape)
    ) {
        when (inReplyTo) {
            is InReplyTo.Error -> {
                Text(
                    stringResource(Res.string.reply_failed_to_load)
                )
            }
            is InReplyTo.Pending,
            is InReplyTo.NotLoaded -> {
                CircularProgressIndicator()
            }
            is InReplyTo.Ready -> {
                CompositionLocalProvider(
                    LocalMessageRenderContext provides MessageRenderContext.IN_REPLY_TO
                ) {
                    EventContentLayout(
                        content = inReplyTo.content,
                        messageMetadata = inReplyTo.content.messageMetadata(
                            MessageFormatDefaults.parser,
                            MessageFormatDefaults.parseStyle,
                        ),
                        senderId = inReplyTo.senderId,
                        senderProfile = inReplyTo.senderProfile,
                        inReplyTo = null, // No recursive reply lookups please
                        isOwn = inReplyTo.senderId.value == LocalSessionId.current?.value,
                        timestamp = null,
                        isSameAsPreviousSender = false,
                    )
                }
            }
        }
    }
}
