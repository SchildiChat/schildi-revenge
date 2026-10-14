package chat.schildi.revenge.compose.destination

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import chat.schildi.revenge.DateTimeFormat
import chat.schildi.revenge.Destination
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.actions.ActionResult
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.actions.LocalListActionProvider
import chat.schildi.revenge.actions.actionProvider
import chat.schildi.revenge.actions.plainTextCopyActionWithMxcUrl
import chat.schildi.revenge.actions.rememberListActions
import chat.schildi.revenge.compose.components.AvatarImage
import chat.schildi.revenge.compose.components.ContextMenuCallbackEntry
import chat.schildi.revenge.compose.components.ContextMenuDecoration
import chat.schildi.revenge.compose.components.EmptyListScreen
import chat.schildi.revenge.compose.components.TopNavigation
import chat.schildi.revenge.compose.components.TopNavigationCloseOrNavigateToInboxIcon
import chat.schildi.revenge.compose.components.TopNavigationIcon
import chat.schildi.revenge.compose.components.TopNavigationTitle
import chat.schildi.revenge.compose.components.WithContextMenu
import chat.schildi.revenge.compose.focus.FocusContainer
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.revenge.compose.focus.rememberFocusId
import chat.schildi.revenge.model.PushHistoryViewModel
import chat.schildi.revenge.viewModelKey
import chat.schildi.resources.toStringHolder
import chat.schildi.revenge.actions.InteractionAction
import chat.schildi.revenge.model.PushEvent
import chat.schildi.revenge.model.PushEventType
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.api.room.CreateTimelineParams
import io.element.android.libraries.matrix.api.room.RoomInfo
import io.element.android.libraries.matrix.api.user.MatrixUser
import org.jetbrains.compose.resources.pluralStringResource
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.collections.immutable.toPersistentList
import org.jetbrains.compose.resources.stringResource
import shire.res.generated.resources.Res
import shire.res.generated.resources.push_history_empty
import shire.res.generated.resources.push_history_failed_n_times
import shire.res.generated.resources.push_history_filter_title
import shire.res.generated.resources.push_history_pending
import shire.res.generated.resources.push_history_push_received
import shire.res.generated.resources.push_history_resolved
import shire.res.generated.resources.push_history_resolved_with_failures
import shire.res.generated.resources.push_history_title
import shire.res.generated.resources.push_history_worker

