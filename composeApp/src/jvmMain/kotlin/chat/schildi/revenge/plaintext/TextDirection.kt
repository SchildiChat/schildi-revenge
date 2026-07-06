package chat.schildi.revenge.plaintext

private const val FIRST_STRONG_ISOLATE = '\u2068'
private const val POP_DIRECTIONAL_ISOLATE = '\u2069'

private val bidiControlChars = setOf(
    '\u061C',
    '\u200E',
    '\u200F',
    '\u202A',
    '\u202B',
    '\u202C',
    '\u202D',
    '\u202E',
    '\u2066',
    '\u2067',
    '\u2068',
    '\u2069',
)

internal fun String.sanitizeDirection(): String {
    return if (any { it in bidiControlChars }) {
        "$FIRST_STRONG_ISOLATE$this$POP_DIRECTIONAL_ISOLATE"
    } else {
        this
    }
}
