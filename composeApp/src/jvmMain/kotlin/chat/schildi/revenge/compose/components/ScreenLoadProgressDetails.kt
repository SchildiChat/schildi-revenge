package chat.schildi.revenge.compose.components

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import chat.schildi.revenge.model.CheckpointLoadState
import chat.schildi.revenge.model.LoadState
import chat.schildi.revenge.model.LoadStateEntry

@Composable
fun ScreenLoadProgressDetails(
    state: LoadState,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        items(state, key = { it.checkpoint }) { item ->
            ScreenLoadingProgressRow(item)
        }
    }
}

@Composable
private fun ScreenLoadingProgressRow(
    item: LoadStateEntry,
    modifier: Modifier = Modifier,
) {
    val statusIcon = when (item.state) {
        CheckpointLoadState.PENDING -> "⏳"
        CheckpointLoadState.LOADED -> "✅"
        CheckpointLoadState.LOADED_FALLBACK -> "☑"
        CheckpointLoadState.FAILED -> "❌"
    }
    val text = buildString {
        append(statusIcon)
        append(" ")
        append(item.checkpoint.name.render())
        if (item.extraInfo != null) {
            append(": ")
            append(item.extraInfo)
        }
    }
    Text(
        text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier,
    )
}
