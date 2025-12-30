package chat.schildi.revenge.actions

import androidx.compose.runtime.compositionLocalOf
import chat.schildi.revenge.PrettyJson
import co.touchlab.kermit.Logger
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.room.BaseRoom
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

val LocalRoomContextSuggestionsProvider = compositionLocalOf<RoomContextSuggestionsProvider?> { null }

data class StateEventCompletionSnapshot(
    val eventType: String,
    val stateKey: String,
    val content: String,
)

class RoomContextSuggestionsProvider(
    val sessionId: SessionId,
    private val peekRoom: () -> BaseRoom?,
) {
    private val log = Logger.withTag("RoomContextSuggestions")
    private val cachedStateEvents = MutableStateFlow<List<StateEventCompletionSnapshot>?>(null)
    val stateEventSuggestions = cachedStateEvents.asStateFlow()
    private var invalidated = false
    fun invalidateCachedState() {
        invalidated = true
    }
    fun prefetchState(scope: CoroutineScope) {
        if (cachedStateEvents.value != null && !invalidated) return
        peekRoom()?.let { room ->
            scope.launch(Dispatchers.IO) {
                invalidated = false
                val fullRoomState = room.fetchFullRoomState()
                    .onFailure { log.w("Failed to fetch full room state", it) }
                    .getOrNull()
                if (fullRoomState != null) {
                    val parsed = fullRoomState.parseRoomStateSnapshot(log)
                    cachedStateEvents.emit(parsed)
                }
            }
        }
    }
}

fun List<String>.parseRoomStateSnapshot(log: Logger) = mapNotNull {
    try {
        val parsed = PrettyJson.parseToJsonElement(it).jsonObject
        StateEventCompletionSnapshot(
            eventType = parsed["type"]!!.jsonPrimitive.content,
            stateKey = parsed["state_key"]!!.jsonPrimitive.content,
            content = PrettyJson.encodeToString(parsed["content"]),
        )
    } catch (e: Exception) {
        log.e("Failed to parse state event", e)
        null
    }
}
