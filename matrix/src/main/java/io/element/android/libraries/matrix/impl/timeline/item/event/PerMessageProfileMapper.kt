package io.element.android.libraries.matrix.impl.timeline.item.event

import io.element.android.libraries.matrix.api.timeline.item.event.PerMessageProfile
import io.element.android.libraries.matrix.impl.media.map
import org.matrix.rustcomponents.sdk.PerMessageProfile as RustPerMessageProfile

fun RustPerMessageProfile.map(): PerMessageProfile = PerMessageProfile(
    id = id,
    displayName = displayName,
    avatarUrl = avatarUrl,
    avatarFile = avatarFile?.map(),
    hasFallback = hasFallback,
)
