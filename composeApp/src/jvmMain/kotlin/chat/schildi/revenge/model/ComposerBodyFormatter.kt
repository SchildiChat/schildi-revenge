package chat.schildi.revenge.model

import co.touchlab.kermit.Logger

object ComposerBodyFormatter {
    private val log = Logger.withTag("ComposerBodyFormatter")

    fun expandDraftSpans(
        plaintext: String,
        spans: List<DraftSpan>,
        allowHtml: Boolean,
    ): String {
        // If there's no need, don't.
        if (spans.isEmpty()) {
            return plaintext
        }

        return try {
            buildString {
                val spansSorted = spans.sortedBy { it.start }
                var previous: DraftSpan? = null
                spansSorted.forEach { span ->
                    // Add text up to this span
                    if (previous == null) {
                        append(plaintext.take(span.start))
                    } else if (previous.end > span.start) {
                        log.e("Drop conflicting draft span in render: $span conflicts with $previous")
                        return@forEach
                    } else {
                        append(plaintext.substring(previous.end, span.start))
                    }
                    // Add actual span text replaced
                    val content = plaintext.substring(span.range)
                    if (allowHtml) {
                        append(span.formatContentToHtml(content))
                    } else {
                        append(span.formatContentToPlaintext(content))
                    }
                    previous = span
                }
                // Add remaining text after last span
                append(plaintext.substring(previous?.end ?: 0, plaintext.length))
            }
        } catch (e: StringIndexOutOfBoundsException) {
            log.e("Span out of bounds, edit race? $e")
            plaintext
        }
    }
}
