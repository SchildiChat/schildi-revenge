package chat.schildi.lib.preferences

actual val scPrefPlatformSupport = object : ScPrefPlatformSupport {
    override val desktopOnly = false
    override val desktopNotifications = false
    override val pushNotifications = true
    override val androidOnly = true
    override val preferTouch = true
}
