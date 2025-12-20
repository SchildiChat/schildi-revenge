package chat.schildi.revenge.compose.util

object UrlUtil {
    private val urlRegex = Regex(
        """\b((?:https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])""",
        RegexOption.IGNORE_CASE
    )

    fun extractUrlsFromText(text: String): List<String> =
        urlRegex.findAll(text).map { it.value }.toList()
}
