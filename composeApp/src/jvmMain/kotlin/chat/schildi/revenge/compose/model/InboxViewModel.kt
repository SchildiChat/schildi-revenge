package chat.schildi.revenge.compose.model

import androidx.lifecycle.ViewModel
import chat.schildi.revenge.matrix.MatrixAppState
import co.touchlab.kermit.Logger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

class InboxViewModel : ViewModel() {
    private val log = Logger.withTag("Inbox")

    @OptIn(ExperimentalCoroutinesApi::class)
    val allRooms = MatrixAppState.activeClients.flatMapLatest {
        log.d { "${it.size} clients active" }
        // TODO
        combine(it.map { client -> client.roomListServiceState }) {
            it
        }
    }
}
