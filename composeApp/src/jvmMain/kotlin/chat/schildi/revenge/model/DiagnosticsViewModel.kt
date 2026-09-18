package chat.schildi.revenge.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chat.schildi.revenge.Destination
import chat.schildi.revenge.TitleProvider
import chat.schildi.revenge.UiState
import chat.schildi.revenge.config.ScAppDirs
import chat.schildi.resources.toStringHolder
import io.element.android.libraries.core.data.ByteUnit
import io.element.android.libraries.matrix.api.core.SessionId
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

data class SdkStoreSizesSnapshot(
    val sessionId: SessionId,
    val stateStoreBytes: Long?,
    val eventCacheStoreBytes: Long?,
    val mediaStoreBytes: Long?,
    val cryptoStoreBytes: Long?,
)

class DiagnosticsViewModel : ViewModel(), TitleProvider {
    private val _state = MutableStateFlow(readPlatformDiagnostics())
    val state = _state.asStateFlow()

    private val _directorySizes = MutableStateFlow<Map<String, Long>?>(null)
    val directorySizes = _directorySizes.asStateFlow()

    private val _sdkStoreSizes = MutableStateFlow<List<SdkStoreSizesSnapshot>>(emptyList())
    val sdkStoreSizes = _sdkStoreSizes.asStateFlow()

    override val windowTitle = flowOf(Res.string.diagnostics.toStringHolder())

    override fun verifyDestination(destination: Destination) = destination is Destination.Diagnostics

    init {
        viewModelScope.launch {
            _directorySizes.value = withContext(Dispatchers.IO) {
                appDirectoryPaths().associateWith { path ->
                    File(path).walkTopDown().sumOf { it.length() }
                }
            }
        }
        viewModelScope.launch {
            while (isActive) {
                _state.value = readPlatformDiagnostics()
                delay(1_000)
            }
        }
        viewModelScope.launch {
            while (isActive) {
                _sdkStoreSizes.value = withContext(Dispatchers.IO) {
                    val sessionIdComparator = UiState.sessionIdComparator.first()
                    UiState.matrixClients.value.entries.map { (sessionId, client) ->
                        val sizes = client.getDatabaseSizes().getOrNull()
                        SdkStoreSizesSnapshot(
                            sessionId = sessionId,
                            stateStoreBytes = sizes?.stateStore?.into(ByteUnit.BYTES),
                            eventCacheStoreBytes = sizes?.eventCacheStore?.into(ByteUnit.BYTES),
                            mediaStoreBytes = sizes?.mediaStore?.into(ByteUnit.BYTES),
                            cryptoStoreBytes = sizes?.cryptoStore?.into(ByteUnit.BYTES),
                        )
                    }.sortedWith(compareBy(sessionIdComparator) { it.sessionId })
                }
                delay(5_000)
            }
        }
    }
}

private fun appDirectoryPaths(): List<String> = listOf(
    ScAppDirs.getUserDataDir(),
    ScAppDirs.getUserConfigDir(),
    ScAppDirs.getUserCacheDir(),
    ScAppDirs.getSiteDataDir(),
    ScAppDirs.getSiteConfigDir(),
    ScAppDirs.getUserLogDir(),
    ScAppDirs.getSharedDir(),
)
