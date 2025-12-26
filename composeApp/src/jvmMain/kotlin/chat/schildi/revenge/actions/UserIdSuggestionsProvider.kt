package chat.schildi.revenge.actions

import androidx.compose.runtime.compositionLocalOf
import chat.schildi.revenge.compose.util.toStringHolder
import io.element.android.libraries.matrix.api.core.UserId
import kotlinx.coroutines.flow.Flow

val LocalUserIdSuggestionsProvider = compositionLocalOf<UserIdSuggestionsProvider?> { null }

data class UserIdSuggestion(
    val userId: UserId,
    val displayName: String?,
) {
    fun toCommandSuggestion() = CommandSuggestion(
        value = userId.value,
        hint = displayName?.toStringHolder(),
    )
}

interface UserIdSuggestionsProvider {
    val userIdInRoomSuggestions: Flow<List<UserIdSuggestion>>
}
