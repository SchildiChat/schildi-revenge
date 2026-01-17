package chat.schildi.revenge.compose.destination.conversation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import chat.schildi.preferences.ScPrefs
import chat.schildi.preferences.value
import chat.schildi.revenge.Anim
import chat.schildi.revenge.model.conversation.ConversationViewModel
import chat.schildi.revenge.Destination
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.LocalMatrixBodyDrawStyle
import chat.schildi.revenge.LocalMatrixBodyFormatter
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.ListAction
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.actions.LocalKeyboardActionProvider
import chat.schildi.revenge.actions.LocalListActionProvider
import chat.schildi.revenge.actions.LocalRoomContextSuggestionsProvider
import chat.schildi.revenge.actions.LocalUserIdSuggestionsProvider
import chat.schildi.revenge.actions.currentActionContext
import chat.schildi.revenge.actions.hierarchicalKeyboardActionProvider
import chat.schildi.revenge.compose.composer.ComposerRow
import chat.schildi.revenge.compose.destination.SplashScreenContent
import chat.schildi.revenge.compose.destination.conversation.event.EventHighlight
import chat.schildi.revenge.compose.destination.conversation.event.message.LocalUrlPreviewStateProvider
import chat.schildi.revenge.compose.focus.FocusContainer
import chat.schildi.revenge.compose.search.LocalSearchProvider
import chat.schildi.revenge.matrixBodyDrawStyle
import chat.schildi.revenge.matrixBodyFormatter
import chat.schildi.revenge.model.conversation.EventJumpTarget
import chat.schildi.revenge.publishTitle
import co.touchlab.kermit.Logger
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.api.timeline.MatrixTimelineItem
import io.element.android.libraries.matrix.api.timeline.item.event.EventOrTransactionId
import kotlinx.collections.immutable.persistentListOf

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun ConversationScreen(destination: Destination.Conversation, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val contentHeight = maxHeight
        val density = LocalDensity.current

        val keyHandler = LocalKeyboardActionHandler.current
        val viewModel: ConversationViewModel = viewModel(
            key = "${LocalDestinationState.current?.id}/${destination.sessionId}/${destination.roomId}",
            factory = ConversationViewModel.factory(destination.sessionId, destination.roomId)
        )
        val timelineItems = viewModel.timelineItems.collectAsState().value
        val forwardPaginationStatus = viewModel.forwardPaginationStatus.collectAsState(null).value
        val backwardPaginationStatus = viewModel.backwardPaginationStatus.collectAsState(null).value
        val timestampSettings = viewModel.timestampSettings.collectAsState().value

        if (timelineItems == null) {
            SplashScreenContent()
            return@BoxWithConstraints
        }

        val actionContext = currentActionContext()
        var isDragging by remember { mutableStateOf(false) }
        val dragAlpha = animateFloatAsState(if (isDragging) 0.4f else 1f)
        val fileDragTarget = remember {
            object : DragAndDropTarget {
                override fun onEntered(event: DragAndDropEvent) {
                    Logger.withTag("DnD").d { "Entered $event" }
                    isDragging = true
                }
                override fun onExited(event: DragAndDropEvent) {
                    Logger.withTag("DnD").d { "Exited $event" }
                    isDragging = false
                }
                override fun onDrop(event: DragAndDropEvent): Boolean {
                    isDragging = false
                    val file = (event.dragData() as? DragData.FilesList)?.readFiles()?.firstOrNull()
                    Logger.withTag("DnD").d { "Received drop $event, file ${file != null}" }
                    return if (file != null) {
                        viewModel.attachFile(actionContext, file)
                    } else {
                        false
                    }
                }
            }
        }

        var initialListOffset by remember { mutableStateOf(Triple(0, 0, -1)) }
        var scrolledToEvent by remember { mutableStateOf<EventJumpTarget?>(null) }
        val targetEvent = viewModel.targetEvent.collectAsState().value
        LaunchedEffect(targetEvent, timelineItems) {
            if (targetEvent == scrolledToEvent || timelineItems.isEmpty()) {
                return@LaunchedEffect
            }
            if (targetEvent == null) {
                // Ignore / keep last
                return@LaunchedEffect
            }
            val index = when(targetEvent) {
                is EventJumpTarget.Event -> timelineItems.indexOfFirst { item ->
                    (item.item as? MatrixTimelineItem.Event)?.eventId == targetEvent.eventId
                }.let {
                    if (it >= 0) {
                        // Reverse list not applied here yet
                        timelineItems.size - it - 1
                    } else {
                        null
                    }
                }
                is EventJumpTarget.Index -> targetEvent.index.takeIf {
                    forwardPaginationStatus?.hasMoreToLoad == false && it in 0..<timelineItems.size
                }
            }
            if (index == null) {
                Logger.withTag("ConversationScreen").w("Cannot find target event $targetEvent in ${timelineItems.size} items")
            } else {
                val offset = density.run { contentHeight.roundToPx() } / 2
                initialListOffset = Triple(index, -offset, targetEvent.renavigationCount)
                scrolledToEvent = targetEvent
            }
        }

        val listState = key(initialListOffset) {
            rememberLazyListState(
                initialFirstVisibleItemIndex = initialListOffset.first,
                initialFirstVisibleItemScrollOffset = initialListOffset.second,
            )
        }

        publishTitle(viewModel)

        LaunchedEffect(listState, backwardPaginationStatus, timelineItems) {
            snapshotFlow {
                listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
            }
                .collect { lastVisibleIndex ->
                    if (lastVisibleIndex != null && lastVisibleIndex >= timelineItems.size - 3 && backwardPaginationStatus?.canPaginate == true) {
                        viewModel.paginateBackward()
                    }
                }
        }
        LaunchedEffect(listState, forwardPaginationStatus, timelineItems) {
            snapshotFlow {
                listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index
            }
                .collect { firstVisibleIndex ->
                    if (firstVisibleIndex != null && firstVisibleIndex <= 3 && forwardPaginationStatus?.canPaginate == true) {
                        viewModel.paginateForward()
                    }
                }
        }

        val listAction = remember(listState) { ListAction(listState, isReverseList = true) }
        FocusContainer(
            LocalSearchProvider provides viewModel,
            LocalKeyboardActionProvider provides
                    viewModel.actionProvider.hierarchicalKeyboardActionProvider(),
            LocalUrlPreviewStateProvider provides viewModel.urlPreviewStateProvider.collectAsState().value,
            LocalUserIdSuggestionsProvider provides viewModel,
            LocalRoomContextSuggestionsProvider provides viewModel.roomContextSuggestionsProvider,
            LocalListActionProvider provides listAction,
            LocalMatrixBodyFormatter provides matrixBodyFormatter(),
            LocalMatrixBodyDrawStyle provides matrixBodyDrawStyle(),
            role = FocusRole.DESTINATION_ROOT_CONTAINER,
        ) {
            val roomMembersById = viewModel.roomMembersById.collectAsState()
            val highlightedJumpTargetEventId = (targetEvent as? EventJumpTarget.Event)?.let {
                if (it.highlight) {
                    it.eventId
                } else {
                    null
                }
            }
            val highlightedActionEventId = viewModel.highlightedActionEventId.collectAsState().value
            Column(Modifier
                .fillMaxSize()
                .widthIn(max = ScPrefs.MAX_WIDTH_CONVERSATION.value().dp)
                .alpha(dragAlpha.value)
                .dragAndDropTarget(
                    shouldStartDragAndDrop = { event ->
                        event.dragData() is DragData.FilesList
                    },
                    target = fileDragTarget,
                ),
            ) {
                val roomInfo = viewModel.roomInfo.collectAsState(null).value
                ConversationTopNavigation(
                    roomInfo?.name ?: "",
                    roomInfo?.avatarUrl?.let { MediaSource(it) },
                )
                // Double reverse helps with stick-to-bottom while paging backwards or receiving messages
                LazyColumn(
                    Modifier.fillMaxWidth().weight(1f),
                    reverseLayout = true,
                    state = listState,
                ) {
                    val renderedItems = timelineItems.reversed()
                    itemsIndexed(
                        renderedItems,
                        key = { index, item ->
                            when (item.item) {
                                is MatrixTimelineItem.Event -> item.item.eventId ?: item.item.transactionId ?: index
                                MatrixTimelineItem.Other -> index
                                is MatrixTimelineItem.Virtual -> item.item.uniqueId
                            }
                        },
                    ) { index, item ->
                        // Reversed list, let's not confuse us too much and still say "previous = older"
                        val next = renderedItems.getOrNull(index - 1)
                        val previous = renderedItems.getOrNull(index + 1)
                        val highlight = when {
                            item.item !is MatrixTimelineItem.Event -> EventHighlight.NONE
                            highlightedActionEventId is EventOrTransactionId.Event &&
                                    item.item.eventId == highlightedActionEventId.eventId -> EventHighlight.ACTION_TARGET
                            highlightedActionEventId is EventOrTransactionId.Transaction &&
                                    item.item.transactionId == highlightedActionEventId.id -> EventHighlight.ACTION_TARGET
                            highlightedJumpTargetEventId != null && item.item.eventId == highlightedJumpTargetEventId -> EventHighlight.JUMP_TARGET
                            else -> EventHighlight.NONE
                        }
                        ConversationItemRow(
                            viewModel = viewModel,
                            item = item,
                            next = next,
                            previous = previous,
                            roomMembersById = roomMembersById.value,
                            highlight = highlight,
                            timestampSettings = timestampSettings,
                        )
                    }
                }
                val identityStateViolations = viewModel.identityStateViolations.collectAsState(null).value ?: persistentListOf()
                IdentityStateChangesRow(
                    identityStateViolations,
                    roomMembersById.value,
                    acknowledge = { viewModel.acknowledgeIdentityStateChange(actionContext, it) },
                )
                val shouldShowComposer = viewModel.shouldShowComposer.collectAsState().value
                LaunchedEffect(shouldShowComposer) {
                    if (shouldShowComposer) {
                        keyHandler.focusByRole(FocusRole.MESSAGE_COMPOSER)
                    }
                }
                AnimatedVisibility(
                    visible = shouldShowComposer,
                    enter = slideInVertically(tween(Anim.DURATION)) { it } +
                            expandVertically(tween(Anim.DURATION), expandFrom = Alignment.Bottom),
                    exit = slideOutVertically(tween(Anim.DURATION)) { it } +
                            shrinkVertically(tween(Anim.DURATION), shrinkTowards = Alignment.Bottom),
                ) {
                    ComposerRow(viewModel)
                }
            }
        }
    }
}
