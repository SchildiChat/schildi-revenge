package chat.schildi.revenge

import androidx.compose.runtime.Composable
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.timeline.item.event.AudioMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.CallNotifyContent
import io.element.android.libraries.matrix.api.timeline.item.event.EmoteMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.EventContent
import io.element.android.libraries.matrix.api.timeline.item.event.FailedToParseMessageLikeContent
import io.element.android.libraries.matrix.api.timeline.item.event.FailedToParseStateContent
import io.element.android.libraries.matrix.api.timeline.item.event.FileMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.ImageMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.LegacyCallInviteContent
import io.element.android.libraries.matrix.api.timeline.item.event.LocationMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.MembershipChange
import io.element.android.libraries.matrix.api.timeline.item.event.MessageContent
import io.element.android.libraries.matrix.api.timeline.item.event.NoticeMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.OtherMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.PollContent
import io.element.android.libraries.matrix.api.timeline.item.event.ProfileChangeContent
import io.element.android.libraries.matrix.api.timeline.item.event.ProfileDetails
import io.element.android.libraries.matrix.api.timeline.item.event.RedactedContent
import io.element.android.libraries.matrix.api.timeline.item.event.RoomMembershipContent
import io.element.android.libraries.matrix.api.timeline.item.event.StateContent
import io.element.android.libraries.matrix.api.timeline.item.event.StickerContent
import io.element.android.libraries.matrix.api.timeline.item.event.StickerMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.TextMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.UnableToDecryptContent
import io.element.android.libraries.matrix.api.timeline.item.event.UnknownContent
import io.element.android.libraries.matrix.api.timeline.item.event.VideoMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.VoiceMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.getDisambiguatedDisplayName
import io.element.android.libraries.matrix.api.timeline.item.event.getDisplayName
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.membership_change_banned
import shire.composeapp.generated.resources.membership_change_error
import shire.composeapp.generated.resources.membership_change_invitation_accepted
import shire.composeapp.generated.resources.membership_change_invitation_rejected
import shire.composeapp.generated.resources.membership_change_invitation_revoked
import shire.composeapp.generated.resources.membership_change_invited
import shire.composeapp.generated.resources.membership_change_joined
import shire.composeapp.generated.resources.membership_change_kicked
import shire.composeapp.generated.resources.membership_change_kicked_and_banned
import shire.composeapp.generated.resources.membership_change_knock_accepted
import shire.composeapp.generated.resources.membership_change_knock_denied
import shire.composeapp.generated.resources.membership_change_knock_retracted
import shire.composeapp.generated.resources.membership_change_knocked
import shire.composeapp.generated.resources.membership_change_left
import shire.composeapp.generated.resources.membership_change_none
import shire.composeapp.generated.resources.membership_change_not_implemented
import shire.composeapp.generated.resources.membership_change_unbanned
import shire.composeapp.generated.resources.membership_reason
import shire.composeapp.generated.resources.message_placeholder_call
import shire.composeapp.generated.resources.message_placeholder_message_failed_to_parse
import shire.composeapp.generated.resources.message_placeholder_message_redacted
import shire.composeapp.generated.resources.message_placeholder_state
import shire.composeapp.generated.resources.message_placeholder_unable_to_decrypt
import shire.composeapp.generated.resources.message_placeholder_unknown
import shire.composeapp.generated.resources.profile_update_avatar
import shire.composeapp.generated.resources.profile_update_cleared_name
import shire.composeapp.generated.resources.profile_update_name
import shire.composeapp.generated.resources.profile_update_name_and_avatar
import shire.composeapp.generated.resources.profile_update_none
import shire.composeapp.generated.resources.profile_update_set_name
import shire.composeapp.generated.resources.profile_update_set_name_and_avatar

object EventTextFormat {
    @Composable
    fun eventToText(content: EventContent, senderProfile: ProfileDetails, senderId: UserId): String {
        return when (content) {
            is MessageContent -> {
                when (val type = content.type) {
                    is EmoteMessageType -> type.body
                    is LocationMessageType -> type.body
                    is AudioMessageType -> type.bestDescription
                    is FileMessageType -> type.bestDescription
                    is ImageMessageType -> type.bestDescription
                    is StickerMessageType -> type.bestDescription
                    is VideoMessageType -> type.bestDescription
                    is VoiceMessageType -> type.bestDescription
                    is OtherMessageType -> type.body
                    is NoticeMessageType -> type.body
                    is TextMessageType -> type.body
                }
            }
            is StickerContent -> content.bestDescription
            CallNotifyContent,
            LegacyCallInviteContent -> stringResource(Res.string.message_placeholder_call)
            is FailedToParseMessageLikeContent,
            is FailedToParseStateContent -> stringResource(Res.string.message_placeholder_message_failed_to_parse)
            is PollContent -> content.question
            is ProfileChangeContent -> profileChangeToText(content, senderProfile, senderId)
            RedactedContent -> stringResource(Res.string.message_placeholder_message_redacted)
            is RoomMembershipContent -> roomMembershipToText(content, senderProfile, senderId)
            is StateContent -> stringResource(Res.string.message_placeholder_state)
            is UnableToDecryptContent -> stringResource(Res.string.message_placeholder_unable_to_decrypt)
            UnknownContent -> stringResource(Res.string.message_placeholder_unknown)
        }
    }

