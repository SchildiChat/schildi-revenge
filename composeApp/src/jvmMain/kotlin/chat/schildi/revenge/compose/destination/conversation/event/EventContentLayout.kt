package chat.schildi.revenge.compose.destination.conversation.event

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import chat.schildi.revenge.compose.destination.conversation.event.message.MessageLayout
import chat.schildi.revenge.compose.destination.conversation.event.message.ImageMessage
import chat.schildi.revenge.compose.destination.conversation.event.message.TextLikeMessage
import chat.schildi.revenge.compose.destination.conversation.event.sender.SenderAvatar
import chat.schildi.revenge.compose.destination.conversation.event.sender.SenderName
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.timeline.item.event.AudioMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.CallNotifyContent
import io.element.android.libraries.matrix.api.timeline.item.event.EmoteMessageType
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

@Composable
fun EventContentLayout(
    content: EventContent,
    senderId: UserId,
    senderProfile: ProfileDetails,
    isOwn: Boolean,
    isSameAsPreviousSender: Boolean,
    inReplyTo: InReplyTo?,
    modifier: Modifier = Modifier
) {
    // TODO make sure every item also renders timestamps in some form
    when (val content = content) {
        is MessageContent -> {
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
                messageContent = {
                    when (val contentType = content.type) {
                        is TextLikeMessageType -> {
                            TextLikeMessage(contentType, isOwn, inReplyTo)
                        }
                        is ImageMessageType -> {
                            ImageMessage(contentType, isOwn, inReplyTo)
                        }
                        is EmoteMessageType -> {
                            // TODO
                            Text("EMOTE")
                        }
                        is LocationMessageType -> {
                            // TODO
                            Text("LOCATION")
                        }
                        is AudioMessageType -> {
                            // TODO
                            Text("AUDIO")
                        }
                        is FileMessageType -> {
                            // TODO
                            Text("FILE")
                        }
                        is StickerMessageType -> {
                            // TODO
                            Text("STICKER")
                        }
                        is VideoMessageType -> {
                            // TODO
                            Text("VIDEO")
                        }
                        is VoiceMessageType -> {
                            // TODO
                            Text("VOICE")
                        }
                        is OtherMessageType -> {
                            // TODO
                            Text("OTHER_MESSAGE")
                        }
                    }
                },
                reactions = {
                    // TODO
                },
            )
        }

        // TODO
        CallNotifyContent -> Text("CALL")
        is FailedToParseMessageLikeContent -> Text("FAILED TO PARSE")
        is FailedToParseStateContent -> Text("FAILED TO PARSE STATE")
        LegacyCallInviteContent -> Text("LEGACY CALL INVITE")
        is PollContent -> Text("POLL")
        is ProfileChangeContent -> Text("PROFILE CHANGE")
        RedactedContent -> Text("REDACTED")
        is RoomMembershipContent -> Text("MEMBERSHIP")
        is StateContent -> Text("STATE")
        is StickerContent -> Text("STICKER")
        is UnableToDecryptContent -> Text("UTD")
        UnknownContent -> Text("UNKNOWN")
    }
}
