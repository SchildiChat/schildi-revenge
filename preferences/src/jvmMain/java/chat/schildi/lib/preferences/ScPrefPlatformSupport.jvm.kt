package chat.schildi.lib.preferences

actual val scPrefPlatformSupport = object : ScPrefPlatformSupport {
    override val desktopOnly = true
    override val androidOnly = false
}
