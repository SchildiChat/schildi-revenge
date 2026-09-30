package chat.schildi.revenge.compose.destination.conversation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import chat.schildi.lib.preferences.ScPrefs
import chat.schildi.revenge.model.conversation.ConversationViewModel
import chat.schildi.revenge.preferences.value
import io.element.android.libraries.matrix.api.timeline.MatrixTimelineItem

@Composable
fun ConversationTopDebugHeader(
    viewModel: ConversationViewModel,
    modifier: Modifier = Modifier,
) {
    val isLiveMergeEnabled = ScPrefs.ALLOW_LIVE_TIMELINE_MERGE.value()
    if (!ScPrefs.SHOW_DEV_INFOS.value() && isLiveMergeEnabled) {
        return
    }
    val state = viewModel.activeTimelineState.collectAsState().value ?: return
    val latestRead = viewModel.latestSeenMessage.collectAsState().value
    val seenUnread = viewModel.hasSeenUnreadLine.collectAsState().value
    val text = remember(
        isLiveMergeEnabled,
        state.isLive,
        state.mergeOffset,
        state.items.size,
        latestRead,
        seenUnread,
    ) {
        buildString {
            if (!isLiveMergeEnabled) {
                append("[WARN: LIVE TIMELINE MERGE DISABLED] ")
            }
            when {
                !state.isLive -> append("detached")
                state.mergeOffset >= 0 -> append("merged@${state.items.size - state.mergeOffset - 1}")
                else -> append("live")
            }
            latestRead?.let {
                append(" TS=")
                append(it.value.take(13))
                val index = state.items.indexOfLast { (it as? MatrixTimelineItem.Event)?.eventId == latestRead }
                if (index >= 0) {
                    append("@")
                    append(state.items.size - index - 1)
                }
                append(" SR=")
                append(seenUnread)
            }
        }
    }
    Text(
        text,
        modifier.padding(4.dp),
        color = if (isLiveMergeEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelSmall,
    )
}
