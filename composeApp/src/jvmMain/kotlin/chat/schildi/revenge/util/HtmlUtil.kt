package chat.schildi.revenge.util

fun String.escapeHtml(): String = buildString(length) {
    this@escapeHtml.forEach { char ->
        append(
            when (char) {
                '&' -> "&amp;"
                '<' -> "&lt;"
                '>' -> "&gt;"
                '"' -> "&quot;"
                '\'' -> "&#39;"
                else -> char
            }
        )
    }
}
