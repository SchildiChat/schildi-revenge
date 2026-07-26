package chat.schildi.revenge.model

import android.os.Debug

actual fun readPlatformDiagnostics(): DiagnosticsState {
    val runtime = Runtime.getRuntime()
    val totalMemory = runtime.totalMemory()
    val memoryInfo = Debug.MemoryInfo()
    Debug.getMemoryInfo(memoryInfo)
    return DiagnosticsState(
        jvmHeap = DiagnosticsSnapshot(
            usedBytes = totalMemory - runtime.freeMemory(),
            committedBytes = totalMemory,
            maxBytes = runtime.maxMemory(),
        ),
        jvmNonHeap = null,
        process = null,
        showProcessMetrics = false,
        processPss = ProcessPssDiagnosticsSnapshot(
            totalBytes = memoryInfo.totalPss.toLong() * 1024L,
            dalvikBytes = memoryInfo.dalvikPss.toLong() * 1024L,
            nativeBytes = memoryInfo.nativePss.toLong() * 1024L,
            otherBytes = memoryInfo.otherPss.toLong() * 1024L,
        ),
    )
}
