package chat.schildi.revenge.model

import chat.schildi.revenge.actions.ActionContext
import chat.schildi.revenge.actions.ActionResult
import chat.schildi.revenge.compose.util.ComposableStringHolder
import chat.schildi.revenge.compose.util.toStringHolder
import io.element.android.libraries.matrix.api.core.UserId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.StateFlow

sealed interface ComposerSuggestion {
    val value: String
    val hint: ComposableStringHolder?
    val shouldAppendSpace: Boolean
}

data class ComposerUserMentionSuggestion(
    val userId: UserId,
    val displayName: String?,
) : ComposerSuggestion {
    override val shouldAppendSpace = true
    override val value: String
        get() = displayName ?: userId.value
    override val hint: ComposableStringHolder?
        get() = if (displayName == null) {
            null
        } else {
            userId.value.toStringHolder()
        }
}

data class ComposerEmojiSuggestion(
    override val value: String,
    val description: String?
) : ComposerSuggestion {
    override val shouldAppendSpace = false
    override val hint: ComposableStringHolder? = description?.toStringHolder()
}

data object ComposerRoomMentionSuggestion : ComposerSuggestion {
    override val shouldAppendSpace = true
    override val value = "@room"
    override val hint: ComposableStringHolder? = null
}

data class ComposerSuggestionsState(
    val suggestions: ImmutableList<ComposerSuggestion> = persistentListOf(),
    val selectedSuggestion: ComposerSuggestion? = null,
)

// TODO also expose can-send-message permissions
interface ComposerViewModel {
    val composerState: StateFlow<DraftValue>
    val composerSuggestions: StateFlow<ComposerSuggestionsState>
    fun onComposerUpdate(value: DraftValue)
    fun sendMessage(context: ActionContext): ActionResult
    fun attachFile(context: ActionContext, path: String): Boolean
    fun launchAttachmentPicker(context: ActionContext): ActionResult
    fun clearAttachment()
    fun onConfirmSuggestion(suggestion: ComposerSuggestion): Boolean
}
