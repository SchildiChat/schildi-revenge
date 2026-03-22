package chat.schildi.revenge.plaintext

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import chat.schildi.revenge.model.conversation.MessageMetadata
import co.touchlab.kermit.Logger
import com.beeper.android.messageformat.MatrixBodyAnnotations
import com.beeper.android.messageformat.SpanAttributes
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
import io.element.android.libraries.matrix.api.timeline.item.event.OtherState
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
import io.element.android.libraries.matrix.api.room.join.JoinRule
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.join_rule_invite
import shire.composeapp.generated.resources.join_rule_knock
import shire.composeapp.generated.resources.join_rule_knock_restricted
import shire.composeapp.generated.resources.join_rule_public
import shire.composeapp.generated.resources.join_rule_restricted
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
import shire.composeapp.generated.resources.message_placeholder_state_event
import shire.composeapp.generated.resources.message_placeholder_state_event_policy_rule_room
import shire.composeapp.generated.resources.message_placeholder_state_event_policy_rule_server
import shire.composeapp.generated.resources.message_placeholder_state_event_policy_rule_user
import shire.composeapp.generated.resources.message_placeholder_state_event_room_aliases
import shire.composeapp.generated.resources.message_placeholder_state_event_room_avatar_cleared
import shire.composeapp.generated.resources.message_placeholder_state_event_room_avatar_set
import shire.composeapp.generated.resources.message_placeholder_state_event_room_canonical_alias
import shire.composeapp.generated.resources.message_placeholder_state_event_room_create
import shire.composeapp.generated.resources.message_placeholder_state_event_room_encryption
import shire.composeapp.generated.resources.message_placeholder_state_event_room_guest_access
import shire.composeapp.generated.resources.message_placeholder_state_event_room_history_visibility
import shire.composeapp.generated.resources.message_placeholder_state_event_room_join_rules
import shire.composeapp.generated.resources.message_placeholder_state_event_room_join_rules_to
import shire.composeapp.generated.resources.message_placeholder_state_event_room_name_cleared
import shire.composeapp.generated.resources.message_placeholder_state_event_room_name_set
import shire.composeapp.generated.resources.message_placeholder_state_event_room_pinned_events_added
import shire.composeapp.generated.resources.message_placeholder_state_event_room_pinned_events_changed
import shire.composeapp.generated.resources.message_placeholder_state_event_room_pinned_events_removed
import shire.composeapp.generated.resources.message_placeholder_state_event_room_server_acl
import shire.composeapp.generated.resources.message_placeholder_state_event_room_third_party_invite
import shire.composeapp.generated.resources.message_placeholder_state_event_room_tombstone
import shire.composeapp.generated.resources.message_placeholder_state_event_room_topic_cleared
import shire.composeapp.generated.resources.message_placeholder_state_event_room_topic_set
import shire.composeapp.generated.resources.message_placeholder_state_event_room_user_power_levels
import shire.composeapp.generated.resources.message_placeholder_state_event_space_child
import shire.composeapp.generated.resources.message_placeholder_state_event_space_parent
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
    fun eventToText(
        content: EventContent,
        messageMetadata: MessageMetadata?,
        senderProfile: ProfileDetails,
        senderId: UserId,
    ): String = eventToText(
        content = content,
        messageMetadata = messageMetadata,
        senderProfile = senderProfile,
        senderId = senderId,
        getString = { res -> stringResource(res) },
        getFormatString = { res, args -> stringResource(res, *args) },
    )

    private inline fun eventToText(
        content: EventContent,
        messageMetadata: MessageMetadata?,
        senderProfile: ProfileDetails,
        senderId: UserId,
        getString: (StringResource) -> String,
        getFormatString: (StringResource, formatArgs: Array<Any>) -> String,
    ): String {
        messageMetadata?.preFormattedContent?.text?.let {
            // Strip spoilers, formatting, and unnecessary whitespace
            return it
                .stripMatrixSpoilers()
                .toString()
                .trim()
                .replace("\\s+".toRegex(), " ")
        }
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
            LegacyCallInviteContent -> getString(Res.string.message_placeholder_call)
            is FailedToParseMessageLikeContent,
            is FailedToParseStateContent -> getString(Res.string.message_placeholder_message_failed_to_parse)
            is PollContent -> content.question
            is ProfileChangeContent -> profileChangeToText(content, senderProfile, senderId, getFormatString)
            RedactedContent -> getString(Res.string.message_placeholder_message_redacted)
            is RoomMembershipContent -> roomMembershipToText(content, senderProfile, senderId, getFormatString)
            is StateContent -> stateEventToText(content, senderProfile, senderId, getString, getFormatString)
            is UnableToDecryptContent -> getString(Res.string.message_placeholder_unable_to_decrypt)
            UnknownContent -> getString(Res.string.message_placeholder_unknown)
        }
    }

    @Composable
    fun stateEventToText(
        stateContent: StateContent,
        senderProfile: ProfileDetails,
        senderId: UserId,
    ) = stateEventToText(
        stateContent = stateContent,
        senderProfile = senderProfile,
        senderId = senderId,
        getString = { res -> stringResource(res) },
        getFormatString = { res, args -> stringResource(res, *args) },
    )

    private inline fun stateEventToText(
        stateContent: StateContent,
        senderProfile: ProfileDetails,
        senderId: UserId,
        getString: (StringResource) -> String,
        getFormatString: (StringResource, formatArgs: Array<Any>) -> String,
    ): String {
        val senderName = senderProfile.getDisambiguatedDisplayName(senderId)
        return when (val content = stateContent.content) {
            is OtherState.Custom -> getFormatString(Res.string.message_placeholder_state_event, arrayOf(senderName, content.eventType))
            OtherState.PolicyRuleRoom -> getFormatString(Res.string.message_placeholder_state_event_policy_rule_room, arrayOf(senderName))
            OtherState.PolicyRuleServer -> getFormatString(Res.string.message_placeholder_state_event_policy_rule_server, arrayOf(senderName))
            OtherState.PolicyRuleUser -> getFormatString(Res.string.message_placeholder_state_event_policy_rule_user, arrayOf(senderName))
            OtherState.RoomAliases -> getFormatString(Res.string.message_placeholder_state_event_room_aliases, arrayOf(senderName))
            is OtherState.RoomAvatar -> when (content.url) {
                null -> getFormatString(Res.string.message_placeholder_state_event_room_avatar_cleared, arrayOf(senderName))
                else -> getFormatString(Res.string.message_placeholder_state_event_room_avatar_set, arrayOf(senderName))
            }
            OtherState.RoomCanonicalAlias -> getFormatString(Res.string.message_placeholder_state_event_room_canonical_alias, arrayOf(senderName))
            OtherState.RoomCreate -> getFormatString(Res.string.message_placeholder_state_event_room_create, arrayOf(senderName))
            OtherState.RoomEncryption -> getFormatString(Res.string.message_placeholder_state_event_room_encryption, arrayOf(senderName))
            OtherState.RoomGuestAccess -> getFormatString(Res.string.message_placeholder_state_event_room_guest_access, arrayOf(senderName))
            OtherState.RoomHistoryVisibility -> getFormatString(Res.string.message_placeholder_state_event_room_history_visibility, arrayOf(senderName))
            is OtherState.RoomJoinRules -> when (content.joinRule) {
                null -> getFormatString(Res.string.message_placeholder_state_event_room_join_rules, arrayOf(senderName))
                else -> getFormatString(
                    Res.string.message_placeholder_state_event_room_join_rules_to,
                    arrayOf(
                        senderName,
                        content.joinRule?.let { joinRuleToText(it, getString) } ?: "",
                    )
                )
            }
            is OtherState.RoomName -> when (content.name) {
                null -> getFormatString(Res.string.message_placeholder_state_event_room_name_cleared, arrayOf(senderName))
                else -> getFormatString(Res.string.message_placeholder_state_event_room_name_set, arrayOf(senderName, content.name ?: ""))
            }
            is OtherState.RoomPinnedEvents -> when (content.change) {
                OtherState.RoomPinnedEvents.Change.ADDED -> getFormatString(Res.string.message_placeholder_state_event_room_pinned_events_added, arrayOf(senderName))
                OtherState.RoomPinnedEvents.Change.REMOVED -> getFormatString(Res.string.message_placeholder_state_event_room_pinned_events_removed, arrayOf(senderName))
                OtherState.RoomPinnedEvents.Change.CHANGED -> getFormatString(Res.string.message_placeholder_state_event_room_pinned_events_changed, arrayOf(senderName))
            }
            OtherState.RoomServerAcl -> getFormatString(Res.string.message_placeholder_state_event_room_server_acl, arrayOf(senderName))
            is OtherState.RoomThirdPartyInvite -> getFormatString(
                Res.string.message_placeholder_state_event_room_third_party_invite,
                arrayOf(
                    senderName,
                    content.displayName ?: ""
                )
            )
            OtherState.RoomTombstone -> getFormatString(Res.string.message_placeholder_state_event_room_tombstone, arrayOf(senderName))
            is OtherState.RoomTopic -> when (content.topic) {
                null -> getFormatString(Res.string.message_placeholder_state_event_room_topic_cleared, arrayOf(senderName))
                else -> getFormatString(Res.string.message_placeholder_state_event_room_topic_set, arrayOf(senderName))
            }
            is OtherState.RoomUserPowerLevels -> getFormatString(Res.string.message_placeholder_state_event_room_user_power_levels, arrayOf(senderName))
            OtherState.SpaceChild -> getFormatString(Res.string.message_placeholder_state_event_space_child, arrayOf(senderName))
            OtherState.SpaceParent -> getFormatString(Res.string.message_placeholder_state_event_space_parent, arrayOf(senderName))
        }
    }

    private inline fun joinRuleToText(
        rule: JoinRule,
        getString: (StringResource) -> String,
    ): String {
        return when (rule) {
            JoinRule.Public -> getString(Res.string.join_rule_public)
            JoinRule.Knock -> getString(Res.string.join_rule_knock)
            JoinRule.Invite -> getString(Res.string.join_rule_invite)
            is JoinRule.Restricted -> getString(Res.string.join_rule_restricted)
            is JoinRule.KnockRestricted -> getString(Res.string.join_rule_knock_restricted)
            is JoinRule.Custom -> rule.value
        }
    }


    @Composable
    fun profileChangeToText(
        content: ProfileChangeContent,
        senderProfile: ProfileDetails,
        senderId: UserId,
    ) = profileChangeToText(
        content = content,
        senderProfile = senderProfile,
        senderId = senderId,
        getFormatString = { res, args -> stringResource(res, *args) },
    )

    private inline fun profileChangeToText(
        content: ProfileChangeContent,
        senderProfile: ProfileDetails,
        senderId: UserId,
        getFormatString: (StringResource, formatArgs: Array<Any>) -> String,
    ): String {
        val senderName = senderProfile.getDisambiguatedDisplayName(senderId)
        return if (content.prevDisplayName == content.displayName) {
            if (content.prevAvatarUrl == content.avatarUrl) {
                getFormatString(Res.string.profile_update_none, arrayOf(senderName))
            } else {
                getFormatString(Res.string.profile_update_avatar, arrayOf(senderName))
            }
        } else if (content.prevAvatarUrl != content.avatarUrl) {
            if (content.prevDisplayName == null) {
                getFormatString(Res.string.profile_update_set_name_and_avatar, arrayOf(senderName))
            } else {
                getFormatString(Res.string.profile_update_name_and_avatar, arrayOf(senderName, content.prevDisplayName ?: ""))
            }
        } else {
            when {
                content.prevDisplayName == null -> getFormatString(Res.string.profile_update_set_name, arrayOf(senderName))
                content.displayName == null -> getFormatString(
                    Res.string.profile_update_cleared_name,
                    arrayOf(
                        senderName,
                        content.prevDisplayName ?: ""
                    )
                )

                else -> getFormatString(Res.string.profile_update_name, arrayOf(senderName, content.prevDisplayName ?: ""))
            }
        }
    }

    @Composable
    fun roomMembershipToText(
        content: RoomMembershipContent,
        senderProfile: ProfileDetails,
        senderId: UserId,
    ) = roomMembershipToText(
        content = content,
        senderProfile = senderProfile,
        senderId = senderId,
        getFormatString = { res, args -> stringResource(res, args) },
    )

    private inline fun roomMembershipToText(
        content: RoomMembershipContent,
        senderProfile: ProfileDetails,
        senderId: UserId,
        getFormatString: (StringResource, formatArgs: Array<Any>) -> String,
    ): String {
        val senderName = senderProfile.getDisambiguatedDisplayName(senderId)
        val otherUser = content.userDisplayName ?: content.userId.value
        val bestName = if (senderProfile.getDisplayName() == null && content.userId == senderId) otherUser else senderName
        val mainText = when (content.change) {
            null,
            MembershipChange.NONE -> getFormatString(Res.string.membership_change_none, arrayOf(bestName))
            MembershipChange.ERROR -> getFormatString(Res.string.membership_change_error, arrayOf(bestName))
            MembershipChange.JOINED -> getFormatString(Res.string.membership_change_joined, arrayOf(bestName))
            MembershipChange.LEFT -> getFormatString(Res.string.membership_change_left, arrayOf(bestName))
            MembershipChange.BANNED -> getFormatString(Res.string.membership_change_banned, arrayOf(senderName, otherUser))
            MembershipChange.UNBANNED -> getFormatString(Res.string.membership_change_unbanned, arrayOf(senderName, otherUser))
            MembershipChange.KICKED -> getFormatString(Res.string.membership_change_kicked, arrayOf(senderName, otherUser))
            MembershipChange.INVITED -> getFormatString(Res.string.membership_change_invited, arrayOf(senderName, otherUser))
            MembershipChange.KICKED_AND_BANNED -> getFormatString(Res.string.membership_change_kicked_and_banned, arrayOf(senderName, otherUser))
            MembershipChange.INVITATION_ACCEPTED -> getFormatString(Res.string.membership_change_invitation_accepted, arrayOf(bestName))
            MembershipChange.INVITATION_REJECTED -> getFormatString(Res.string.membership_change_invitation_rejected, arrayOf(bestName))
            MembershipChange.INVITATION_REVOKED -> getFormatString(Res.string.membership_change_invitation_revoked, arrayOf(senderName, otherUser))
            MembershipChange.KNOCKED -> getFormatString(Res.string.membership_change_knocked, arrayOf(bestName))
            MembershipChange.KNOCK_ACCEPTED -> getFormatString(Res.string.membership_change_knock_accepted, arrayOf(senderName, otherUser))
            MembershipChange.KNOCK_RETRACTED -> getFormatString(Res.string.membership_change_knock_retracted, arrayOf(bestName))
            MembershipChange.KNOCK_DENIED -> getFormatString(Res.string.membership_change_knock_denied, arrayOf(senderName, otherUser))
            MembershipChange.NOT_IMPLEMENTED -> getFormatString(Res.string.membership_change_not_implemented, arrayOf(bestName))
        }
        return buildString {
            append(mainText)
            if (!content.reason.isNullOrBlank()) {
                append(". ")
                append(getFormatString(Res.string.membership_reason, arrayOf(content.reason ?: "")))
            }
        }
    }
}

fun AnnotatedString.stripMatrixSpoilers(): AnnotatedString {
    val ranges = getStringAnnotations(MatrixBodyAnnotations.SPAN, 0, text.length)
        .filter {
            try {
                Json.decodeFromString<SpanAttributes>(it.item).isSpoiler
            } catch (e: Exception) {
                Logger.withTag("stripMatrixSpoilers").w("Failed to parse span attributes", e)
                false
            }
        }
        .sortedBy { it.start }

    if (ranges.isEmpty()) return this

    return buildAnnotatedString {
        var cursor = 0

        for (r in ranges) {
            // Skip overlapping/contained ranges
            if (r.start < cursor) continue

            if (cursor < r.start) {
                append(subSequence(cursor, r.start))
            }

            val replacement = "█".repeat((r.end - r.start).coerceIn(0, 12))
            append(replacement)

            cursor = r.end
        }

        if (cursor < text.length) {
            append(subSequence(cursor, text.length))
        }
    }
}
