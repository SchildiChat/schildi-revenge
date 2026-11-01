package chat.schildi.revenge.model

import androidx.compose.runtime.compositionLocalOf
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.timeline.item.event.ImageLikeMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.MessageType
import io.element.android.libraries.matrix.api.timeline.item.event.VideoMessageType
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import kotlin.collections.plus

val LocalMediaPolicy = compositionLocalOf<ShowMediaPolicy> { ShowMediaPolicy.OptOut() }

fun MessageType.shouldApplyMediaPolicy() = when (this) {
    is ImageLikeMessageType -> true
    is VideoMessageType -> true
    else -> false
}

sealed interface ShowMediaPolicy {
    fun shouldShow(eventId: EventId): Boolean
    fun shouldShow(eventId: EventId?) = eventId?.let(::shouldShow) ?: true
    fun reveal(eventId: EventId): ShowMediaPolicy
    fun hide(eventId: EventId): ShowMediaPolicy
    fun toggle(eventId: EventId): ShowMediaPolicy
    data class OptIn(
        val revealed: ImmutableSet<EventId> = persistentSetOf(),
    ) : ShowMediaPolicy {
        override fun shouldShow(eventId: EventId) = eventId in revealed
        override fun reveal(eventId: EventId): OptIn {
            return if (eventId in revealed) {
                this
            } else {
                copy(revealed = (revealed + eventId).toPersistentSet())
            }
        }
        override fun hide(eventId: EventId): OptIn {
            return if (eventId in revealed) {
                copy(revealed = (revealed - eventId).toPersistentSet())
            } else {
                this
            }
        }
        override fun toggle(eventId: EventId): OptIn {
            return if (eventId in revealed) {
                copy(revealed = (revealed - eventId).toPersistentSet())
            } else {
                copy(revealed = (revealed + eventId).toPersistentSet())
            }
        }
    }

    data class OptOut(
        val hidden: ImmutableSet<EventId> = persistentSetOf(),
    ) : ShowMediaPolicy {
        override fun shouldShow(eventId: EventId) = eventId !in hidden
        override fun reveal(eventId: EventId): OptOut {
            return if (eventId in hidden) {
                copy(hidden = (hidden - eventId).toPersistentSet())
            } else {
                this
            }
        }
        override fun hide(eventId: EventId): OptOut {
            return if (eventId in hidden) {
                this
            } else {
                copy(hidden = (hidden + eventId).toPersistentSet())
            }
        }
        override fun toggle(eventId: EventId): OptOut {
            return if (eventId in hidden) {
                copy(hidden = (hidden - eventId).toPersistentSet())
            } else {
                copy(hidden = (hidden + eventId).toPersistentSet())
            }
        }
    }
}
