package chat.schildi.revenge.compose.destination

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import chat.schildi.revenge.Destination
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.ListActions
import chat.schildi.revenge.actions.LocalListActionProvider
import chat.schildi.revenge.compose.components.TopNavigation
import chat.schildi.revenge.compose.components.TopNavigationCloseOrNavigateToInboxIcon
import chat.schildi.revenge.compose.components.TopNavigationTitle
import chat.schildi.revenge.compose.focus.FocusContainer
import chat.schildi.revenge.model.DiagnosticsSnapshot
import chat.schildi.revenge.model.DiagnosticsViewModel
import chat.schildi.revenge.model.ProcessDiagnosticsSnapshot
import chat.schildi.revenge.model.ProcessPssDiagnosticsSnapshot
import chat.schildi.revenge.publishTitle
import chat.schildi.revenge.util.formatBytes
import chat.schildi.revenge.viewModelKey
import org.jetbrains.compose.resources.stringResource
import shire.res.generated.resources.Res
import shire.res.generated.resources.diagnostics
import shire.res.generated.resources.diagnostics_committed
import shire.res.generated.resources.diagnostics_current
import shire.res.generated.resources.diagnostics_dalvik
import shire.res.generated.resources.diagnostics_jvm_heap
import shire.res.generated.resources.diagnostics_jvm_non_heap
import shire.res.generated.resources.diagnostics_max
import shire.res.generated.resources.diagnostics_native
import shire.res.generated.resources.diagnostics_native_estimated
import shire.res.generated.resources.diagnostics_other
import shire.res.generated.resources.diagnostics_process_pss
import shire.res.generated.resources.diagnostics_process_rss
import shire.res.generated.resources.diagnostics_render_api
import shire.res.generated.resources.diagnostics_system_info
import shire.res.generated.resources.diagnostics_total
import shire.res.generated.resources.diagnostics_unavailable

@Composable
fun DiagnosticsScreen(
    destination: Destination.Diagnostics,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
) {
    val renderApi = remember { platformRenderApi() }
    val viewModel: DiagnosticsViewModel =
        viewModel(
            key = viewModelKey(destination),
            factory = viewModelFactory { initializer { DiagnosticsViewModel() } },
        )
    publishTitle(viewModel)
    val state = viewModel.state.collectAsState().value
    val listState = rememberLazyListState()
    val listAction = remember(listState) { ListActions(listState) }
    FocusContainer(
        LocalListActionProvider provides listAction,
        modifier = modifier.windowInsetsPadding(
            WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        ),
        role = FocusRole.DESTINATION_ROOT_CONTAINER,
    ) {
        Column {
            TopNavigation {
                TopNavigationTitle(stringResource(Res.string.diagnostics))
                TopNavigationCloseOrNavigateToInboxIcon()
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LazyColumn(
                    modifier = contentModifier.padding(horizontal = Dimens.windowPadding),
                    verticalArrangement = Dimens.verticalArrangement,
                    state = listState,
                    contentPadding = WindowInsets.navigationBars
                        .only(WindowInsetsSides.Bottom)
                        .asPaddingValues(),
                ) {
                    item(key = "systemInfo") {
                        MetricCard(
                            title = stringResource(Res.string.diagnostics_system_info),
                        ) {
                            MetricLine(
                                label = stringResource(Res.string.diagnostics_render_api),
                                value = renderApi,
                            )
                        }
                    }
                    item(key = "jvm") {
                        MemorySection(
                            title = stringResource(Res.string.diagnostics_jvm_heap),
                            snapshot = state.jvmHeap,
                        )
                    }
                    state.jvmNonHeap?.let { jvmNonHeap ->
                        item(key = "nonHeap") {
                            MemorySection(
                                title = stringResource(Res.string.diagnostics_jvm_non_heap),
                                snapshot = jvmNonHeap,
                            )
                        }
                    }
                    if (state.showProcessMetrics) {
                        item(key = "process") {
                            ProcessMemorySection(
                                snapshot = state.process,
                            )
                        }
                    }
                    state.processPss?.let { processPss ->
                        item(key = "processPss") {
                            ProcessPssSection(snapshot = processPss)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProcessMemorySection(
    snapshot: ProcessDiagnosticsSnapshot?,
    modifier: Modifier = Modifier,
) {
    MetricCard(
        title = stringResource(Res.string.diagnostics_process_rss),
        modifier = modifier,
    ) {
        MetricLine(
            label = stringResource(Res.string.diagnostics_current),
            value = snapshot?.rssBytes?.formatBytes() ?: stringResource(Res.string.diagnostics_unavailable),
        )
    }
    MetricCard(
        title = stringResource(Res.string.diagnostics_native_estimated),
        modifier = modifier.padding(top = 12.dp),
    ) {
        MetricLine(
            label = stringResource(Res.string.diagnostics_current),
            value = snapshot?.estimatedNativeBytes?.formatBytes() ?: stringResource(Res.string.diagnostics_unavailable),
        )
    }
}

@Composable
private fun ProcessPssSection(
    snapshot: ProcessPssDiagnosticsSnapshot,
    modifier: Modifier = Modifier,
) {
    MetricCard(
        title = stringResource(Res.string.diagnostics_process_pss),
        modifier = modifier,
    ) {
        MetricLine(
            label = stringResource(Res.string.diagnostics_total),
            value = snapshot.totalBytes.formatBytes(),
        )
        MetricLine(
            label = stringResource(Res.string.diagnostics_dalvik),
            value = snapshot.dalvikBytes.formatBytes(),
        )
        MetricLine(
            label = stringResource(Res.string.diagnostics_native),
            value = snapshot.nativeBytes.formatBytes(),
        )
        MetricLine(
            label = stringResource(Res.string.diagnostics_other),
            value = snapshot.otherBytes.formatBytes(),
        )
    }
}

@Composable
private fun MemorySection(
    title: String,
    snapshot: DiagnosticsSnapshot,
    modifier: Modifier = Modifier,
) {
    MetricCard(title = title, modifier = modifier) {
        MetricLine(
            label = stringResource(Res.string.diagnostics_current),
            value = snapshot.usedBytes.formatBytes(),
        )
        MetricLine(
            label = stringResource(Res.string.diagnostics_committed),
            value = snapshot.committedBytes.formatBytes(),
        )
        MetricLine(
            label = stringResource(Res.string.diagnostics_max),
            value = snapshot.maxBytes.formatBytes(),
        )
        LinearProgressIndicator(
            progress = { safeRatio(snapshot.usedBytes, snapshot.maxBytes) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MetricCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            content()
        }
    }
}

@Composable
private fun MetricLine(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = FontFamily.Monospace,
        )
    }
}

private fun safeRatio(
    value: Long,
    maxValue: Long,
): Float {
    if (maxValue <= 0L) return 0f
    return (value.toDouble() / maxValue.toDouble()).coerceIn(0.0, 1.0).toFloat()
}
