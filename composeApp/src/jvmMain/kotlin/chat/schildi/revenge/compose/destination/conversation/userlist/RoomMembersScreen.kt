package chat.schildi.revenge.compose.destination.conversation.userlist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import chat.schildi.preferences.ScPrefs
import chat.schildi.preferences.value
import chat.schildi.revenge.Destination
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.ListAction
import chat.schildi.revenge.actions.LocalListActionProvider
import chat.schildi.revenge.actions.LocalRoomContextSuggestionsProvider
import chat.schildi.revenge.actions.LocalUserIdSuggestionsProvider
import chat.schildi.revenge.compose.destination.SplashScreenContent
import chat.schildi.revenge.compose.focus.FocusContainer
import chat.schildi.revenge.compose.search.LocalSearchProvider
import chat.schildi.revenge.model.userlist.RoomMemberListViewModel
import chat.schildi.revenge.publishTitle
import chat.schildi.revenge.viewModelKey

@Composable
fun RoomMembersScreen(destination: Destination.RoomMembers, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        val viewModel: RoomMemberListViewModel = viewModel(
            key = viewModelKey(destination),
            factory = RoomMemberListViewModel.factory(destination.sessionId, destination.roomId)
        )
        publishTitle(viewModel)

        val members = viewModel.entries.collectAsState(null)
        if (members.value.isNullOrEmpty()) {
            SplashScreenContent()
            return@Box
        }

        val listState = rememberLazyListState()
        val listAction = remember(listState) { ListAction(listState) }
        FocusContainer(
            LocalSearchProvider provides viewModel,
            LocalUserIdSuggestionsProvider provides viewModel,
            LocalRoomContextSuggestionsProvider provides viewModel.roomContextSuggestionsProvider,
            LocalListActionProvider provides listAction,
            role = FocusRole.DESTINATION_ROOT_CONTAINER,
        ) {
            Column(Modifier
                .widthIn(max = ScPrefs.MAX_WIDTH_ROOM_DETAILS.value().dp)
                .fillMaxSize()
            ) {
                LazyColumn(
                    Modifier.fillMaxWidth().weight(1f),
                    state = listState,
                ) {
                    items(
                        members.value ?: emptyList(),
                        key = { item ->
                            item.userId
                        },
                    ) { roomMember ->
                        RoomMemberRow(
                            roomMember = roomMember,
                            viewModel = viewModel,
                        )
                    }
                }
            }
        }
    }
}
