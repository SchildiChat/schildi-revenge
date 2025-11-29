package chat.schildi.revenge.compose.destination.inbox

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.compose.search.LocalSearchProvider
import chat.schildi.revenge.model.InboxViewModel

@Composable
fun InboxScreen() {
    val viewModel: InboxViewModel = viewModel()
    CompositionLocalProvider(
        LocalSearchProvider provides viewModel,
    ) {
        val states = viewModel.allStates.collectAsState().value
        val rooms = viewModel.rooms.collectAsState().value
        LazyColumn(Modifier.widthIn(max = Dimens.Inbox.maxWidth).fillMaxSize()) {
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
                    InboxRow(room)
                }
            }
        }
    }
}
