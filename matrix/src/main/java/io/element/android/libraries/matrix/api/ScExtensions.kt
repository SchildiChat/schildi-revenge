package io.element.android.libraries.matrix.api

import io.element.android.libraries.matrix.api.core.RoomId

data class AccountDataRawEvent(
    val eventType: String,
    val content: String,
)

data class MutualRoomsPagedInfo(
    val count: Long,
    val joined: List<RoomId>,
    val nextBatch: String?,
)
