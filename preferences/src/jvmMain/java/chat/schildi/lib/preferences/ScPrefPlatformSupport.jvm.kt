package chat.schildi.lib.preferences

import chat.schildi.lib.util.SystemInfo

actual val scPrefPlatformSupport = object : ScPrefPlatformSupport {
    override val desktopOnly = true
    // TODO fix windows notifications
    override val desktopNotifications = !SystemInfo.isWindows()
    override val pushNotifications = false
    override val androidOnly = false
}
