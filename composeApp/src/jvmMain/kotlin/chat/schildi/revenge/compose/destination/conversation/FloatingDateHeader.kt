package chat.schildi.revenge.compose.destination.conversation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.compose.destination.conversation.virtual.DayHeader
import chat.schildi.revenge.model.conversation.ScTimelineItem
import io.element.android.libraries.matrix.api.timeline.MatrixTimelineItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged


@OptIn(FlowPreview::class)
@Composable
fun BoxScope.FloatingDateHeader(
    listState: LazyListState,
    timelineItems: ImmutableList<ScTimelineItem>?,
) {
    var renderedTimestamp by remember { mutableLongStateOf(0L) }
    var isScrolling by remember { mutableStateOf(false) }

    // Collect date to render
    LaunchedEffect(listState, timelineItems) {
        if (timelineItems == null) {
            return@LaunchedEffect
        }
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastIndex }.distinctUntilChanged().collect { _ ->
            renderedTimestamp = listState.layoutInfo.visibleItemsInfo.asReversed().firstNotNullOfOrNull { info ->
                val index = info.index
                (if (index >= 0 && index < timelineItems.size) {
                    when (val item = timelineItems[index].item) {
                        is MatrixTimelineItem.Event -> item.event.timestamp
                        //is MatrixTimelineItem.Virtual -> (item.virtual as? VirtualTimelineItem.DayDivider)?.timestamp
                        //MatrixTimelineItem.Other -> null
                        else -> null
                    }
                } else {
                    null
                })
            } ?: 0L
        }
    }

    // Collect whether user is scrolling
    LaunchedEffect(listState) {
        // Debounce: start scroll should trigger date immediately, but end scroll should delay a bit before hiding date again
        snapshotFlow { listState.isScrollInProgress }.distinctUntilChanged().debounce { if (it) 0 else 1000 }.collect {
            isScrolling = it
        }
    }

    // Render date header
    AnimatedVisibility(
        visible = renderedTimestamp > 0L && isScrolling && listState.canScrollForward, // "forward" = up / towards past (if false, then reached top / no need for date header!)
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = Dimens.Conversation.FloatingDate.topMargin)
    ) {
        DayHeader(
            renderedTimestamp,
            Modifier
                .background(
                    MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                    Dimens.Conversation.FloatingDate.shape,
                )
                .padding(Dimens.Conversation.FloatingDate.contentPadding),
        )
    }
}
