package chat.schildi.revenge.model.conversation

import chat.schildi.resources.toStringHolder
import chat.schildi.revenge.compose.components.EditableDropdownEntry
import io.element.android.libraries.matrix.api.createroom.RoomPreset
import io.element.android.libraries.matrix.api.room.history.RoomHistoryVisibility
import io.element.android.libraries.matrix.api.room.join.JoinRule
import io.element.android.libraries.matrix.api.roomdirectory.RoomVisibility
import kotlinx.collections.immutable.persistentListOf
import shire.res.generated.resources.Res
import shire.res.generated.resources.hint_hidden
import shire.res.generated.resources.history_visibility_invited
import shire.res.generated.resources.history_visibility_joined
import shire.res.generated.resources.history_visibility_shared
import shire.res.generated.resources.history_visibility_world_readable
import shire.res.generated.resources.join_rule_invite
import shire.res.generated.resources.join_rule_knock
import shire.res.generated.resources.join_rule_public
import shire.res.generated.resources.hint_private_room
import shire.res.generated.resources.hint_public_room
import shire.res.generated.resources.hint_published
import shire.res.generated.resources.room_preset_private_chat
import shire.res.generated.resources.room_preset_public_chat
import shire.res.generated.resources.room_preset_trusted_private_chat

val JOIN_RULE_ENTRIES = persistentListOf(
    EditableDropdownEntry(
        JoinRule.Invite,
        Res.string.join_rule_invite.toStringHolder()
    ),
    EditableDropdownEntry(
        JoinRule.Knock,
        Res.string.join_rule_knock.toStringHolder()
    ),
    EditableDropdownEntry(
        JoinRule.Public,
        Res.string.join_rule_public.toStringHolder()
    ),
)

val HISTORY_VISIBILITY_ENTRIES = persistentListOf(
    EditableDropdownEntry(
        RoomHistoryVisibility.Joined,
        Res.string.history_visibility_joined.toStringHolder()
    ),
    EditableDropdownEntry(
        RoomHistoryVisibility.Invited,
        Res.string.history_visibility_invited.toStringHolder()
    ),
    EditableDropdownEntry(
        RoomHistoryVisibility.Shared,
        Res.string.history_visibility_shared.toStringHolder()
    ),
    EditableDropdownEntry(
        RoomHistoryVisibility.WorldReadable,
        Res.string.history_visibility_world_readable.toStringHolder()
    ),
)

val ROOM_VISIBILITY_ENTRIES = persistentListOf(
    EditableDropdownEntry(
        RoomVisibility.Private,
        Res.string.hint_hidden.toStringHolder(),
    ),
    EditableDropdownEntry(
        RoomVisibility.Public,
        Res.string.hint_published.toStringHolder(),
    ),
)

val ROOM_PRESETS = persistentListOf(
    EditableDropdownEntry(
        RoomPreset.PRIVATE_CHAT,
        Res.string.room_preset_private_chat.toStringHolder(),
    ),
    EditableDropdownEntry(
        RoomPreset.PUBLIC_CHAT,
        Res.string.room_preset_public_chat.toStringHolder(),
    ),
    EditableDropdownEntry(
        RoomPreset.TRUSTED_PRIVATE_CHAT,
        Res.string.room_preset_trusted_private_chat.toStringHolder(),
    ),
)
val COMMON_ROOM_PRESETS = ROOM_PRESETS.filter { it.value != RoomPreset.TRUSTED_PRIVATE_CHAT }
