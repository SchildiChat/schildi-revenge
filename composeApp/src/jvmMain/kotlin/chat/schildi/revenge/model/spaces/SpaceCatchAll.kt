package chat.schildi.revenge.model.spaces

import chat.schildi.revenge.config.keybindings.SpaceCatchAllInviteMode
import chat.schildi.revenge.config.keybindings.SpaceCatchAllMode
import io.element.android.libraries.matrix.api.room.SpaceCatchAllInfo

fun SpaceCatchAllInfo?.toSpaceCatchAllFilterMode() = when {
    this == null || !includeOrphans -> null
    else -> when (filterIsDirect) {
        null -> SpaceCatchAllMode.All
        true -> SpaceCatchAllMode.Dms
        false -> SpaceCatchAllMode.Groups
    }
}

fun SpaceCatchAllInfo?.toSpaceCatchAllInviteFilterMode() = when {
    this == null || !includeOrphans -> null
    else -> when (filterIsInvite) {
        null -> SpaceCatchAllInviteMode.All
        true -> SpaceCatchAllInviteMode.Invites
        false -> SpaceCatchAllInviteMode.NonInvites
    }
}
