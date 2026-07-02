package chat.schildi.revenge.plaintext

import chat.schildi.resources.ComposableStringHolder
import chat.schildi.resources.toStringHolder
import io.element.android.libraries.matrix.api.room.RoomMember
import shire.res.generated.resources.Res
import shire.res.generated.resources.power_level_user_role_admin
import shire.res.generated.resources.power_level_user_role_combined
import shire.res.generated.resources.power_level_user_role_creator
import shire.res.generated.resources.power_level_user_role_default
import shire.res.generated.resources.power_level_user_role_moderator
import shire.res.generated.resources.power_level_user_role_owner

object UserRoleFormat {
    val CANONICAL_POWER_LEVELS = listOf(0L, 50L, 100L)

    fun formatUserRole(role: RoomMember.Role): ComposableStringHolder {
        return when (role) {
            is RoomMember.Role.Owner -> if (role.isCreator) {
                Res.string.power_level_user_role_creator
            } else {
                Res.string.power_level_user_role_owner
            }
            RoomMember.Role.Admin -> Res.string.power_level_user_role_admin
            RoomMember.Role.Moderator -> Res.string.power_level_user_role_moderator
            RoomMember.Role.User -> Res.string.power_level_user_role_default
        }.toStringHolder()
    }

    fun formatUserRoleWithPowerLevel(role: RoomMember.Role, powerLevel: Long?): ComposableStringHolder {
        return if (powerLevel == null || powerLevel in CANONICAL_POWER_LEVELS) {
            formatUserRole(role)
        } else {
            Res.string.power_level_user_role_combined.toStringHolder(
                formatUserRole(role),
                powerLevel.toString().toStringHolder(),
            )
        }
    }
}
