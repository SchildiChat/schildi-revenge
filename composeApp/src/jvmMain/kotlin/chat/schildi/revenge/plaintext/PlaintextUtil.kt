package chat.schildi.revenge.plaintext

fun String.limit(maxLength: Int) = if (length <= maxLength) {
    this
} else {
    take(maxLength - 1) + "…"
}
