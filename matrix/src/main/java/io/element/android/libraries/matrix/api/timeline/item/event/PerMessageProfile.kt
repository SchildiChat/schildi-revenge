package io.element.android.libraries.matrix.api.timeline.item.event

import androidx.compose.runtime.Immutable
import io.element.android.libraries.matrix.api.media.MediaSource

@Immutable
data class PerMessageProfile(
    val id: String,
    val displayName: String?,
    val avatarUrl: String?,
    val avatarFile: MediaSource?,
    val hasFallback: Boolean,
)
