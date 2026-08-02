package chat.schildi.revenge.model.devtools

import chat.schildi.resources.ComposableStringHolder
import io.element.android.libraries.matrix.api.core.RoomId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.serialization.Serializable

@Serializable
sealed interface StateLikeType {
    val renderKey: String
    fun matchesSearch(lower: String): Boolean
    @Serializable
    data class AccountData(val eventType: String) : StateLikeType {
        override val renderKey = "a/$eventType"
        override fun matchesSearch(lower: String) = eventType.lowercase().contains(lower)
    }
    @Serializable
    data class RoomAccountData(val roomId: RoomId, val eventType: String) : StateLikeType {
        override val renderKey = "ra/$roomId/$eventType"
        override fun matchesSearch(lower: String) = eventType.lowercase().contains(lower)
    }
    @Serializable
    data class RoomState(val roomId: RoomId, val eventType: String, val stateKey: String) : StateLikeType {
        override val renderKey = "r/$roomId/$eventType/$stateKey"
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