    @Composable
    fun profileChangeToText(content: ProfileChangeContent, senderProfile: ProfileDetails, senderId: UserId): String {
        val senderName = senderProfile.getDisambiguatedDisplayName(senderId)
        return if (content.prevDisplayName == content.displayName) {
            if (content.prevAvatarUrl == content.avatarUrl) {
                stringResource(Res.string.profile_update_none, senderName)
            } else {
                stringResource(Res.string.profile_update_avatar, senderName)
            }
        } else if (content.prevAvatarUrl != content.avatarUrl) {
            if (content.prevDisplayName == null) {
                stringResource(Res.string.profile_update_set_name_and_avatar, senderName)
            } else {
                stringResource(Res.string.profile_update_name_and_avatar, senderName, content.prevDisplayName ?: "")
            }
        } else {
            when {
                content.prevDisplayName == null -> stringResource(Res.string.profile_update_set_name, senderName)
                content.displayName == null -> stringResource(
                    Res.string.profile_update_cleared_name,
                    senderName,
                    content.prevDisplayName ?: ""
                )

                else -> stringResource(Res.string.profile_update_name, senderName, content.prevDisplayName ?: "")
            }
        }
    }

    @Composable
    fun roomMembershipToText(content: RoomMembershipContent, senderProfile: ProfileDetails, senderId: UserId): String {
        val senderName = senderProfile.getDisambiguatedDisplayName(senderId)
        val otherUser = content.userDisplayName ?: content.userId.value
        val bestName = if (senderProfile.getDisplayName() == null && content.userId == senderId) otherUser else senderName
        val mainText = when (content.change) {
            null,
            MembershipChange.NONE -> stringResource(Res.string.membership_change_none, bestName)
            MembershipChange.ERROR -> stringResource(Res.string.membership_change_error, bestName)
            MembershipChange.JOINED -> stringResource(Res.string.membership_change_joined, bestName)
            MembershipChange.LEFT -> stringResource(Res.string.membership_change_left, bestName)
            MembershipChange.BANNED -> stringResource(Res.string.membership_change_banned, senderName, otherUser)
            MembershipChange.UNBANNED -> stringResource(Res.string.membership_change_unbanned, senderName, otherUser)
            MembershipChange.KICKED -> stringResource(Res.string.membership_change_kicked, senderName, otherUser)
            MembershipChange.INVITED -> stringResource(Res.string.membership_change_invited, senderName, otherUser)
            MembershipChange.KICKED_AND_BANNED -> stringResource(Res.string.membership_change_kicked_and_banned, senderName, otherUser)
            MembershipChange.INVITATION_ACCEPTED -> stringResource(Res.string.membership_change_invitation_accepted, bestName)
            MembershipChange.INVITATION_REJECTED -> stringResource(Res.string.membership_change_invitation_rejected, bestName)
            MembershipChange.INVITATION_REVOKED -> stringResource(Res.string.membership_change_invitation_revoked, senderName, otherUser)
            MembershipChange.KNOCKED -> stringResource(Res.string.membership_change_knocked, bestName)
            MembershipChange.KNOCK_ACCEPTED -> stringResource(Res.string.membership_change_knock_accepted, senderName, otherUser)
            MembershipChange.KNOCK_RETRACTED -> stringResource(Res.string.membership_change_knock_retracted, bestName)
            MembershipChange.KNOCK_DENIED -> stringResource(Res.string.membership_change_knock_denied, senderName, otherUser)
            MembershipChange.NOT_IMPLEMENTED -> stringResource(Res.string.membership_change_not_implemented, bestName)
        }
        return buildString {
            append(mainText)
            if (!content.reason.isNullOrBlank()) {
                append(". ")
                append(stringResource(Res.string.membership_reason, content.reason ?: ""))
            }
        }
    }
}
