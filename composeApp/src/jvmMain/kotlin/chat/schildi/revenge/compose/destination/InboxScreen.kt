package chat.schildi.revenge.compose.destination

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import chat.schildi.revenge.compose.components.clickToNavigate
import chat.schildi.revenge.compose.model.InboxViewModel
import chat.schildi.revenge.navigation.ChatDestination
import chat.schildi.revenge.navigation.toStringHolder

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
