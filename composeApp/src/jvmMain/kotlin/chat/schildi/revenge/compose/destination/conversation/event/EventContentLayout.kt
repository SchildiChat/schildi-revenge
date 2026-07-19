package chat.schildi.revenge.compose.destination.conversation.event

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chat.schildi.lib.preferences.ScPrefs
import chat.schildi.revenge.preferences.value
import chat.schildi.revenge.Destination
import chat.schildi.revenge.actions.buildNavigationActionProvider
import chat.schildi.revenge.compose.components.WithTooltip
import chat.schildi.revenge.compose.destination.conversation.event.message.AudioMessage
import chat.schildi.revenge.compose.destination.conversation.event.message.FileMessage
import chat.schildi.revenge.compose.destination.conversation.event.message.MessageLayout
import chat.schildi.revenge.compose.destination.conversation.event.message.ImageMessage
import chat.schildi.revenge.compose.destination.conversation.event.message.LocalThreadReplyContext
import chat.schildi.revenge.compose.destination.conversation.event.message.TextLikeMessage
import chat.schildi.revenge.compose.destination.conversation.event.message.TimestampOverlayContent
import chat.schildi.revenge.compose.destination.conversation.event.message.VideoMessage
import chat.schildi.revenge.compose.destination.conversation.event.message.VoiceMessage
import chat.schildi.revenge.compose.destination.conversation.event.sender.SenderAvatar
import chat.schildi.revenge.compose.destination.conversation.event.sender.SenderName
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.revenge.model.conversation.MessageMetadata
import com.beeper.android.messageformat.MatrixFormatInteractionState
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.room.CreateTimelineParams
import io.element.android.libraries.matrix.api.timeline.item.EventThreadInfo
import io.element.android.libraries.matrix.api.timeline.item.event.AudioMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.CallNotifyContent
import io.element.android.libraries.matrix.api.timeline.item.event.EventContent
import io.element.android.libraries.matrix.api.timeline.item.event.FailedToParseMessageLikeContent
import io.element.android.libraries.matrix.api.timeline.item.event.FailedToParseStateContent
import io.element.android.libraries.matrix.api.timeline.item.event.FileMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.GalleryMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.ImageLikeMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.InReplyTo
import io.element.android.libraries.matrix.api.timeline.item.event.LegacyCallInviteContent
import io.element.android.libraries.matrix.api.timeline.item.event.LiveLocationContent
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
import io.element.android.libraries.matrix.api.timeline.item.event.TextLikeMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.TimelineItemDebugInfoProvider
import io.element.android.libraries.matrix.api.timeline.item.event.UnableToDecryptContent
import io.element.android.libraries.matrix.api.timeline.item.event.UnknownContent
import io.element.android.libraries.matrix.api.timeline.item.event.VideoMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.VoiceMessageType
import org.jetbrains.compose.resources.stringResource
import shire.res.generated.resources.Res
import shire.res.generated.resources.message_placeholder_message_failed_to_parse
import shire.res.generated.resources.message_placeholder_message_redacted
import shire.res.generated.resources.message_placeholder_unable_to_decrypt
import shire.res.generated.resources.message_placeholder_unknown
import shire.res.generated.resources.message_thread

