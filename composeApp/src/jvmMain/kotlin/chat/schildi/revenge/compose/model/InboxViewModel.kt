package chat.schildi.revenge.compose.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chat.schildi.revenge.matrix.MatrixAppState
import co.touchlab.kermit.Logger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class InboxViewModel : ViewModel() {
    private val log = Logger.withTag("Inbox")

    init {
        log.d { "Init" }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val allRooms = MatrixAppState.activeClients.flatMapLatest {
        log.d { "${it.size} clients active" }
        // TODO
        combine(it.map { client -> client.roomListServiceState }) {
            it
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)
}
