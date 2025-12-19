package chat.schildi.revenge.compose.destination.inbox

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import chat.schildi.preferences.ScPrefs
import chat.schildi.preferences.value
import chat.schildi.revenge.Anim
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.ListAction
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.actions.LocalKeyboardActionProvider
import chat.schildi.revenge.actions.LocalListActionProvider
import chat.schildi.revenge.actions.hierarchicalKeyboardActionProvider
import chat.schildi.revenge.compose.focus.FocusContainer
import chat.schildi.revenge.compose.search.LocalSearchProvider
import chat.schildi.revenge.model.DraftRepo
import chat.schildi.revenge.model.InboxViewModel
import chat.schildi.revenge.model.spaces.filterByUnread
import chat.schildi.revenge.model.spaces.resolveSelection
import chat.schildi.revenge.publishTitle
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

@OptIn(FlowPreview::class)
@Composable
fun InboxScreen(modifier: Modifier = Modifier) {
    val viewModel: InboxViewModel = viewModel(key = LocalDestinationState.current?.id.toString())
    publishTitle(viewModel)

    // Filters that should reset list state
    val searchQuery = LocalKeyboardActionHandler.current.searchQuery.collectAsState("").value
    val accounts = viewModel.accounts.collectAsState().value
    val accountsSorted = viewModel.accountsSorted.collectAsState().value
    val spaceSelection = viewModel.spaceSelection.collectAsState().value
    val spaces = viewModel.spaces.collectAsState().value?.filterByUnread(
        spaceSelection,
        ScPrefs.PSEUDO_SPACE_HIDE_EMPTY_UNREAD.value(),
    )
    val selectedSpace = spaces?.resolveSelection(spaceSelection)

    val listState = key(searchQuery, selectedSpace?.selectionId, accountsSorted) {
        rememberLazyListState()
    }

    val drafts = DraftRepo.roomsWithDrafts.collectAsState(persistentSetOf())
    FocusContainer(
        LocalSearchProvider provides viewModel,
        LocalKeyboardActionProvider provides viewModel.hierarchicalKeyboardActionProvider(),
        LocalListActionProvider provides remember(listState) { ListAction(listState) },
        modifier = modifier,
        role = FocusRole.DESTINATION_ROOT_CONTAINER,
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            InboxTopNavigation()
            val rooms = viewModel.rooms.collectAsState().value
            val roomsByRoomId = viewModel.roomsByRoomId.collectAsState().value
            val dmsByHeroes = viewModel.dmsByHeroes.collectAsState().value
            val needsAccountDisambiguation = (accountsSorted?.count { it.isCurrentlyVisible } ?: 0) > 1

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
                    )
                }
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
            LazyColumn(Modifier.widthIn(max = ScPrefs.MAX_WIDTH_INBOX.value().dp).fillMaxSize(), state = listState) {
                if (!accountsSorted.isNullOrEmpty()) {
                    item {
                        AccountSelectorRow(
                            viewModel = viewModel,
                            accounts = accountsSorted,
                            modifier = Modifier.padding(vertical = Dimens.listPadding),
                        )
                    }
                }
                rooms?.let {
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
                                        room.summary.isOneToOne && (dmsByHeroes[room.summary.info.heroes]?.size ?: 0) > 1
                                )
                        InboxRow(
                            room,
                            hasDraft = room.draftKey in drafts.value,
                            user = if (needsDisambiguation)
                                accounts?.get(room.sessionId)?.user
                            else
                                null
                        )
                    }
                }
            }
        }
    }
}
