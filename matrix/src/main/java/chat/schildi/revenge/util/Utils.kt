package chat.schildi.revenge.util

inline fun <T>tryOrNull(block: () -> T): T? = try {
    block()
} catch (_: Throwable) {
    null
}