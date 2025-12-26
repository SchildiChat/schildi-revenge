package chat.schildi.revenge.actions

import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.flow.Flow

val LocalUserIdSuggestionsProvider = compositionLocalOf<UserIdSuggestionsProvider?> { null }

interface UserIdSuggestionsProvider {
    val userIdInRoomSuggestions: Flow<List<CommandSuggestion>>
}
