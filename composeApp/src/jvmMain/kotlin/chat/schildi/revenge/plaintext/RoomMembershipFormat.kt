package chat.schildi.revenge.plaintext

import chat.schildi.revenge.compose.util.ComposableStringHolder
import chat.schildi.revenge.compose.util.toStringHolder
import io.element.android.libraries.matrix.api.room.RoomMembershipState
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.room_membership_ban
import shire.composeapp.generated.resources.room_membership_invite
import shire.composeapp.generated.resources.room_membership_join
import shire.composeapp.generated.resources.room_membership_knock
import shire.composeapp.generated.resources.room_membership_leave

object RoomMembershipFormat {
    fun formatRoomMembership(membership: RoomMembershipState): ComposableStringHolder {
        return when (membership) {
            RoomMembershipState.BAN -> Res.string.room_membership_ban
            RoomMembershipState.INVITE -> Res.string.room_membership_invite
            RoomMembershipState.JOIN -> Res.string.room_membership_join
            RoomMembershipState.KNOCK -> Res.string.room_membership_knock
            RoomMembershipState.LEAVE -> Res.string.room_membership_leave
        }.toStringHolder()
    }
}
