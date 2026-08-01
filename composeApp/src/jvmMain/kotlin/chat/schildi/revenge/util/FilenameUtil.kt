package chat.schildi.revenge.util

private val invalidFilenameCharacters = Regex("[<>:\"/\\\\|?*\\u0000-\\u001f\\u007f]+")
private val nonAsciiPathComponentCharacters = Regex("[^A-Za-z0-9._-]+")
private val windowsReservedFilename = Regex("(?i)^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\\..*)?$")

fun String.toSafeFilename(
    fallback: String = "file",
    maxBytes: Int = 255,
): String {
    require(maxBytes > 0) { "maxBytes must be positive" }

    var normalized = normalizeFilename(this)
    if (normalized.isEmpty()) {
        normalized = normalizeFilename(fallback).ifEmpty { "file" }
    }
    if (windowsReservedFilename.matches(normalized)) {
        normalized = "_$normalized"
    }
    if (normalized.toByteArray().size <= maxBytes) {
        return normalized
    }

    val extensionStart = normalized.lastIndexOf('.').takeIf { it > 0 }
    val extension = extensionStart?.let(normalized::substring).orEmpty()
    val extensionBytes = extension.toByteArray().size
    return if (extensionBytes < maxBytes) {
        normalized.substring(0, extensionStart ?: normalized.length)
            .takeUtf8Bytes(maxBytes - extensionBytes) + extension
    } else {
        normalized.takeUtf8Bytes(maxBytes)
    }
}

fun String.toSafeAsciiPathComponent(
    fallback: String = "file",
    maxLength: Int = 255,
): String {
    require(maxLength > 0) { "maxLength must be positive" }

    return replace(nonAsciiPathComponentCharacters, "_")
        .trim('_')
        .ifEmpty { fallback }
        .take(maxLength)
}

private fun normalizeFilename(value: String): String = value
    .replace(invalidFilenameCharacters, "_")
    .trimEnd { it == '.' || it.isWhitespace() }

private fun String.takeUtf8Bytes(maxBytes: Int): String {
    var byteCount = 0
    var endIndex = 0
    while (endIndex < length) {
        val codePoint = codePointAt(endIndex)
        val codePointBytes = when {
            codePoint <= 0x7f -> 1
            codePoint <= 0x7ff -> 2
            codePoint <= 0xffff -> 3
            else -> 4
        }
        if (byteCount + codePointBytes > maxBytes) break
        byteCount += codePointBytes
        endIndex += Character.charCount(codePoint)
    }
    return substring(0, endIndex)
}
