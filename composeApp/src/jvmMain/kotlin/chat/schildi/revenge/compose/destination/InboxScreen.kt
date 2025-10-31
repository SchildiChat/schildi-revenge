package chat.schildi.revenge.compose.destination

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import chat.schildi.revenge.compose.model.InboxViewModel

@Composable
fun InboxScreen() {
    val viewModel: InboxViewModel = viewModel()
    val rooms = viewModel.allRooms.collectAsState(null).value
    Text("$rooms")
    rooms?.let {
        LazyColumn {
            items(rooms) { room ->
                Text(
                    room.toString(),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
