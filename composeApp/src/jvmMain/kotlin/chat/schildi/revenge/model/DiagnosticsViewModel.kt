package chat.schildi.revenge.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chat.schildi.revenge.Destination
import chat.schildi.revenge.TitleProvider
import chat.schildi.resources.toStringHolder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import shire.res.generated.resources.Res
import shire.res.generated.resources.diagnostics

data class DiagnosticsSnapshot(
    val usedBytes: Long,
    val committedBytes: Long,
    val maxBytes: Long,
)

data class ProcessDiagnosticsSnapshot(
    val rssBytes: Long,
    val estimatedNativeBytes: Long,
)

data class ProcessPssDiagnosticsSnapshot(
    val totalBytes: Long,
    val dalvikBytes: Long,
    val nativeBytes: Long,
    val otherBytes: Long,
)

data class DiagnosticsState(
    val jvmHeap: DiagnosticsSnapshot,
    val jvmNonHeap: DiagnosticsSnapshot?,
    val process: ProcessDiagnosticsSnapshot?,
    val showProcessMetrics: Boolean,
    val processPss: ProcessPssDiagnosticsSnapshot?,
)

class DiagnosticsViewModel : ViewModel(), TitleProvider {
    private val _state = MutableStateFlow(readPlatformDiagnostics())
    val state = _state.asStateFlow()

    override val windowTitle = flowOf(Res.string.diagnostics.toStringHolder())

    override fun verifyDestination(destination: Destination) = destination is Destination.Diagnostics

    init {
        viewModelScope.launch {
            while (isActive) {
                _state.value = readPlatformDiagnostics()
                delay(1_000)
            }
        }
    }
}
