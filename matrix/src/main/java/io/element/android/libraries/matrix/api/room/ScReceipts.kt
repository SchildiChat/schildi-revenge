package io.element.android.libraries.matrix.api.room

import io.element.android.libraries.matrix.api.core.EventId

/**
 * Receipts to send in a batch for the `read_markers` endpoint.
 */
data class Receipts(
    val fullyRead: EventId?,
    val publicReadReceipt: EventId?,
    val privateReadReceipt: EventId?,
)
