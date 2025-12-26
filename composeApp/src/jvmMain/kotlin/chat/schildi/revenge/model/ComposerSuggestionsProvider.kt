package chat.schildi.revenge.model

import androidx.compose.ui.text.input.TextFieldValue
import chat.schildi.revenge.actions.UserIdSuggestionsProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn

class ComposerSuggestionsProvider(
    queryFlow: Flow<TextFieldValue>,
    userIdSuggestionsProvider: UserIdSuggestionsProvider,
    canPingRoomFlow: Flow<Boolean>,
) {
    private val rawSuggestions = combine(
        queryFlow,
        userIdSuggestionsProvider.userIdInRoomSuggestions,
        canPingRoomFlow,
    ) { query, userIds, canPingRoom ->
        val currentCompletionEntity = query.getCurrentCompletionEntity()?.text
        if (currentCompletionEntity == null || !currentCompletionEntity.startsWith("@")) {
            ComposerSuggestionsState()
        } else {
            val userSuggestions = userIds
                .filter {
                    it.userId.value.startsWith(currentCompletionEntity) ||
                            // Allow @displayName as well
                            it.displayName?.startsWith(currentCompletionEntity.substring(1)) == true
                }
                .map { ComposerUserMentionSuggestion(it.userId, it.displayName) }
            val roomSuggestions = if (canPingRoom &&
                ComposerRoomMentionSuggestion.value.startsWith(currentCompletionEntity)
            ) {
                listOf(ComposerRoomMentionSuggestion)
            } else {
                emptyList()
            }
            ComposerSuggestionsState(
                suggestions = (userSuggestions + roomSuggestions).toImmutableList(),
            )
        }
    }.flowOn(Dispatchers.IO)

    val currentSelection = MutableStateFlow<ComposerSuggestion?>(null)

    val suggestionsState = combine(
        rawSuggestions,
        currentSelection,
    ) { state, selection ->
        state.copy(selectedSuggestion = selection.takeIf { state.suggestions.contains(it) })
    }.flowOn(Dispatchers.IO)
}

data class CompletionEntity(
    val start: Int,
    val end: Int,
    val text: String,
) {
    val range: IntRange
        get() = IntRange(start, end-1)
}

fun TextFieldValue.getCurrentCompletionEntity(): CompletionEntity? {
    val cursor = selection.end
    if (cursor <= 0) return null
    val textBeforeCursor = text.substring(0, cursor)
    if (textBeforeCursor.isBlank()) {
        return null
    }
    val startIndex = if (selection.start != cursor) {
        selection.start
    } else {
        val lastWhitespace = textBeforeCursor.indexOfLast { it.isWhitespace() }
        lastWhitespace + 1
    }
    val completionText = text.substring(startIndex, cursor)
    if (completionText.isBlank()) {
        return null
    }
    return CompletionEntity(startIndex, cursor, completionText)
}
