package chat.schildi.revenge.plaintext

import chat.schildi.notifications.SyncNotification
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.notification.NotificationContent
import io.element.android.libraries.matrix.api.room.RoomMembershipState
import org.jetbrains.compose.resources.getString
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.membership_change_banned
import shire.composeapp.generated.resources.membership_change_invited
import shire.composeapp.generated.resources.membership_change_joined
import shire.composeapp.generated.resources.membership_change_knocked
import shire.composeapp.generated.resources.membership_change_left
import shire.composeapp.generated.resources.message_placeholder_call
import shire.composeapp.generated.resources.message_placeholder_encrypted_message
import shire.composeapp.generated.resources.message_placeholder_invite_by
import shire.composeapp.generated.resources.message_placeholder_key_verification
import shire.composeapp.generated.resources.message_placeholder_message_redacted
import shire.composeapp.generated.resources.message_placeholder_message_redacted_with_reason
import shire.composeapp.generated.resources.message_placeholder_reaction
import shire.composeapp.generated.resources.message_placeholder_state_event_policy_rule_room
import shire.composeapp.generated.resources.message_placeholder_state_event_policy_rule_server
import shire.composeapp.generated.resources.message_placeholder_state_event_policy_rule_user
import shire.composeapp.generated.resources.message_placeholder_state_event_room_aliases
import shire.composeapp.generated.resources.message_placeholder_state_event_room_avatar_changed
import shire.composeapp.generated.resources.message_placeholder_state_event_room_canonical_alias
import shire.composeapp.generated.resources.message_placeholder_state_event_room_create
import shire.composeapp.generated.resources.message_placeholder_state_event_room_encryption
import shire.composeapp.generated.resources.message_placeholder_state_event_room_guest_access
import shire.composeapp.generated.resources.message_placeholder_state_event_room_history_visibility
import shire.composeapp.generated.resources.message_placeholder_state_event_room_join_rules
import shire.composeapp.generated.resources.message_placeholder_state_event_room_member_changed
import shire.composeapp.generated.resources.message_placeholder_state_event_room_name_changed
import shire.composeapp.generated.resources.message_placeholder_state_event_room_pinned_events_changed
import shire.composeapp.generated.resources.message_placeholder_state_event_room_server_acl
import shire.composeapp.generated.resources.message_placeholder_state_event_room_third_party_invite_generic
import shire.composeapp.generated.resources.message_placeholder_state_event_room_tombstone
import shire.composeapp.generated.resources.message_placeholder_state_event_room_topic_cleared
import shire.composeapp.generated.resources.message_placeholder_state_event_room_topic_set
import shire.composeapp.generated.resources.message_placeholder_state_event_room_user_power_levels
import shire.composeapp.generated.resources.message_placeholder_state_event_space_child
import shire.composeapp.generated.resources.message_placeholder_state_event_space_parent
import shire.composeapp.generated.resources.message_placeholder_sticker

object NotificationEventTextFormat {

    suspend fun notificationToText(notification: SyncNotification, stripNewlines: Boolean = false): String {
        return when (val content = notification.content) {
            is NotificationContent.Invite -> getString(Res.string.message_placeholder_invite_by)
            is NotificationContent.MessageLike -> {
                val textContent = messageLikeToText(content, stripNewlines)
                val senderName = notification.senderName()
                if (notification.roomInfo.isDirect && senderName == notification.roomInfo.displayName) {
                    textContent
                } else {
                    "$senderName: $textContent"
                }
            }
            is NotificationContent.StateEvent -> stateEventToText(
                content,
                notification.senderId,
                notification.senderName(),
            )
        }
    }

    private fun SyncNotification.senderName() = senderInfo.displayName ?: senderId.value

    private suspend fun messageLikeToText(
        content: NotificationContent.MessageLike,
        stripNewlines: Boolean,
    ): String {
        return when (content) {
            NotificationContent.MessageLike.CallAnswer,
            is NotificationContent.MessageLike.CallInvite,
            NotificationContent.MessageLike.CallHangup,
            NotificationContent.MessageLike.CallCandidates,
            is NotificationContent.MessageLike.RtcNotification -> getString(Res.string.message_placeholder_call)
            NotificationContent.MessageLike.KeyVerificationReady,
            NotificationContent.MessageLike.KeyVerificationStart,
            NotificationContent.MessageLike.KeyVerificationCancel,
            NotificationContent.MessageLike.KeyVerificationAccept,
            NotificationContent.MessageLike.KeyVerificationKey,
            NotificationContent.MessageLike.KeyVerificationMac,
            NotificationContent.MessageLike.KeyVerificationDone -> getString(Res.string.message_placeholder_key_verification)
            is NotificationContent.MessageLike.ReactionContent -> getString(Res.string.message_placeholder_reaction)
            NotificationContent.MessageLike.RoomEncrypted -> getString(Res.string.message_placeholder_encrypted_message)
            is NotificationContent.MessageLike.RoomMessage -> EventTextFormat.messageTypeToText(content.messageType, stripNewlines)
            is NotificationContent.MessageLike.RoomRedaction -> {
                if (content.reason.isNullOrBlank()) {
                    getString(Res.string.message_placeholder_message_redacted)
                } else {
                    getString(
                        Res.string.message_placeholder_message_redacted_with_reason,
                        content.reason ?: "",
                    )
                }
            }
            NotificationContent.MessageLike.Sticker -> getString(Res.string.message_placeholder_sticker)
            is NotificationContent.MessageLike.Poll -> content.question
        }
    }

