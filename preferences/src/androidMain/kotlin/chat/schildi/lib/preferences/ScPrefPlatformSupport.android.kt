package chat.schildi.lib.preferences

actual val scPrefPlatformSupport = object : ScPrefPlatformSupport {
    override val desktopOnly = false
    override val androidOnly = true
}
