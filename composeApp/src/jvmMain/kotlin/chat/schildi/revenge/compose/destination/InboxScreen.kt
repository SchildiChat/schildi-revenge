package chat.schildi.revenge.compose.destination

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import chat.schildi.revenge.UiState
import chat.schildi.revenge.compose.components.clickToNavigate
import chat.schildi.revenge.compose.media.imageLoader
import chat.schildi.revenge.compose.model.InboxViewModel
import chat.schildi.revenge.navigation.ChatDestination
import chat.schildi.revenge.navigation.toStringHolder
import coil3.compose.AsyncImage
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.ui.media.MediaRequestData

@Composable
fun InboxScreen() {
    val viewModel: InboxViewModel = viewModel()
    val states = viewModel.allStates.collectAsState().value
    val rooms = viewModel.allRooms.collectAsState().value
    LazyColumn(Modifier.fillMaxSize()) {
        states?.let {
            items(states) { state ->
                Text(
                    state.toString(),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        rooms?.let {
            items(rooms) { room ->
                val roomName = room.summary.info.name ?: room.summary.info.id.value
                Row {
                    room.summary.info.avatarUrl?.let { avatarUrl ->
                        AsyncImage(
                            model = MediaRequestData(MediaSource(avatarUrl), MediaRequestData.Kind.Content),
                            contentDescription = null,
                            // TODO that image loader approach needs to be revised, probably should at least hashmap the clients for session ids
                            imageLoader = imageLoader(UiState.matrixClients.collectAsState().value.find { it.sessionId == room.sessionId }),
                        )
                    }
                    Text(
                        "$roomName [${room.sessionId.value}]",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clickToNavigate(
                            initialTitle = room.summary.info.name?.toStringHolder()
                        ) {
                            ChatDestination(room.sessionId, room.summary.roomId)
                        }
                    )
                }
            }
        }
    }
}
