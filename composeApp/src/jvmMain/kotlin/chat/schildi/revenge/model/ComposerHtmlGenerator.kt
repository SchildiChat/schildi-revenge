package chat.schildi.revenge.model

import co.touchlab.kermit.Logger
import io.element.android.libraries.matrix.api.room.IntentionalMention
import io.ktor.http.encodeURLPath
import org.jsoup.nodes.Entities

object ComposerHtmlGenerator {
    private val log = Logger.withTag("ComposerHtml")

    fun generateFormattedHtmlBody(plaintext: String, mentions: List<DraftMention>): String? {
        // If there's no need for HTML, don't.
        if (mentions.none { mention ->
                (mention.mention as? IntentionalMention.User)?.let {
                    it.userId.value != plaintext.substring(mention.range)
                } == true
        }) {
            return null
        }

        // Don't allow entering plaintext html for now
        val original = Entities.escape(plaintext)

        return buildString {
            val mentionsSorted = mentions.sortedBy { it.start }
            var previous: DraftMention? = null
            mentionsSorted.forEach { mention ->
                // Add text up to this mention
                if (previous == null) {
                    append(original.take(mention.start))
                } else if (previous.end > mention.start) {
                    log.e("Drop conflicting mention in render: $mention conflicts with $previous")
                    return@forEach
                } else {
                    append(original.substring(previous.end, mention.start))
                }
                // Add actual mention text replaced
                val mentionText = when (val intentionalMention = mention.mention) {
                    IntentionalMention.Room -> "@room"
                    is IntentionalMention.User -> {
                        val text = original.substring(mention.range)
                        "<a href=\"https://matrix.to/#/${intentionalMention.userId.value.encodeURLPath()}\">$text</a>"
                    }
                }
                append(mentionText)
                previous = mention
            }
            // Add remaining text after last mention
            append(original.substring(previous?.end ?: 0, original.length))
        }
    }
}
