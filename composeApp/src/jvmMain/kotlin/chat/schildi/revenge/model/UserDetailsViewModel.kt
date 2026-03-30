package chat.schildi.revenge.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import chat.schildi.revenge.UiState
import chat.schildi.revenge.actions.UserIdSuggestion
import chat.schildi.revenge.actions.UserIdSuggestionsProvider
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.core.UserId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class UserDetailsViewModel(
    val sessionId: SessionId,
    val userId: UserId,
) : ViewModel(), UserIdSuggestionsProvider {
    private val client = UiState.selectClient(sessionId, viewModelScope)

    val info = client.map {
        it?.getProfile(userId)?.getOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    override val userIdInRoomSuggestions: Flow<List<UserIdSuggestion>> = info.map { info ->
        listOf(
            UserIdSuggestion(userId, info?.displayName)
        )
    }

    companion object {
        fun factory(
            sessionId: SessionId,
            userId: UserId,
        ) = viewModelFactory {
            initializer {
                UserDetailsViewModel(sessionId, userId)
            }
        }
    }
}
