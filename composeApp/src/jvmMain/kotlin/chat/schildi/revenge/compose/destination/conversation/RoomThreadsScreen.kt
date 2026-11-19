package chat.schildi.revenge.compose.destination.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import chat.schildi.revenge.DateTimeFormat
import chat.schildi.revenge.Destination
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.InteractionAction
import chat.schildi.revenge.actions.LocalKeyboardActionProvider
import chat.schildi.revenge.actions.LocalListActionProvider
import chat.schildi.revenge.actions.LocalRoomContextSuggestionsProvider
import chat.schildi.revenge.actions.actionProvider
import chat.schildi.revenge.actions.hierarchicalKeyboardActionProvider
import chat.schildi.revenge.actions.rememberListActions
import chat.schildi.revenge.compose.components.AvatarImage
import chat.schildi.revenge.compose.components.EmptyListScreen
import chat.schildi.revenge.compose.components.SelectionContainerUnlessTouch
import chat.schildi.revenge.compose.destination.conversation.userlist.ConversationDetailsTopNavigation
import chat.schildi.revenge.compose.destination.conversation.virtual.PagingIndicator
import chat.schildi.revenge.compose.focus.FocusContainer
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.revenge.compose.search.LocalSearchProvider
import chat.schildi.revenge.model.RoomThreadsViewModel
import chat.schildi.revenge.plaintext.EventTextFormat
import chat.schildi.revenge.publishTitle
import chat.schildi.revenge.viewModelKey
import chat.schildi.resources.toStringHolder
import chat.schildi.revenge.model.ScThreadListItem
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.api.room.CreateTimelineParams
import io.element.android.libraries.matrix.api.room.threads.ThreadListItem
import io.element.android.libraries.matrix.api.room.threads.ThreadListPaginationStatus
import io.element.android.libraries.matrix.api.timeline.item.event.getAvatarUrl
import io.element.android.libraries.matrix.api.timeline.item.event.getDisambiguatedDisplayName
import io.element.android.libraries.matrix.api.timeline.item.event.getDisplayName
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import shire.res.generated.resources.Res
import shire.res.generated.resources.empty_screen_placeholder_threads
import shire.res.generated.resources.n_threaded_replies
import shire.res.generated.resources.threads_title

