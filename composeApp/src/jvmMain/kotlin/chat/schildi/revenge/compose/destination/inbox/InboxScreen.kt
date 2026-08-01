package chat.schildi.revenge.compose.destination.inbox

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import chat.schildi.lib.preferences.ScPrefs
import chat.schildi.revenge.preferences.value
import chat.schildi.revenge.Anim
import chat.schildi.revenge.Destination
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.ListActions
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.actions.LocalKeyboardActionProvider
import chat.schildi.revenge.actions.LocalListActionProvider
import chat.schildi.revenge.actions.hierarchicalKeyboardActionProvider
import chat.schildi.revenge.compose.components.DiagnosticsRow
import chat.schildi.revenge.compose.components.EmptyListScreen
import chat.schildi.revenge.compose.focus.FocusContainer
import chat.schildi.revenge.compose.search.LocalSearchProvider
import chat.schildi.resources.toStringHolder
import chat.schildi.revenge.actions.InteractionAction
import chat.schildi.revenge.actions.actionProvider
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.revenge.model.DraftRepo
import chat.schildi.revenge.model.InboxViewModel
import chat.schildi.revenge.model.spaces.PSEUDO_SPACE_ID_NO_FILTER
import chat.schildi.revenge.model.spaces.SpaceListDataSource
import chat.schildi.revenge.model.spaces.filterByVisible
import chat.schildi.revenge.model.spaces.resolveSelection
import chat.schildi.revenge.publishTitle
import chat.schildi.revenge.viewModelKey
import chat.schildi.theme.scExposures
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import org.jetbrains.compose.resources.stringResource
import shire.res.generated.resources.Res
import shire.res.generated.resources.action_account_management
import shire.res.generated.resources.empty_screen_placeholder_inbox
import shire.res.generated.resources.empty_screen_placeholder_space

