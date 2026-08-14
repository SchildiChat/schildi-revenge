package chat.schildi.lib.preferences

interface ScPrefPlatformSupport {
    val desktopOnly: Boolean
    val notifications: Boolean
        get() = pushNotifications || desktopNotifications
    val desktopNotifications: Boolean
    val pushNotifications: Boolean
    val androidOnly: Boolean
}

expect val scPrefPlatformSupport: ScPrefPlatformSupport
