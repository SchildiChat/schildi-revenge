package chat.schildi.revenge.plaintext

import chat.schildi.notifications.SyncNotification
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.notification.NotificationContent
import io.element.android.libraries.matrix.api.room.RoomMembershipState
import io.element.android.libraries.matrix.api.timeline.item.event.AudioMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.EmoteMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.FileMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.ImageMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.LocationMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.MessageType
import io.element.android.libraries.matrix.api.timeline.item.event.NoticeMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.OtherMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.StickerMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.TextMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.VideoMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.VoiceMessageType
import org.jetbrains.compose.resources.StringResource
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
    suspend fun notificationToText(notification: SyncNotification): String =
        notificationToText(
            notification = notification,
            getString = { res -> getString(res) },
            getFormatString = { res, args -> getString(res, *args) },
        )

    private inline fun notificationToText(
        notification: SyncNotification,
        getString: (StringResource) -> String,
        getFormatString: (StringResource, formatArgs: Array<Any>) -> String,
    ): String {
        return when (val content = notification.content) {
            is NotificationContent.Invite -> getFormatString(Res.string.message_placeholder_invite_by, arrayOf(notification.senderName()))
            is NotificationContent.MessageLike -> {
                val textContent = messageLikeToText(content, getString, getFormatString)
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
                getFormatString,
            )
        }
    }

    private fun SyncNotification.senderName() = senderInfo.displayName ?: senderId.value

    private inline fun messageLikeToText(
        content: NotificationContent.MessageLike,
        getString: (StringResource) -> String,
        getFormatString: (StringResource, formatArgs: Array<Any>) -> String,
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
            is NotificationContent.MessageLike.RoomMessage -> messageTypeToText(content.messageType)
            is NotificationContent.MessageLike.RoomRedaction -> {
                if (content.reason.isNullOrBlank()) {
                    getString(Res.string.message_placeholder_message_redacted)
                } else {
                    getFormatString(
                        Res.string.message_placeholder_message_redacted_with_reason,
                        arrayOf(content.reason ?: ""),
                    )
                }
            }
            NotificationContent.MessageLike.Sticker -> getString(Res.string.message_placeholder_sticker)
            is NotificationContent.MessageLike.Poll -> content.question
        }
    }

    private inline fun stateEventToText(
        stateContent: NotificationContent.StateEvent,
        senderId: UserId,
        senderName: String,
        getFormatString: (StringResource, formatArgs: Array<Any>) -> String,
    ): String {
        return when (stateContent) {
            NotificationContent.StateEvent.PolicyRuleRoom ->
                getFormatString(
                    Res.string.message_placeholder_state_event_policy_rule_room,
                    arrayOf(senderName),
                )
            NotificationContent.StateEvent.PolicyRuleServer ->
                getFormatString(
                    Res.string.message_placeholder_state_event_policy_rule_server,
                    arrayOf(senderName),
                )
            NotificationContent.StateEvent.PolicyRuleUser ->
                getFormatString(
                    Res.string.message_placeholder_state_event_policy_rule_user,
                    arrayOf(senderName),
                )
            NotificationContent.StateEvent.RoomAliases ->
                getFormatString(
                    Res.string.message_placeholder_state_event_room_aliases,
                    arrayOf(senderName),
                )
            NotificationContent.StateEvent.RoomAvatar ->
                getFormatString(
                    Res.string.message_placeholder_state_event_room_avatar_changed,
                    arrayOf(senderName),
                )
            NotificationContent.StateEvent.RoomCanonicalAlias ->
                getFormatString(
                    Res.string.message_placeholder_state_event_room_canonical_alias,
                    arrayOf(senderName),
                )
            NotificationContent.StateEvent.RoomCreate ->
                getFormatString(
                    Res.string.message_placeholder_state_event_room_create,
                    arrayOf(senderName),
                )
            NotificationContent.StateEvent.RoomEncryption ->
                getFormatString(
                    Res.string.message_placeholder_state_event_room_encryption,
                    arrayOf(senderName),
                )
            NotificationContent.StateEvent.RoomGuestAccess ->
                getFormatString(
                    Res.string.message_placeholder_state_event_room_guest_access,
                    arrayOf(senderName),
                )
            NotificationContent.StateEvent.RoomHistoryVisibility ->
                getFormatString(
                    Res.string.message_placeholder_state_event_room_history_visibility,
                    arrayOf(senderName),
                )
            NotificationContent.StateEvent.RoomJoinRules ->
                getFormatString(
                    Res.string.message_placeholder_state_event_room_join_rules,
                    arrayOf(senderName),
                )
            is NotificationContent.StateEvent.RoomMemberContent ->
                roomMembershipToText(
                    content = stateContent,
                    senderId = senderId,
                    senderName = senderName,
                    getFormatString = getFormatString,
                )
            NotificationContent.StateEvent.RoomName ->
                getFormatString(
                    Res.string.message_placeholder_state_event_room_name_changed,
                    arrayOf(senderName),
                )
            NotificationContent.StateEvent.RoomPinnedEvents ->
                getFormatString(
                    Res.string.message_placeholder_state_event_room_pinned_events_changed,
                    arrayOf(senderName),
                )
            NotificationContent.StateEvent.RoomPowerLevels ->
                getFormatString(
                    Res.string.message_placeholder_state_event_room_user_power_levels,
                    arrayOf(senderName),
                )
            NotificationContent.StateEvent.RoomServerAcl ->
                getFormatString(
                    Res.string.message_placeholder_state_event_room_server_acl,
                    arrayOf(senderName),
                )
            NotificationContent.StateEvent.RoomThirdPartyInvite ->
                getFormatString(
                    Res.string.message_placeholder_state_event_room_third_party_invite_generic,
                    arrayOf(senderName),
                )
            NotificationContent.StateEvent.RoomTombstone ->
                getFormatString(
                    Res.string.message_placeholder_state_event_room_tombstone,
                    arrayOf(senderName),
                )
            is NotificationContent.StateEvent.RoomTopic -> {
                if (stateContent.topic.isBlank()) {
                    getFormatString(Res.string.message_placeholder_state_event_room_topic_cleared, arrayOf(senderName))
                } else {
                    getFormatString(Res.string.message_placeholder_state_event_room_topic_set, arrayOf(senderName))
                }
            }
            NotificationContent.StateEvent.SpaceChild ->
                getFormatString(
                    Res.string.message_placeholder_state_event_space_child,
                    arrayOf(senderName),
                )
            NotificationContent.StateEvent.SpaceParent ->
                getFormatString(
                    Res.string.message_placeholder_state_event_space_parent,
                    arrayOf(senderName),
                )
        }
    }

    private inline fun roomMembershipToText(
        content: NotificationContent.StateEvent.RoomMemberContent,
        senderId: UserId,
        senderName: String,
        getFormatString: (StringResource, formatArgs: Array<Any>) -> String,
    ): String {
        val otherUser = content.userId.value
        return when (content.membershipState) {
            RoomMembershipState.BAN ->
                getFormatString(
                    Res.string.membership_change_banned,
                    arrayOf(senderName, otherUser),
                )
            RoomMembershipState.INVITE ->
                getFormatString(
                    Res.string.membership_change_invited,
                    arrayOf(senderName, otherUser),
                )
            RoomMembershipState.JOIN -> {
                if (content.userId == senderId) {
                    getFormatString(Res.string.membership_change_joined, arrayOf(senderName))
                } else {
                    getFormatString(
                        Res.string.message_placeholder_state_event_room_member_changed,
                        arrayOf(senderName, otherUser),
                    )
                }
            }
            RoomMembershipState.KNOCK -> {
                if (content.userId == senderId) {
                    getFormatString(Res.string.membership_change_knocked, arrayOf(senderName))
                } else {
                    getFormatString(
                        Res.string.message_placeholder_state_event_room_member_changed,
                        arrayOf(senderName, otherUser),
                    )
                }
            }
            RoomMembershipState.LEAVE -> {
                if (content.userId == senderId) {
                    getFormatString(Res.string.membership_change_left, arrayOf(senderName))
                } else {
                    getFormatString(
                        Res.string.message_placeholder_state_event_room_member_changed,
                        arrayOf(senderName, otherUser),
                    )
                }
            }
        }
    }

    private fun messageTypeToText(type: MessageType): String {
        return when (type) {
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
}
