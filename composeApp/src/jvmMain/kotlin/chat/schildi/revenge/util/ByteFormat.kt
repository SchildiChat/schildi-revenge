package chat.schildi.revenge.util

import java.util.Locale

fun Long.formatBytes(): String {
    val units = arrayOf("B", "KiB", "MiB", "GiB", "TiB")
    var value = toDouble()
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.lastIndex) {
        value /= 1024
        unitIndex++
    }
    return String.format(Locale.ROOT, "%.1f %s", value, units[unitIndex])
}