@OptIn(FlowPreview::class)
@Composable
fun InboxScreen(
    destination: Destination.Inbox,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
) {
    val viewModel: InboxViewModel = viewModel(
        key = viewModelKey(destination),
        factory = viewModelFactory { initializer { InboxViewModel() } }
    )
    publishTitle(viewModel)

    // Filters that should reset list state
    val searchQuery = LocalKeyboardActionHandler.current.searchQueryForDestination(viewModel)
        .collectAsState("").value
    val accounts = viewModel.accounts.collectAsState().value
    val accountsSorted = viewModel.accountsSorted.collectAsState().value
    val accountUnreadCounts = viewModel.accountUnreadCounts.collectAsState().value
    val spaceSelection = viewModel.spaceSelection.collectAsState().value
    val spaces = viewModel.spaces.collectAsState().value?.filterByVisible(
        spaceSelection,
        ScPrefs.PSEUDO_SPACE_HIDE_EMPTY_UNREAD.value(),
    )
    val selectedSpace = if (searchQuery.isNullOrBlank()) {
        spaces?.resolveSelection(spaceSelection, followWildcards = true)
    } else {
        // Search ignores spaces
        null
    }

    val listState = key(searchQuery, selectedSpace?.selectionId) {
        rememberLazyListState()
    }

    val drafts = DraftRepo.roomsWithDrafts.collectAsState(persistentSetOf())
    FocusContainer(
        LocalSearchProvider provides viewModel,
        LocalKeyboardActionProvider provides viewModel.hierarchicalKeyboardActionProvider(),
        LocalListActionProvider provides remember(listState) { ListActions(listState) },
        modifier = modifier.windowInsetsPadding(
            WindowInsets.safeContent.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        ),
        role = FocusRole.DESTINATION_ROOT_CONTAINER,
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            InboxTopNavigation(viewModel.windowTitle.collectAsState(null).value?.render())
            val roomsState = viewModel.filteredRooms.collectAsState().value
            val rooms = roomsState?.rooms
            val roomsByRoomId = viewModel.roomsByRoomId.collectAsState().value
            val dmsByHeroes = viewModel.dmsByHeroes.collectAsState().value
            val needsAccountDisambiguation = (accountsSorted?.count { it.isCurrentlyVisible } ?: 0) > 1

            val spaceSwipeState = rememberSpaceSwipeState()
            val spaceSelectionState = spaces.toSelectionState(spaceSelection)

            if (spaces != null) {
                AnimatedVisibility(
                    visible = viewModel.showSpaceUi.collectAsState().value,
                    enter = slideInVertically(tween(Anim.DURATION)) { -it } +
                            expandVertically(tween(Anim.DURATION), expandFrom = Alignment.Top),
                    exit = slideOutVertically(tween(Anim.DURATION)) { -it } +
                            shrinkVertically(tween(Anim.DURATION), shrinkTowards = Alignment.Top),
                ) {
                    SpaceSelectorRow(
                        lazyListState = listState,
                        spacesList = spaces,
                        totalUnreadCounts = null, // TODO
                        spaceSelectionHierarchy = spaceSelection,
                        onSpaceSelected = viewModel::setSpaceSelection,
                        modifier = Modifier,
                        getSpaceActionProvider = viewModel::getKeyboardActionProviderForSpace,
                    )
                }
            }

            if (ScPrefs.SHOW_DEV_INFOS.value()) {
                DiagnosticsRow(Modifier.fillMaxWidth())
            }

            // Observe which rooms are visible in the list so subscribe to room list updates
            LaunchedEffect(listState, rooms, accountsSorted) {
                snapshotFlow {
                    // Adjust for header account selector offset
                    val roomsOffset = if (!accountsSorted.isNullOrEmpty()) 1 else 0
                    listState.layoutInfo.visibleItemsInfo.map { it.index - roomsOffset }
                }
                    .debounce(200)
                    .collect { indices ->
                        val visibleRooms = indices.mapNotNull { rooms?.getOrNull(it) }
                        viewModel.onVisibleRoomsChanged(visibleRooms)
                    }
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .spaceSwipe(
                        swipeState = spaceSwipeState,
                        selectionState = spaceSelectionState,
                    ) { space ->
                        viewModel.setSpaceSelection(
                            if (spaceSelection.isEmpty()) {
                                listOf(space?.selectionId ?: PSEUDO_SPACE_ID_NO_FILTER)
                            } else {
                                val parentId = spaceSelection.getOrNull(spaceSelection.size - 2)
                                spaceSelection.take(spaceSelection.size - 1) +
                                        (space?.selectionId?.takeIf { it != parentId } ?: PSEUDO_SPACE_ID_NO_FILTER)
                            }
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (rooms.isNullOrEmpty()) {
                    Column(contentModifier.fillMaxSize()) {
                        if (!accountsSorted.isNullOrEmpty()) {
                            AccountSelectorRow(
                                viewModel = viewModel,
                                accounts = accountsSorted,
                                unreadCounts = accountUnreadCounts,
                                modifier = Modifier.padding(vertical = Dimens.listPadding),
                            )
                        }
                        EmptyListScreen(
                            title = if (selectedSpace == null) {
                                Res.string.empty_screen_placeholder_inbox.toStringHolder()
                            } else {
                                Res.string.empty_screen_placeholder_space.toStringHolder()
                            },
                            icon = if (selectedSpace == null) {
                                rememberVectorPainter(Icons.Default.Inbox)
                            } else {
                                val pseudoSpaceIcon = (selectedSpace as? SpaceListDataSource.PseudoSpaceItem)
                                    ?.icon as? SpaceListDataSource.PseudoSpaceIconSource.Icon
                                if (pseudoSpaceIcon != null) {
                                    rememberVectorPainter(pseudoSpaceIcon.icon)
                                } else {
                                    rememberVectorPainter(Icons.Default.TravelExplore)
                                }
                            },
                            renderedSearchTerm = roomsState?.searchTerm,
                            isLoading = roomsState == null,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            if (accounts != null && accounts.isEmpty()) {
                                Text(
                                    stringResource(Res.string.action_account_management),
                                    color = MaterialTheme.scExposures.accentColor,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier
                                        .padding(Dimens.listPadding)
                                        .keyFocusable(
                                            role = FocusRole.AUX_ITEM,
                                            actionProvider = actionProvider(
                                                primaryAction = InteractionAction.Navigate {
                                                    Destination.AccountManagement()
                                                }
                                            ),
                                        ),
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = contentModifier.fillMaxSize(),
                        state = listState,
                        contentPadding = WindowInsets.navigationBars
                            .only(WindowInsetsSides.Bottom)
                            .asPaddingValues(),
                    ) {
                        if (!accountsSorted.isNullOrEmpty()) {
                            item {
                                AccountSelectorRow(
                                    viewModel = viewModel,
                                    accounts = accountsSorted,
                                    unreadCounts = accountUnreadCounts,
                                    modifier = Modifier.padding(vertical = Dimens.listPadding),
                                )
                            }
                        }
                        items(
                            rooms,
                            key = { room ->
                                Pair(room.sessionId, room.summary.roomId)
                            }
                        ) { room ->
                            val needsDisambiguation = needsAccountDisambiguation &&
                                    selectedSpace?.sessionIds.let { it == null || it.size > 1 } &&
                                    (
                                            room.summary.isInvite() ||
                                                    (roomsByRoomId[room.summary.roomId]?.size ?: 0) > 1 ||
                                                    room.summary.isDm && (dmsByHeroes[room.summary.info.heroes]?.size
                                                ?: 0) > 1
                                            )
                            InboxRow(
                                viewModel,
                                room,
                                hasDraft = room.key in drafts.value,
                                user = remember(accounts) { accounts?.get(room.sessionId)?.user },
                                needsAccountDisambiguation = needsDisambiguation,
                            )
                        }
                    }
                }
                SpaceSwipeIndicatorOverlay(
                    swipeState = spaceSwipeState,
                    selectionState = spaceSelectionState,
                )
            }
        }
    }
}
