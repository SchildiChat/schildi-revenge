package android.util

object Base64 {
    const val NO_PADDING = 1
    const val NO_WRAP = 2

    fun encodeToString(src: ByteArray, flags: Int): String {
        var encoder = java.util.Base64.getEncoder()
        if ((flags and NO_PADDING) != 0) {
            encoder = encoder.withoutPadding()
        }
        return encoder.encodeToString(src)
    }
}
