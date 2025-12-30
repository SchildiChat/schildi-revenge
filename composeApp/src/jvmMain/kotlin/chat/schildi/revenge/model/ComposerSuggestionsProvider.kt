package chat.schildi.revenge.model

import androidx.compose.ui.text.input.TextFieldValue
import chat.schildi.revenge.actions.UserIdSuggestionsProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import org.kodein.emoji.Emoji
import org.kodein.emoji.list

class ComposerSuggestionsProvider(
    queryFlow: Flow<DraftValue>,
    userIdSuggestionsProvider: UserIdSuggestionsProvider,
    canPingRoomFlow: Flow<Boolean>,
) {
    private val rawSuggestions = combine(
        queryFlow,
        userIdSuggestionsProvider.userIdInRoomSuggestions,
        canPingRoomFlow,
    ) { query, userIds, canPingRoom ->
        val currentCompletionEntity = query.textFieldValue.getCurrentCompletionEntity()?.text
        // Don't suggest completions if we're in a mention already
        val cursorRange = query.textFieldValue.selection
        if (query.mentions.any { it.end == cursorRange.start || it.textRange.intersects(cursorRange) }) {
            return@combine ComposerSuggestionsState()
        }
        when {
            currentCompletionEntity == null -> ComposerSuggestionsState()
            currentCompletionEntity.startsWith("@") -> {
                // Mentions
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
            currentCompletionEntity.startsWith(":") -> {
                // Emojis
                val shortcodePrefix = currentCompletionEntity.substring(1)
                val suggestions = Emoji.list().filter { it.details.aliases.any { it.startsWith(shortcodePrefix) } }
                    .map { ComposerEmojiSuggestion(it.details.string, it.details.description) }
                ComposerSuggestionsState(suggestions.toImmutableList())
            }
            else -> ComposerSuggestionsState()
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
    val cursor = selection.max
    if (cursor <= 0) return null
    val textBeforeCursor = text.substring(0, cursor)
    if (textBeforeCursor.isBlank()) {
        return null
    }
    val startIndex = if (selection.min != cursor) {
        selection.min
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
