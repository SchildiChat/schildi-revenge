package chat.schildi.revenge.model

import chat.schildi.matrixsdk.ImagePackImageSource
import chat.schildi.matrixsdk.ImagePackImageWithRawInfo
import chat.schildi.revenge.actions.ActionContext
import chat.schildi.revenge.actions.ActionResult
import chat.schildi.revenge.compose.util.ComposableStringHolder
import chat.schildi.revenge.compose.util.toStringHolder
import com.beeper.android.messageformat.InlineImageInfo
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.room.IntentionalMention
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.StateFlow

sealed interface ComposerSuggestion {
    val value: String
    val hint: ComposableStringHolder?
    val shouldAppendSpace: Boolean
    val previewImage: InlineImageInfo?
        get() = null
    fun buildDraftSpan(start: Int, end: Int): DraftSpan? = null
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
    override fun buildDraftSpan(start: Int, end: Int) =
        DraftMention(start = start, end = end, mention = IntentionalMention.User(userId))
}

data object ComposerRoomMentionSuggestion : ComposerSuggestion {
    override val shouldAppendSpace = true
    override val value = "@room"
    override val hint: ComposableStringHolder? = null
    override fun buildDraftSpan(start: Int, end: Int) =
        DraftMention(start = start, end = end, mention = IntentionalMention.Room)
}

data class ComposerEmojiSuggestion(
    override val value: String,
    val aliases: List<String>,
    val description: String?,
) : ComposerSuggestion {
    override val shouldAppendSpace = false
    override val hint: ComposableStringHolder? = description?.toStringHolder()
}

data class ComposerCustomEmoteSuggestion(
    val source: ImagePackImageSource,
    val shortcode: String,
    val image: ImagePackImageWithRawInfo,
) : ComposerSuggestion {
    override val shouldAppendSpace = false
    override val hint = listOfNotNull(
        image.body,
        source.info?.displayName,
    ).takeIf { it.isNotEmpty() }?.joinToString(separator = " | ")?.toStringHolder()
    override val value = ":$shortcode:"
    override val previewImage = InlineImageInfo(
        image.url,
        isEmote = true,
        width = null,
        height = null,
        title = shortcode,
        alt = image.body,
    )
    override fun buildDraftSpan(start: Int, end: Int) =
        DraftCustomEmote(start = start, end = end, shortcode = shortcode, image = image, source = source)
}

data class ComposerSuggestionsState(
    val suggestions: ImmutableList<ComposerSuggestion> = persistentListOf(),
    val selectedSuggestion: ComposerSuggestion? = null,
)

data class ComposerRoomInfo(
    val isEncrypted: Boolean?,
    val isPublic: Boolean?,
)

interface ComposerViewModel {
    val composerState: StateFlow<ComposerState>
    val composerSuggestions: StateFlow<ComposerSuggestionsState>
    val composerRoomInfo: StateFlow<ComposerRoomInfo?>
    fun onComposerUpdate(value: DraftValue)
    fun sendMessage(context: ActionContext): ActionResult
    fun attachFile(context: ActionContext, path: String): Boolean
    fun launchAttachmentPicker(context: ActionContext): ActionResult
    fun clearAttachment()
    fun onConfirmSuggestion(suggestion: ComposerSuggestion): Boolean
}
