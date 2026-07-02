package chat.schildi.revenge.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chat.schildi.revenge.Destination
import chat.schildi.revenge.TitleProvider
import chat.schildi.resources.toStringHolder
import co.touchlab.kermit.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import shire.res.generated.resources.Res
import shire.res.generated.resources.diagnostics
import java.lang.management.ManagementFactory
import java.nio.file.Files
import java.nio.file.Path

data class DiagnosticsSnapshot(
    val usedBytes: Long,
    val committedBytes: Long,
    val maxBytes: Long,
)

data class ProcessDiagnosticsSnapshot(
    val rssBytes: Long,
    val estimatedNativeBytes: Long,
)

data class DiagnosticsState(
    val jvmHeap: DiagnosticsSnapshot,
    val jvmNonHeap: DiagnosticsSnapshot,
    val process: ProcessDiagnosticsSnapshot?,
)

class DiagnosticsViewModel : ViewModel(), TitleProvider {
    private val log = Logger.withTag("DiagnosticsViewModel")
    private val memoryBean =
        runCatching { ManagementFactory.getMemoryMXBean() }
            .onFailure { error ->
                log.w(error) { "Failed to access JVM management metrics" }
            }.getOrNull()
    private val runtime = Runtime.getRuntime()
    private var processMemoryReadFailed = false

    private val _state = MutableStateFlow(snapshot())
    val state = _state.asStateFlow()

    override val windowTitle = flowOf(Res.string.diagnostics.toStringHolder())

    override fun verifyDestination(destination: Destination) = destination is Destination.Diagnostics

    init {
        viewModelScope.launch {
            while (isActive) {
                _state.value = snapshot()
                delay(1_000)
            }
        }
    }

    private fun snapshot(): DiagnosticsState {
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
}
