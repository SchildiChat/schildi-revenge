package chat.schildi.revenge.model.spaces

import chat.schildi.revenge.config.keybindings.SpaceCatchAllMode
import io.element.android.libraries.matrix.api.room.SpaceCatchAllInfo

fun SpaceCatchAllInfo?.toSpaceCatchAllMode() = when {
    this == null || !includeOrphans -> SpaceCatchAllMode.None
    else -> when (filterIsDirect) {
        null -> SpaceCatchAllMode.All
        true -> SpaceCatchAllMode.Dms
        false -> SpaceCatchAllMode.Groups
    }
}