@Composable
fun RoomThreadsScreen(
    destination: Destination.RoomThreads,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
) {
    val viewModel: RoomThreadsViewModel =
        viewModel(
            key = viewModelKey(destination),
            factory = RoomThreadsViewModel.factory(destination.sessionId, destination.roomId),
        )
    publishTitle(viewModel)

    val threadsState = viewModel.state.collectAsState(null).value
    val paginationStatus = viewModel.paginationStatus.collectAsState(null).value
    val isLoading = viewModel.isLoading.collectAsState().value
    val listState = key(threadsState?.searchTerm) {
        rememberLazyListState()
    }
    FocusContainer(
        LocalSearchProvider provides viewModel,
        LocalRoomContextSuggestionsProvider provides viewModel.roomContextSuggestionsProvider,
        LocalKeyboardActionProvider provides viewModel.actionProvider.hierarchicalKeyboardActionProvider(),
        LocalListActionProvider provides rememberListActions(listState),
        role = FocusRole.DESTINATION_ROOT_CONTAINER,
        modifier = modifier.windowInsetsPadding(
            WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        ).fillMaxSize(),
    ) {
        Column(contentModifier.fillMaxSize()) {
            ConversationDetailsTopNavigation(stringResource(Res.string.threads_title))
            if (threadsState?.items.isNullOrEmpty()) {
                EmptyListScreen(
                    title = Res.string.empty_screen_placeholder_threads.toStringHolder(),
                    icon = rememberVectorPainter(Icons.Default.Forum),
                    renderedSearchTerm = threadsState?.searchTerm,
                    isLoading = isLoading,
                    loadState = viewModel.loadState,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            } else {
                LazyColumn(
                    Modifier.fillMaxWidth().weight(1f),
                    state = listState,
                    contentPadding = WindowInsets.navigationBars
                        .only(WindowInsetsSides.Bottom)
                        .asPaddingValues(),
                ) {
                    items(
                        threadsState.items,
                        key = { item -> item.item.threadId.value },
                    ) { item ->
                        ThreadListItemRow(
                            item = item,
                            sessionId = destination.sessionId,
                            roomId = destination.roomId,
                        )
                    }
                    val showPagingIndicator = when (paginationStatus) {
                        is ThreadListPaginationStatus.Idle -> paginationStatus.hasMoreToLoad
                        ThreadListPaginationStatus.Loading -> true
                        null -> false
                    }
                    if (showPagingIndicator) {
                        item {
                            PagingIndicator(
                                Modifier.padding(vertical = Dimens.windowPadding),
                            )
                        }
                    }
                }
            }
        }
        LaunchedEffect(listState, threadsState?.items, paginationStatus) {
            val items = threadsState?.items ?: return@LaunchedEffect
            if ((paginationStatus as? ThreadListPaginationStatus.Idle)?.hasMoreToLoad != true) {
                return@LaunchedEffect
            }
            snapshotFlow {
                listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
            }.collect { lastVisibleIndex ->
                if (lastVisibleIndex != null && lastVisibleIndex >= items.size - 3) {
                    viewModel.loadMore()
                }
            }
        }
    }
}

@Composable
private fun ThreadListItemRow(
    item: ScThreadListItem,
    sessionId: SessionId,
    roomId: RoomId,
    modifier: Modifier = Modifier,
) {
    val root = item.item.rootEvent
    val latest = item.item.latestEvent?.takeIf { it.eventId != root.eventId }
    Row(
        modifier
            .keyFocusable(
                FocusRole.LIST_ITEM,
                actionProvider = actionProvider(
                    primaryAction = InteractionAction.Navigate {
                        Destination.Conversation(
                            sessionId = sessionId,
                            roomId = roomId,
                            timelineParams = CreateTimelineParams.Threaded(item.item.threadId),
                        )
                    },
                ),
            )
            .padding(
                horizontal = Dimens.windowPadding,
                vertical = Dimens.listPadding,
            ),
        horizontalArrangement = Arrangement.spacedBy(Dimens.Inbox.avatarItemPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarImage(
            source = root.senderProfile.getAvatarUrl()?.let { MediaSource(it) },
            size = Dimens.Inbox.avatar,
            displayName = root.senderProfile.getDisplayName() ?: root.senderId.value,
            contentDescription = root.senderProfile.getDisambiguatedDisplayName(root.senderId),
        )
        SelectionContainerUnlessTouch(Modifier.weight(1f)) {
            Column(
                verticalArrangement = Arrangement.spacedBy(
                    2.dp,
                    Alignment.CenterVertically,
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Dimens.horizontalArrangement,
                ) {
                    Text(
                        root.senderProfile.getDisplayName() ?: root.senderId.value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        DateTimeFormat.formatTimestampAsDateOrTime(latest?.timestamp ?: root.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                root.content?.let { content ->
                    Text(
                        EventTextFormat.eventToText(
                            content = content,
                            messageMetadata = item.rootEventMessageMetadata,
                            senderProfile = root.senderProfile,
                            senderId = root.senderId,
                            stripNewlines = true,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    horizontalArrangement = Dimens.horizontalArrangementSmall,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (item.item.numberOfReplies > 0) {
                        Text(
                            item.item.numberOfReplies.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Icon(
                            imageVector = Icons.Default.Forum,
                            contentDescription = pluralStringResource(
                                Res.plurals.n_threaded_replies,
                                item.item.numberOfReplies.toInt(),
                                item.item.numberOfReplies
                            ),
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (latest != null) {
                        latest.senderProfile.getAvatarUrl()?.let {
                            AvatarImage(
                                source = MediaSource(it),
                                size = Dimens.Conversation.receiptSize,
                                displayName = latest.senderProfile.getDisplayName() ?: latest.senderId.value,
                                contentDescription = latest.senderProfile.getDisambiguatedDisplayName(latest.senderId),
                            )
                        }
                        Text(
                            latest.senderProfile.getDisplayName() ?: latest.senderId.value,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        latest.content?.let { content ->
                            Text(
                                EventTextFormat.eventToText(
                                    content = content,
                                    messageMetadata = item.latestEventMessageMetadata,
                                    senderProfile = latest.senderProfile,
                                    senderId = latest.senderId,
                                    stripNewlines = true,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}