@Composable
fun PushHistoryScreen(
    destination: Destination.PushHistory,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
) {
    val viewModel: PushHistoryViewModel = viewModel(
        key = viewModelKey(destination),
        factory = viewModelFactory { initializer { PushHistoryViewModel(destination.sessionId) } },
    )
    val state = viewModel.state.collectAsState().value
    val accounts = viewModel.accounts.collectAsState().value
    val listState = rememberLazyListState()
    val listAction = rememberListActions(listState)
    FocusContainer(
        LocalListActionProvider provides listAction,
        modifier = modifier.windowInsetsPadding(
            WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        ),
        role = FocusRole.DESTINATION_ROOT_CONTAINER,
    ) {
        Column(Modifier.fillMaxSize()) {
            TopNavigation {
                TopNavigationTitle(stringResource(Res.string.push_history_title))
                PushHistoryFilterMenu(viewModel)
                TopNavigationCloseOrNavigateToInboxIcon()
            }
            if (state?.events.isNullOrEmpty()) {
                EmptyListScreen(
                    title = Res.string.push_history_empty.toStringHolder(),
                    icon = rememberVectorPainter(Icons.Default.Notifications),
                    isSearching = false,
                    isLoading = state == null,
                    loadState = viewModel.loadState,
                    modifier = contentModifier.fillMaxSize(),
                )
            } else {
                LazyColumn(
                    modifier = contentModifier,
                    verticalArrangement = Dimens.verticalArrangement,
                    state = listState,
                    contentPadding = WindowInsets.navigationBars
                        .only(WindowInsetsSides.Bottom)
                        .asPaddingValues(),
                ) {
                    itemsIndexed(state.events, key = { _, it -> it.renderKey }) { index, event ->
                        PushHistoryRow(
                            event = event,
                            previous = state.events.getOrNull(index - 1),
                            roomInfo = state.roomInfoByRoomId[event.scopedRoomId],
                            user = remember(accounts, event.sessionId) {
                                accounts?.get(event.sessionId)
                            },
                            needsDisambiguation = viewModel.sessionId == null,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PushHistoryFilterMenu(viewModel: PushHistoryViewModel) {
    val enabledTypes = viewModel.enabledTypes.collectAsState().value
    val keyHandler = LocalKeyboardActionHandler.current
    val destinationState = LocalDestinationState.current
    val focusId = rememberFocusId()
    WithContextMenu(
        focusId = focusId,
        entries = PushEventType.entries.map { type ->
            val checked = enabledTypes.contains(type)
            ContextMenuCallbackEntry(
                title = type.titleRes.toStringHolder(),
                action = {
                    viewModel.toggleType(type)
                    ActionResult.Success()
                },
                decoration = ContextMenuDecoration.Toggle(checked),
                enabled = !checked || enabledTypes.size > 1,
            )
        }.toPersistentList(),
    ) { openContextMenu ->
        TopNavigationIcon(
            Icons.Default.FilterList,
            stringResource(Res.string.push_history_filter_title),
            modifier = Modifier.keyFocusable(
                FocusRole.SHADOW_ITEM,
                id = focusId,
                actionProvider = actionProvider(
                    primaryAction = openContextMenu,
                ),
                addClickListener = false,
            ),
        ) {
            openContextMenu ?: return@TopNavigationIcon
            keyHandler.executeAction(openContextMenu, destinationState)
        }
    }
}

@Composable
private fun PushHistoryRow(
    event: PushEvent,
    previous: PushEvent?,
    roomInfo: RoomInfo?,
    user: MatrixUser?,
    needsDisambiguation: Boolean,
    modifier: Modifier = Modifier,
) {
    val roomName = roomInfo?.name?.takeIf(String::isNotEmpty)
        ?: event.roomId?.value
        ?: event.sessionId.value
    val rowTitle = when (event) {
        is PushEvent.PushNotificationReceived -> stringResource(Res.string.push_history_push_received, roomName)
        is PushEvent.PushWorker -> stringResource(Res.string.push_history_worker, roomName)
        is PushEvent.PushNotificationResolved -> {
            if (event.entity.failureCount > 0) {
                pluralStringResource(
                    Res.plurals.push_history_resolved_with_failures,
                    event.entity.failureCount,
                    event.entity.failureCount
                ).let {
                    event.entity.lastFailure?.let { failure ->
                        it + ": " + failure
                    } ?: it
                }
            } else if (event.entity.lastFailure != null) {
                stringResource(Res.string.push_history_resolved) + ", " + event.entity.lastFailure
            } else {
                stringResource(Res.string.push_history_resolved)
            }
        }
    }
    val canCollapse = event is PushEvent.PushNotificationReceived && event.entity.payload != null
    val collapse = remember { mutableStateOf(canCollapse) }
    val actionProvider = actionProvider(
        copyActions = plainTextCopyActionWithMxcUrl(roomInfo?.avatarUrl) { roomName },
        primaryAction = if (canCollapse) {
            InteractionAction.Invoke {
                collapse.value = !collapse.value
                true
            }
        } else {
            event.roomId?.let { roomId ->
                InteractionAction.Navigate {
                    Destination.Conversation(
                        sessionId = event.sessionId,
                        roomId = roomId,
                        timelineParams = event.eventId?.let {
                            CreateTimelineParams.Focused(it)
                        }
                    )
                }
            }
        },
    )
    Row(
        modifier.keyFocusable(
            FocusRole.LIST_ITEM,
            actionProvider = actionProvider,
        ).padding(
            horizontal = Dimens.windowPadding,
            vertical = Dimens.listPadding,
        ),
        horizontalArrangement = Dimens.horizontalArrangementBig,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(42.dp)) {
            val showAvatar = event.roomId == null || when (event) {
                is PushEvent.PushNotificationReceived -> previous is PushEvent.PushNotificationReceived || previous?.scopedRoomId != event.scopedRoomId
                is PushEvent.PushWorker -> previous !is PushEvent.PushNotificationResolved || previous.scopedRoomId != event.scopedRoomId
                is PushEvent.PushNotificationResolved -> true
            }
            if (showAvatar) {
                AvatarImage(
                    source = if (event.roomId == null) {
                        user?.avatarUrl?.let(::MediaSource)
                    } else {
                        roomInfo?.avatarUrl?.let { MediaSource(it) }
                    },
                    sessionId = event.sessionId,
                    size = 42.dp,
                    shape = Dimens.avatarShape,
                    displayName = roomName,
                )
                if (needsDisambiguation && event.roomId != null) {
                    AvatarImage(
                        source = user?.avatarUrl?.let(::MediaSource),
                        sessionId = event.sessionId,
                        size = 14.dp,
                        shape = CircleShape,
                        displayName = user?.displayName ?: event.sessionId.value,
                        modifier = Modifier.align(Alignment.BottomEnd),
                    )
                }
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                rowTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = when (event) {
                is PushEvent.PushNotificationReceived -> {
                    event.eventId?.value?.takeIf { collapse.value } ?: event.entity.payload ?: "null"
                }
                is PushEvent.PushNotificationResolved -> {
                    if (event.entity.resolvedTimestamp != null) {
                        event.entity.eventId
                    } else if (event.entity.failureCount > 0 || event.entity.lastFailure != null) {
                        pluralStringResource(Res.plurals.push_history_failed_n_times, event.entity.failureCount, event.entity.failureCount) +
                                ": " + event.entity.lastFailure
                    } else {
                        stringResource(Res.string.push_history_pending)
                    }
                }
                is PushEvent.PushWorker -> event.entity.duration.milliseconds.toString()
            }
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (collapse.value) 1 else Int.MAX_VALUE,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            DateTimeFormat.formatTimeOrDateTime(event.timestamp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
