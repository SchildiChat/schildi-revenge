package chat.schildi.revenge.compose.destination.conversation.event

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import chat.schildi.revenge.compose.destination.conversation.event.message.TimestampOverlayContent
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.timeline.item.event.MembershipChange
import io.element.android.libraries.matrix.api.timeline.item.event.ProfileDetails
import io.element.android.libraries.matrix.api.timeline.item.event.RoomMembershipContent
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

@Composable
fun RoomMembershipRow(
    content: RoomMembershipContent,
    senderId: UserId,
    senderProfile: ProfileDetails,
    timestamp: TimestampOverlayContent?,
    modifier: Modifier = Modifier,
) {
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
    val text = buildAnnotatedString {
        append(mainText)
        if (!content.reason.isNullOrBlank()) {
            append(". ")
            append(stringResource(Res.string.membership_reason, content.reason ?: ""))
        }
    }
    StateUpdateRow(
        text = text,
        senderProfile = senderProfile,
        timestamp = timestamp,
        modifier = modifier,
    )
}
