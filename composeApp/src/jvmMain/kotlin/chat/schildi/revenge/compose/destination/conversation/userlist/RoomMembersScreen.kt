package chat.schildi.revenge.compose.destination.conversation.userlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.lifecycle.viewmodel.compose.viewModel
import chat.schildi.revenge.Destination
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.ListAction
import chat.schildi.revenge.actions.LocalListActionProvider
import chat.schildi.revenge.actions.LocalRoomContextSuggestionsProvider
import chat.schildi.revenge.actions.LocalUserIdSuggestionsProvider
import chat.schildi.revenge.compose.components.EmptyListScreen
import chat.schildi.revenge.compose.focus.FocusContainer
import chat.schildi.revenge.compose.search.LocalSearchProvider
import chat.schildi.revenge.compose.util.toStringHolder
import chat.schildi.revenge.model.userlist.RoomMemberListViewModel
import chat.schildi.revenge.publishTitle
import chat.schildi.revenge.viewModelKey
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.empty_screen_placeholder_room_members
import shire.composeapp.generated.resources.room_members_title_loaded
import shire.composeapp.generated.resources.room_members_title_loading

@Composable
fun RoomMembersScreen(
    destination: Destination.RoomMembers,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
) {
    val viewModel: RoomMemberListViewModel =
        viewModel(
            key = viewModelKey(destination),
            factory = RoomMemberListViewModel.factory(destination.sessionId, destination.roomId),
        )
    publishTitle(viewModel)

    val listState = rememberLazyListState()
    val listAction = remember(listState) { ListAction(listState) }
    FocusContainer(
        LocalSearchProvider provides viewModel,
        LocalUserIdSuggestionsProvider provides viewModel,
        LocalRoomContextSuggestionsProvider provides viewModel.roomContextSuggestionsProvider,
        LocalListActionProvider provides listAction,
        role = FocusRole.DESTINATION_ROOT_CONTAINER,
        modifier = modifier.fillMaxSize(),
    ) {
        val membersState = viewModel.entries.collectAsState(null).value
        val members = membersState?.items
        Column(contentModifier.fillMaxSize()) {
            ConversationDetailsTopNavigation(
                if (members == null)
                    stringResource(Res.string.room_members_title_loading)
                else
                    pluralStringResource(Res.plurals.room_members_title_loaded, members.size, members.size)
            )
            if (members.isNullOrEmpty()) {
                EmptyListScreen(
                    title = Res.string.empty_screen_placeholder_room_members.toStringHolder(),
                    icon = rememberVectorPainter(Icons.Default.Groups),
                    renderedSearchTerm = membersState?.searchTerm,
                    isLoading = members == null,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            } else {
                LazyColumn(
                    Modifier.fillMaxWidth().weight(1f),
                    state = listState,
                ) {
                    items(
                        members,
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
