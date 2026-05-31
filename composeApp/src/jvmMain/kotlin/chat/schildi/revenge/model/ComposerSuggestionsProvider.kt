package chat.schildi.revenge.model

import androidx.compose.ui.text.input.TextFieldValue
import chat.schildi.matrixsdk.ImagePack
import chat.schildi.matrixsdk.ImagePackImageSource
import chat.schildi.matrixsdk.ImagePackWithSource
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
    queryFlow: Flow<ComposerState>,
    userIdSuggestionsProvider: UserIdSuggestionsProvider,
    canPingRoomFlow: Flow<Boolean>,
    imagePackFlow: Flow<List<ImagePackWithSource>?>,
) {
    private val rawSuggestions = combine(
        queryFlow,
        userIdSuggestionsProvider.userIdInRoomSuggestions,
        canPingRoomFlow,
        imagePackFlow,
    ) { query, userIds, canPingRoom, imagePacks ->
        if (query !is DraftValue) {
            return@combine ComposerSuggestionsState()
        }
        val currentCompletionEntity = query.textFieldValue.getCurrentCompletionEntity()?.text
        // Don't suggest completions if we're in a draft span already
        val cursorRange = query.textFieldValue.selection
        if (query.spans.any { it.end == cursorRange.start || it.textRange.intersects(cursorRange) }) {
            return@combine ComposerSuggestionsState()
        }
        when {
            currentCompletionEntity == null -> ComposerSuggestionsState()
            currentCompletionEntity.startsWith("@") && query.allowsMention -> {
                val search = currentCompletionEntity.substring(1)
                // Mentions
                val userSuggestions = userIds
                    .filter {
                        it.userId.value.contains(search, ignoreCase = true) ||
                                // Allow @displayName as well
                                it.displayName?.contains(search, ignoreCase = true) == true
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
            currentCompletionEntity.startsWith(":") && query.allowsCustomEmote || query.type == DraftType.STICKER -> {
                val shortcodePrefix = currentCompletionEntity.removePrefix(":")
                // Emojis
                val emojiSuggestion = if (query.allowsCustomEmote) {
                    Emoji.list().filter { it.details.aliases.any { it.contains(shortcodePrefix) } }
                        .map { ComposerEmojiSuggestion(it.details.string, it.details.aliases, it.details.description) }
                } else {
                    emptyList()
                }
                // Custom emotes / stickers
                val customEmoteSuggestions = imagePacks?.flatMap { (pack, source) ->
                    if (query.allowsCustomEmote && pack.supportsCustomEmoji ||
                        query.type == DraftType.STICKER && pack.supportsSticker) {
                        pack.images.toList().filter { (shortcode, image) ->
                            shortcode.contains(shortcodePrefix)
                                    || image.body?.contains(shortcodePrefix) == true
                        }.map {
                            ComposerCustomEmoteSuggestion(
                                ImagePackImageSource(source, pack.pack),
                                it.first,
                                it.second
                            )
                        }
                    } else {
                        emptyList()
                    }
                }.orEmpty()
                // Combined
                val suggestionsSorted = (emojiSuggestion + customEmoteSuggestions).sortedBy {
                    val keys = when (it) {
                        is ComposerCustomEmoteSuggestion -> listOfNotNull(it.shortcode, it.image.body)
                        is ComposerEmojiSuggestion -> it.aliases
                        else -> emptyList()
                    }
                    keys.minOf {
                        it.indexOf(shortcodePrefix).takeIf { it >= 0 } ?: Int.MAX_VALUE
                    }
                }
                ComposerSuggestionsState(suggestionsSorted.toImmutableList())
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
