package chat.schildi.lib.util

enum class OperatingSystem {
    Linux,
    Windows,
    Mac,
    Unknown,
}

object SystemInfo {

    fun getOsName() = System.getProperty("os.name")

    fun getOs(): OperatingSystem {
        val osName = getOsName().lowercase()
        return when {
            "linux" in osName -> OperatingSystem.Linux
            "windows" in osName -> OperatingSystem.Windows
            "mac" in osName || "darwin" in osName -> OperatingSystem.Mac
            else -> OperatingSystem.Unknown
        }
    }

    fun isLinux() = getOs() == OperatingSystem.Linux
    fun isWindows() = getOs() == OperatingSystem.Windows

    fun javaRuntime(): String = listOfNotNull(
        System.getProperty("java.runtime.name"),
        System.getProperty("java.runtime.version"),
    ).joinToString(" ").ifEmpty { "Unknown" }

    fun javaVm(): String = buildString {
        append(
            listOfNotNull(
                System.getProperty("java.vm.name"),
                System.getProperty("java.vm.version"),
            ).joinToString(" ")
        )
        System.getProperty("java.vendor")?.let { vendor ->
            if (isNotEmpty()) {
                append(" ")
            }
            append("(")
            append(vendor)
            append(")")
        }
    }.ifEmpty { "Unknown" }
}
