package chat.schildi.revenge.model

import co.touchlab.kermit.Logger
import java.lang.management.ManagementFactory
import java.nio.file.Files
import java.nio.file.Path

private val log = Logger.withTag("PlatformDiagnostics")
private val memoryBean =
    runCatching { ManagementFactory.getMemoryMXBean() }
        .onFailure { error -> log.w(error) { "Failed to access JVM management metrics" } }
        .getOrNull()
private val runtime = Runtime.getRuntime()
private var processMemoryReadFailed = false

actual fun readPlatformDiagnostics(): DiagnosticsState {
    val totalMemory = runtime.totalMemory()
    val freeMemory = runtime.freeMemory()
    val nonHeapUsage = memoryBean?.nonHeapMemoryUsage
    val jvmHeap =
        DiagnosticsSnapshot(
            usedBytes = totalMemory - freeMemory,
            committedBytes = totalMemory,
            maxBytes = runtime.maxMemory(),
        )
    val jvmNonHeap =
        DiagnosticsSnapshot(
            usedBytes = nonHeapUsage?.used ?: 0L,
            committedBytes = nonHeapUsage?.committed ?: 0L,
            maxBytes = nonHeapUsage?.max ?: 0L,
        )
    val process =
        readProcessRssBytes()?.let { rssBytes ->
            ProcessDiagnosticsSnapshot(
                rssBytes = rssBytes,
                estimatedNativeBytes = (rssBytes - jvmHeap.usedBytes - jvmNonHeap.usedBytes).coerceAtLeast(0L),
            )
        }
    return DiagnosticsState(
        jvmHeap = jvmHeap,
        jvmNonHeap = jvmNonHeap,
        process = process,
        showProcessMetrics = true,
        processPss = null,
    )
}

private fun readProcessRssBytes(): Long? {
    val statusPath = Path.of("/proc/self/status")
    if (!Files.isReadable(statusPath)) return null
    return runCatching {
        Files.newBufferedReader(statusPath).useLines { lines ->
            lines.firstNotNullOfOrNull(::parseVmRssLine)
        }
    }.onFailure { error ->
        if (!processMemoryReadFailed) {
            processMemoryReadFailed = true
            log.w(error) { "Failed to read process RSS from /proc/self/status" }
        }
    }.getOrNull()
}

private fun parseVmRssLine(line: String): Long? {
    if (!line.startsWith("VmRSS:")) return null
    val kibibytes =
        line
            .substringAfter(':')
            .trim()
            .substringBefore(' ')
            .toLongOrNull()
            ?: return null
    return kibibytes * 1024L
}
