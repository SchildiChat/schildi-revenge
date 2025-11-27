package chat.schildi.revenge.compose.destination

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import chat.schildi.revenge.compose.media.imageLoader
import chat.schildi.revenge.compose.media.onAsyncImageError
import chat.schildi.revenge.compose.model.ChatViewModel
import chat.schildi.revenge.navigation.ChatDestination
import chat.schildi.revenge.publishTitle
import coil3.compose.AsyncImage
import io.element.android.libraries.matrix.api.timeline.MatrixTimelineItem
import io.element.android.libraries.matrix.api.timeline.item.event.ImageMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.MessageContent
import io.element.android.libraries.matrix.api.timeline.item.virtual.VirtualTimelineItem
import io.element.android.libraries.matrix.ui.media.MediaRequestData
import kotlinx.collections.immutable.persistentListOf

@Composable
fun ChatScreen(destination: ChatDestination) {
    val viewModel: ChatViewModel = viewModel(factory = ChatViewModel.factory(destination.sessionId, destination.roomId))
    val timelineItems = viewModel.timelineItems.collectAsState(persistentListOf()).value
    val forwardPaginationStatus = viewModel.forwardPaginationStatus.collectAsState(null).value
    val backwardPaginationStatus = viewModel.backwardPaginationStatus.collectAsState(null).value
    val listState = rememberLazyListState()

    publishTitle(viewModel)

    LaunchedEffect(listState, forwardPaginationStatus) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        }
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex != null && lastVisibleIndex >= timelineItems.size - 3 && forwardPaginationStatus?.canPaginate == true) {
                    viewModel.paginateForward()
                }
            }
    }
    LaunchedEffect(listState, backwardPaginationStatus) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index
        }
            .collect { firstVisibleIndex ->
                if (firstVisibleIndex != null && firstVisibleIndex <= 3 && backwardPaginationStatus?.canPaginate == true) {
                    viewModel.paginateBackward()
                }
            }
    }

    LazyColumn(Modifier.fillMaxSize(), reverseLayout = true, state = listState) {
        items(timelineItems) { item ->
            Column {
                when (item) {
                    is MatrixTimelineItem.Virtual -> {
                        // TODO
                        when (val virtualItem = item.virtual) {
                            is VirtualTimelineItem.DayDivider -> Text(virtualItem.timestamp.toString())
                            VirtualTimelineItem.LastForwardIndicator -> Text("FWD")
                            is VirtualTimelineItem.LoadingIndicator -> Text("LOADING")
                            VirtualTimelineItem.ReadMarker -> Text("NEW")
                            VirtualTimelineItem.RoomBeginning -> Text("BEGINNING")
                            VirtualTimelineItem.TypingNotification -> Text("TYPING")
                        }
                    }

                    is MatrixTimelineItem.Event -> {
                        when (val content = item.event.content) {
                            is MessageContent -> {
                                when (val contentType = content.type) {
                                    is ImageMessageType -> {
                                        AsyncImage(
                                            MediaRequestData(contentType.source, MediaRequestData.Kind.Thumbnail(1000)),
                                            null,
                                            imageLoader = imageLoader(),
                                            onError = ::onAsyncImageError,
                                        )
                                    }

                                    else -> {} // TODO
                                }
                                Text(content.body) // TODO
                            }

                            else -> {} // TODO
                        }
                        // TODO
                        Text("${item.eventId}: ${item.event.content}")
                    }

                    MatrixTimelineItem.Other -> {
                        // TODO
                        Text("???")
                    }
                }
            }
        }
    }
}