    private suspend fun stateEventToText(
        stateContent: NotificationContent.StateEvent,
        senderId: UserId,
        senderName: String,
    ): String {
        return when (stateContent) {
            NotificationContent.StateEvent.PolicyRuleRoom ->
                getString(
                    Res.string.message_placeholder_state_event_policy_rule_room,
                    senderName,
                )
            NotificationContent.StateEvent.PolicyRuleServer ->
                getString(
                    Res.string.message_placeholder_state_event_policy_rule_server,
                    senderName,
                )
            NotificationContent.StateEvent.PolicyRuleUser ->
                getString(
                    Res.string.message_placeholder_state_event_policy_rule_user,
                    senderName,
                )
            NotificationContent.StateEvent.RoomAvatar ->
                getString(
                    Res.string.message_placeholder_state_event_room_avatar_changed,
                    senderName,
                )
            NotificationContent.StateEvent.RoomCanonicalAlias ->
                getString(
                    Res.string.message_placeholder_state_event_room_canonical_alias,
                    senderName,
                )
            NotificationContent.StateEvent.RoomCreate ->
                getString(
                    Res.string.message_placeholder_state_event_room_create,
                    senderName,
                )
            NotificationContent.StateEvent.RoomEncryption ->
                getString(
                    Res.string.message_placeholder_state_event_room_encryption,
                    senderName,
                )
            NotificationContent.StateEvent.RoomGuestAccess ->
                getString(
                    Res.string.message_placeholder_state_event_room_guest_access,
                    senderName,
                )
            NotificationContent.StateEvent.RoomHistoryVisibility ->
                getString(
                    Res.string.message_placeholder_state_event_room_history_visibility,
                    senderName,
                )
            NotificationContent.StateEvent.RoomJoinRules ->
                getString(
                    Res.string.message_placeholder_state_event_room_join_rules,
                    senderName,
                )
            is NotificationContent.StateEvent.RoomMemberContent ->
                roomMembershipToText(
                    content = stateContent,
                    senderId = senderId,
                    senderName = senderName,
                )
            NotificationContent.StateEvent.RoomName ->
                getString(
                    Res.string.message_placeholder_state_event_room_name_changed,
                    senderName,
                )
            NotificationContent.StateEvent.RoomPinnedEvents ->
                getString(
                    Res.string.message_placeholder_state_event_room_pinned_events_changed,
                    senderName,
                )
            NotificationContent.StateEvent.RoomPowerLevels ->
                getString(
                    Res.string.message_placeholder_state_event_room_user_power_levels,
                    senderName,
                )
            NotificationContent.StateEvent.RoomServerAcl ->
                getString(
                    Res.string.message_placeholder_state_event_room_server_acl,
                    senderName,
                )
            NotificationContent.StateEvent.RoomThirdPartyInvite ->
                getString(
                    Res.string.message_placeholder_state_event_room_third_party_invite_generic,
                    senderName,
                )
            NotificationContent.StateEvent.RoomTombstone ->
                getString(
                    Res.string.message_placeholder_state_event_room_tombstone,
                    senderName,
                )
            is NotificationContent.StateEvent.RoomTopic -> {
                if (stateContent.topic.isBlank()) {
                    getString(Res.string.message_placeholder_state_event_room_topic_cleared, senderName)
                } else {
                    getString(Res.string.message_placeholder_state_event_room_topic_set, senderName)
                }
            }
            NotificationContent.StateEvent.SpaceChild ->
                getString(
                    Res.string.message_placeholder_state_event_space_child,
                    senderName,
                )
            NotificationContent.StateEvent.SpaceParent ->
                getString(
                    Res.string.message_placeholder_state_event_space_parent,
                    senderName,
                )
        }
    }

    private suspend fun roomMembershipToText(
        content: NotificationContent.StateEvent.RoomMemberContent,
        senderId: UserId,
        senderName: String,
    ): String {
        val otherUser = content.userId.value
        return when (content.membershipState) {
            RoomMembershipState.BAN ->
                getString(
                    Res.string.membership_change_banned,
                    senderName, otherUser,
                )
            RoomMembershipState.INVITE ->
                getString(
                    Res.string.membership_change_invited,
                    senderName, otherUser,
                )
            RoomMembershipState.JOIN -> {
                if (content.userId == senderId) {
                    getString(Res.string.membership_change_joined, senderName)
                } else {
                    getString(
                        Res.string.message_placeholder_state_event_room_member_changed,
                        senderName, otherUser,
                    )
                }
            }
            RoomMembershipState.KNOCK -> {
                if (content.userId == senderId) {
                    getString(Res.string.membership_change_knocked, senderName)
                } else {
                    getString(
                        Res.string.message_placeholder_state_event_room_member_changed,
                        senderName, otherUser,
                    )
                }
            }
            RoomMembershipState.LEAVE -> {
                if (content.userId == senderId) {
                    getString(Res.string.membership_change_left, senderName)
                } else {
                    getString(
                        Res.string.message_placeholder_state_event_room_member_changed,
                        senderName, otherUser,
                    )
                }
            }
        }
    }

}
