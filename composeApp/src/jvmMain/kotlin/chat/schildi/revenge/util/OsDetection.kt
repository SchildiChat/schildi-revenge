package chat.schildi.revenge.util

enum class OperatingSystem {
    Linux,
    Windows,
    Mac,
    Unknown,
}

object OsDetection {

    fun getOsName() = System.getProperty("os.name")

    fun get(): OperatingSystem {
        val osName = getOsName().lowercase()
        return when {
            "linux" in osName -> OperatingSystem.Linux
            "windows" in osName -> OperatingSystem.Windows
            "mac" in osName || "darwin" in osName -> OperatingSystem.Mac
            else -> OperatingSystem.Unknown
        }
    }

    fun isLinux() = get() == OperatingSystem.Linux
    fun isWindows() = get() == OperatingSystem.Windows
}
