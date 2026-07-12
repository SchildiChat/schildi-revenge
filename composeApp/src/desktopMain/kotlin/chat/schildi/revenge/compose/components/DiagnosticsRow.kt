package chat.schildi.revenge.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import chat.schildi.revenge.Destination
import chat.schildi.revenge.model.DiagnosticsViewModel
import chat.schildi.revenge.util.formatBytes
import chat.schildi.revenge.viewModelKey
import org.jetbrains.skiko.SkiaLayer
import java.awt.Component
import java.awt.Container

data class WindowDiagnostics(
    val isVsyncEnabled: Boolean?,
) {
    companion object {
        fun from(window: ComposeWindow) = WindowDiagnostics(
            isVsyncEnabled = window.findSkiaLayer()?.properties?.isVsyncEnabled
        )

        private fun Component.findSkiaLayer(): SkiaLayer? {
            if (this is SkiaLayer) return this

            if (this is Container) {
                for (child in components) {
                    child.findSkiaLayer()?.let { return it }
                }
            }

            return null
        }
    }
}

val LocalWindowDiagnostics = compositionLocalOf<WindowDiagnostics?> { null }

@Composable
actual fun DiagnosticsRow(modifier: Modifier) {
    val diagnosticsViewModel: DiagnosticsViewModel =
        viewModel(
            key = viewModelKey(Destination.Diagnostics),
            factory = viewModelFactory { initializer { DiagnosticsViewModel() } },
        )
    val diagnostics = diagnosticsViewModel.state.collectAsState().value
    Row(
        modifier,
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val windowDiagnostics = LocalWindowDiagnostics.current
        val text = buildString {
            append(diagnostics.jvmHeap.usedBytes.formatBytes())
            if (diagnostics.process != null) {
                append("+")
                append(diagnostics.process.estimatedNativeBytes.formatBytes())
                append("/")
                append(diagnostics.process.rssBytes.formatBytes())
            }
            append(" vsync=")
            append(windowDiagnostics?.isVsyncEnabled)
        }
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
