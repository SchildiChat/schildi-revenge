package chat.schildi.revenge.model.devtools

import chat.schildi.resources.ComposableStringHolder
import io.element.android.libraries.matrix.api.core.RoomId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

sealed interface StateLikeType {
    fun matchesSearch(lower: String): Boolean
    data class AccountData(val eventType: String) : StateLikeType {
        override fun matchesSearch(lower: String) = eventType.lowercase().contains(lower)
    }
    data class RoomAccountData(val roomId: RoomId, val eventType: String) : StateLikeType {
        override fun matchesSearch(lower: String) = eventType.lowercase().contains(lower)
    }
    data class RoomState(val roomId: RoomId, val eventType: String, val stateKey: String) : StateLikeType {
        override fun matchesSearch(lower: String) = eventType.lowercase().contains(lower) || stateKey.lowercase().contains(lower)
    }
}

data class DevToolsStateLikeEventContent<T : StateLikeType>(
    val type: T,
    val content: String,
    val canEdit: Boolean = true,
) {
    fun matchesSearch(lower: String) = type.matchesSearch(lower) || content.lowercase().contains(lower)
}

sealed interface DevToolsSection {
    data class EventList<T : StateLikeType>(
        val title: ComposableStringHolder,
        val entries: ImmutableList<DevToolsStateLikeEventContent<T>>,
        val searchHighlight: String?,
    ) : DevToolsSection
}

fun <T : StateLikeType>ImmutableList<DevToolsStateLikeEventContent<T>>.filterForSearch(lower: String?) = if (lower == null) {
    this
} else {
    filter { it.matchesSearch(lower) }.toPersistentList()
}
