package chat.schildi.revenge.actions

import androidx.compose.runtime.compositionLocalOf
import io.element.android.libraries.matrix.api.core.SessionId

val LocalRoomContextSuggestionsProvider = compositionLocalOf<RoomContextSuggestionsProvider?> { null }

interface RoomContextSuggestionsProvider {
    val sessionId: SessionId
}
