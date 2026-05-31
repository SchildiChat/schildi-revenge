package chat.schildi.revenge.compose.util

import androidx.compose.ui.text.AnnotatedString
import com.beeper.android.messageformat.InlineImageInfo
import com.beeper.android.messageformat.MatrixBodyAnnotations
import org.kodein.emoji.EmojiFinder
import org.kodein.emoji.findEmoji

object EmojiUtil {
    private val finder = EmojiFinder()

    fun String.containsOnlyEmojis(): Boolean {
        var index = 0
        finder.findEmoji(this).forEach { emoji ->
            if (substring(index, emoji.start).isNotBlank()) return false
            index = emoji.end
        }
        return substring(index).isBlank()
    }

    fun AnnotatedString.containsOnlyEmojis(
        inlineImages: Map<String, InlineImageInfo> = emptyMap(),
    ): Boolean {
        val stringToCheck = if (inlineImages.isNotEmpty()) {
            if (inlineImages.values.any { !it.isEmote }) {
                return false
            }
            // If all is custom emotes, those also count as emojis.
            val customEmotes = getStringAnnotations(MatrixBodyAnnotations.INLINE_IMAGE, 0, length)
                .sortedByDescending { it.start }
            var toCheck: CharSequence = this
            customEmotes.forEach {
                toCheck = toCheck.removeRange(it.start, it.end)
            }
            toCheck.toString()
        } else {
            toString()
        }
        return stringToCheck.containsOnlyEmojis()
    }
}
