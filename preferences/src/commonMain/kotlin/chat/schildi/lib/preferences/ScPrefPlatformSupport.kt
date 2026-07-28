package chat.schildi.lib.preferences

interface ScPrefPlatformSupport {
    val desktopOnly: Boolean
    val androidOnly: Boolean
}

expect val scPrefPlatformSupport: ScPrefPlatformSupport
