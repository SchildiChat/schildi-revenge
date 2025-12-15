package chat.schildi.revenge.compose.destination.conversation.event

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import chat.schildi.revenge.compose.destination.conversation.event.message.MessageLayout
import chat.schildi.revenge.compose.destination.conversation.event.message.ImageMessage
import chat.schildi.revenge.compose.destination.conversation.event.message.TextLikeMessage
import chat.schildi.revenge.compose.destination.conversation.event.message.TimestampOverlayContent
import chat.schildi.revenge.compose.destination.conversation.event.sender.SenderAvatar
import chat.schildi.revenge.compose.destination.conversation.event.sender.SenderName
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.timeline.item.event.AudioMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.CallNotifyContent
import io.element.android.libraries.matrix.api.timeline.item.event.EventContent
import io.element.android.libraries.matrix.api.timeline.item.event.FailedToParseMessageLikeContent
import io.element.android.libraries.matrix.api.timeline.item.event.FailedToParseStateContent
import io.element.android.libraries.matrix.api.timeline.item.event.FileMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.ImageMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.InReplyTo
import io.element.android.libraries.matrix.api.timeline.item.event.LegacyCallInviteContent
import io.element.android.libraries.matrix.api.timeline.item.event.LocationMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.MessageContent
import io.element.android.libraries.matrix.api.timeline.item.event.OtherMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.PollContent
import io.element.android.libraries.matrix.api.timeline.item.event.ProfileChangeContent
import io.element.android.libraries.matrix.api.timeline.item.event.ProfileDetails
import io.element.android.libraries.matrix.api.timeline.item.event.RedactedContent
import io.element.android.libraries.matrix.api.timeline.item.event.RoomMembershipContent
import io.element.android.libraries.matrix.api.timeline.item.event.StateContent
import io.element.android.libraries.matrix.api.timeline.item.event.StickerContent
import io.element.android.libraries.matrix.api.timeline.item.event.StickerMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.TextLikeMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.UnableToDecryptContent
import io.element.android.libraries.matrix.api.timeline.item.event.UnknownContent
import io.element.android.libraries.matrix.api.timeline.item.event.VideoMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.VoiceMessageType
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.message_placeholder_message_failed_to_parse
import shire.composeapp.generated.resources.message_placeholder_message_redacted
import shire.composeapp.generated.resources.message_placeholder_unable_to_decrypt

@Composable
fun EventContentLayout(
    content: EventContent,
    senderId: UserId,
    senderProfile: ProfileDetails,
    isOwn: Boolean,
    timestamp: TimestampOverlayContent?,
    isSameAsPreviousSender: Boolean,
    inReplyTo: InReplyTo?,
    modifier: Modifier = Modifier
) {
    @Composable
    fun EventMessageLayout(messageContent: @Composable () -> Unit) {
        MessageLayout(
            modifier = modifier,
            isOwn = isOwn,
            senderAvatar = {
                if (!isSameAsPreviousSender) {
                    SenderAvatar(senderProfile)
                }
            },
            senderName = {
                if (!isSameAsPreviousSender) {
                    SenderName(senderId, senderProfile)
                }
            },
            messageContent = messageContent,
            reactions = {
                // TODO
            }
        )
    }
    @Composable
    fun EventMessageFallback(text: String) {
        EventMessageLayout {
            MessageFallback(text, isOwn, timestamp, inReplyTo)
        }
    }
    // TODO make sure every item also renders timestamps in some form
    when (content) {
        is MessageContent -> {
            EventMessageLayout {
                when (val contentType = content.type) {
                    is TextLikeMessageType -> {
                        TextLikeMessage(contentType, isOwn, timestamp, inReplyTo)
                    }
                    is ImageMessageType -> {
                        ImageMessage(contentType, isOwn, timestamp, inReplyTo)
                    }
                    is LocationMessageType -> {
                        // TODO
                        MessageFallback("LOCATION", isOwn, timestamp, inReplyTo)
                    }
                    is AudioMessageType -> {
                        // TODO
                        MessageFallback("AUDIO", isOwn, timestamp, inReplyTo)
                    }
                    is FileMessageType -> {
                        // TODO
                        MessageFallback("FILE", isOwn, timestamp, inReplyTo)
                    }
                    is StickerMessageType -> {
                        // TODO
                        MessageFallback("STICKER", isOwn, timestamp, inReplyTo)
                    }
                    is VideoMessageType -> {
                        // TODO
                        MessageFallback("VIDEO", isOwn, timestamp, inReplyTo)
                    }
                    is VoiceMessageType -> {
                        // TODO
                        MessageFallback("VOICE", isOwn, timestamp, inReplyTo)
                    }
                    is OtherMessageType -> {
                        // TODO
                        MessageFallback("OTHER_MESSAGE", isOwn, timestamp, inReplyTo)
                    }
                }
            }
        }

        is RoomMembershipContent -> RoomMembershipRow(content, senderId, senderProfile, timestamp, modifier)
        is ProfileChangeContent -> ProfileChangeRow(content, senderId, senderProfile, timestamp, modifier)

        // TODO
        CallNotifyContent -> EventMessageFallback("CALL")
        is FailedToParseMessageLikeContent -> EventMessageFallback(stringResource(Res.string.message_placeholder_message_failed_to_parse))
        is FailedToParseStateContent -> EventMessageFallback("FAILED TO PARSE STATE")
        LegacyCallInviteContent -> EventMessageFallback("LEGACY CALL INVITE")
        is PollContent -> EventMessageFallback("POLL")
        RedactedContent -> EventMessageFallback(stringResource(Res.string.message_placeholder_message_redacted)) // TODO can I tell if user deleted themselves or someone else?
        is StateContent -> EventMessageFallback("STATE")
        is StickerContent -> EventMessageFallback("STICKER")
        is UnableToDecryptContent -> EventMessageFallback(stringResource(Res.string.message_placeholder_unable_to_decrypt))
        UnknownContent -> EventMessageFallback("UNKNOWN")
    }
}