@Composable
fun EventContentLayout(
    content: EventContent,
    messageMetadata: MessageMetadata?,
    senderId: UserId,
    senderProfile: ProfileDetails,
    isOwn: Boolean,
    timestamp: TimestampOverlayContent?,
    isSameAsPreviousSender: Boolean,
    inReplyTo: InReplyTo?,
    threadInfo: EventThreadInfo?,
    modifier: Modifier = Modifier,
    formatInteractionState: MatrixFormatInteractionState? = null,
    timelineItemDebugInfoProvider: TimelineItemDebugInfoProvider? = null,
) {
    @Composable
    fun EventMessageLayout(messageContent: @Composable () -> Unit) {
        MessageLayout(
            modifier = modifier,
            isOwn = isOwn,
            senderAvatar = {
                if (!isSameAsPreviousSender) {
                    SenderAvatar(senderProfile, senderId)
                }
            },
            senderName = {
                if (!isSameAsPreviousSender) {
                    // Render threaded message indicator in the same row as the sender name if
                    val threadReplyContext = LocalThreadReplyContext.current
                    if (threadReplyContext != null) {
                        Row(
                            modifier.width(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SenderName(senderId, senderProfile)
                            // Threaded message indicator
                            WithTooltip(stringResource(Res.string.message_thread)) {
                                Icon(
                                    Icons.Default.Gesture,
                                    stringResource(Res.string.message_thread),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp).keyFocusable(
                                        actionProvider = buildNavigationActionProvider {
                                            Destination.Conversation(
                                                sessionId = threadReplyContext.sessionId,
                                                roomId = threadReplyContext.roomId,
                                                timelineParams = CreateTimelineParams.Threaded(threadReplyContext.threadId),
                                            )
                                        }
                                    ),
                                )
                            }
                        }
                    } else {
                        SenderName(senderId, senderProfile)
                    }
                }
            },
            messageContent = messageContent,
        )
    }
    @Composable
    fun EventMessageFallback(text: String) {
        EventMessageLayout {
            MessageFallback(text, isOwn, timestamp, inReplyTo, threadInfo)
        }
    }
    // TODO make sure every item also renders timestamps in some form
    when (content) {
        is MessageContent -> {
            EventMessageLayout {
                when (val contentType = content.type) {
                    is TextLikeMessageType -> {
                        TextLikeMessage(
                            contentType,
                            messageMetadata,
                            isOwn,
                            timestamp,
                            inReplyTo,
                            threadInfo,
                            interactionState = formatInteractionState,
                        )
                    }
                    is ImageLikeMessageType -> {
                        ImageMessage(contentType, messageMetadata, isOwn, timestamp, inReplyTo, threadInfo)
                    }
                    is LocationMessageType -> {
                        // TODO
                        MessageFallback("LOCATION", isOwn, timestamp, inReplyTo, threadInfo)
                    }
                    is AudioMessageType -> {
                        AudioMessage(contentType, messageMetadata, isOwn, timestamp, inReplyTo, threadInfo)
                    }
                    is FileMessageType -> {
                        FileMessage(contentType, messageMetadata, isOwn, timestamp, inReplyTo, threadInfo)
                    }
                    is VideoMessageType -> {
                        VideoMessage(contentType, messageMetadata, isOwn, timestamp, inReplyTo, threadInfo)
                    }
                    is VoiceMessageType -> {
                        VoiceMessage(contentType, messageMetadata, isOwn, timestamp, inReplyTo, threadInfo)
                    }
                    is GalleryMessageType -> {
                        // TODO
                        MessageFallback("GALLERY", isOwn, timestamp, inReplyTo, threadInfo)
                    }
                    is OtherMessageType -> {
                        MessageFallback(
                            stringResource(Res.string.message_placeholder_unknown),
                            isOwn,
                            timestamp,
                            inReplyTo,
                            threadInfo,
                        )
                    }
                }
            }
        }

        is StickerContent -> EventMessageLayout {
            val caption = content.body?.takeIf { content.filename.isNotEmpty() && content.filename != it }
            ImageMessage(
                source = content.source,
                messageMetadata = messageMetadata,
                caption = caption,
                isOwn = isOwn,
                timestamp = timestamp,
                inReplyTo = inReplyTo,
                threadInfo = threadInfo,
                isSticker = true,
            )
        }

        is RoomMembershipContent -> RoomMembershipRow(content, senderId, senderProfile, timestamp, modifier)
        is ProfileChangeContent -> ProfileChangeRow(content, senderId, senderProfile, timestamp, modifier)
        is StateContent -> StateEventRow(content, senderId, senderProfile, timestamp, modifier)
        is FailedToParseStateContent -> StateEventFallbackRow(content.eventType, senderId, senderProfile, timestamp, modifier)
        is UnknownContent -> {
            if (ScPrefs.VIEW_HIDDEN_EVENTS.value()) {
                HiddenEventRow(timelineItemDebugInfoProvider, senderId, senderProfile, timestamp, modifier)
            } else {
                EventMessageFallback(stringResource(Res.string.message_placeholder_unknown))
            }
        }

        // TODO
        is CallNotifyContent -> EventMessageFallback("CALL")
        is FailedToParseMessageLikeContent -> {
            EventMessageLayout {
                MessageFallback(stringResource(Res.string.message_placeholder_message_failed_to_parse), isOwn, timestamp, inReplyTo, threadInfo, textColor = MaterialTheme.colorScheme.error)
            }
        }
        LegacyCallInviteContent -> EventMessageFallback("LEGACY CALL INVITE")
        is PollContent -> EventMessageFallback("POLL")
        is LiveLocationContent -> EventMessageFallback("LIVE LOCATION")
        RedactedContent -> EventMessageFallback(stringResource(Res.string.message_placeholder_message_redacted)) // TODO can I tell if user deleted themselves or someone else?
        is UnableToDecryptContent -> {
            val message = when (val data = content.data) {
                is UnableToDecryptContent.Data.MegolmV1AesSha2 -> buildString {
                    append(stringResource(Res.string.message_placeholder_unable_to_decrypt))
                    append(" (${data.utdCause.name})")
                }
                is UnableToDecryptContent.Data.OlmV1Curve25519AesSha2 -> buildString {
                    append(stringResource(Res.string.message_placeholder_unable_to_decrypt))
                    append(" (olm)")
                }
                UnableToDecryptContent.Data.Unknown -> stringResource(Res.string.message_placeholder_unable_to_decrypt)
            }
            EventMessageLayout {
                MessageFallback(message, isOwn, timestamp, inReplyTo, threadInfo, textColor = MaterialTheme.colorScheme.error)
            }
        }
    }
}
