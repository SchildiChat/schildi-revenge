package chat.schildi.revenge.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import chat.schildi.preferences.RevengePrefs
import chat.schildi.revenge.TitleProvider
import chat.schildi.revenge.UiState
import chat.schildi.revenge.compose.util.ComposableStringHolder
import chat.schildi.revenge.Destination
import chat.schildi.revenge.actions.KeyboardActionProvider
import chat.schildi.revenge.compose.util.toStringHolder
import chat.schildi.revenge.config.keybindings.Action
import chat.schildi.revenge.config.keybindings.KeyTrigger
import co.touchlab.kermit.Logger
import io.element.android.features.messages.impl.timeline.TimelineController
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.timeline.ReceiptType
import io.element.android.libraries.matrix.api.timeline.Timeline
import io.element.android.x.di.AppGraph
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationViewModel(
    private val sessionId: SessionId,
    private val roomId: RoomId,
    private val appGraph: AppGraph = UiState.appGraph,
) : ViewModel(), TitleProvider, KeyboardActionProvider {
    private val log = Logger.withTag("ChatView/$roomId")

    private val clientFlow = UiState.selectClient(sessionId, viewModelScope)

    private val sessionGraphFlow = clientFlow.map { client ->
        client?.let {
            appGraph.sessionGraphFactory.create(it)
        }
    }
    private val roomPair = clientFlow.map { client ->
        Pair(client?.getRoom(roomId), client?.getJoinedRoom(roomId))
    }

    private val roomGraphFlow = combine(sessionGraphFlow, roomPair) { sessionGraph, (baseRoom, joinedRoom) ->
        sessionGraph ?: return@combine null
        joinedRoom ?: return@combine null
        baseRoom ?: return@combine null
        sessionGraph.roomGraphFactory.create(joinedRoom, baseRoom)
    }

    private val timelineController = flow {
        var controller: TimelineController? = null
        roomPair.collect {
            controller?.close()
            controller = it.second?.let { TimelineController(it) }
            emit(controller)
        }
    }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    override fun onCleared() {
        super.onCleared()
        timelineController.value?.close()
    }

    val activeTimeline = timelineController.flatMapLatest {
        it?.activeTimelineFlow() ?: flowOf(null)
    }

    val timelineItems = activeTimeline.flatMapLatest {
        it ?: return@flatMapLatest flowOf(persistentListOf())
        it.timelineItems.map { it.toPersistentList() }
    }

    val forwardPaginationStatus = activeTimeline.flatMapLatest { it?.forwardPaginationStatus ?: flowOf(null) }
    val backwardPaginationStatus = activeTimeline.flatMapLatest { it?.backwardPaginationStatus ?: flowOf(null) }

    override val windowTitle: Flow<ComposableStringHolder?> = roomPair.map { (baseRoom, joinedRoom) ->
        baseRoom?.info()?.name?.toStringHolder()
    }.filterNotNull()

    override fun verifyDestination(destination: Destination): Boolean {
        return destination is Destination.Conversation && destination.sessionId == sessionId && destination.roomId == roomId
    }

    fun paginateForward() {
        viewModelScope.launch {
            log.d("Request forward pagination")
            timelineController.value?.paginate(Timeline.PaginationDirection.FORWARDS)
                ?.onFailure { log.w("Cannot paginate forwards") }
                ?.onSuccess { log.d("Paginated forwards") }
        }
    }

    fun paginateBackward() {
        viewModelScope.launch {
            log.d("Request backward pagination")
            timelineController.value?.paginate(Timeline.PaginationDirection.BACKWARDS)
                ?.onFailure { log.w("Cannot paginate backwards") }
                ?.onSuccess { log.d("Paginated backwards") }
        }
    }

    override fun handleNavigationModeEvent(key: KeyTrigger): Boolean {
        val keyConfig = UiState.keybindingsConfig.value
        val conversationAction = keyConfig.conversation.find { it.trigger == key } ?: return false
        return when (conversationAction.action) {
            Action.Conversation.SetSetting -> {
                viewModelScope.launch {
                    RevengePrefs.handleSetAction(conversationAction.args)
                }
                true
            }
            Action.Conversation.ToggleSetting -> {
                viewModelScope.launch {
                    RevengePrefs.handleToggleAction(conversationAction.args)
                }
                true
            }
        }
    }

    private fun markEventAsRead(eventId: EventId, receiptType: ReceiptType): Boolean {
        val timeline = timelineController.value ?: run {
            log.e { "No timeline to execute event action" }
            return false
        }
        viewModelScope.launch {
            timeline.invokeOnCurrentTimeline {
                sendReadReceipt(eventId, receiptType)
                    .onFailure { log.e("Failed to send private read receipt", it) }
                // Always keep fully read in sync with read receipts for now
                sendReadReceipt(eventId, ReceiptType.FULLY_READ)
                    .onFailure { log.e("Failed to send fully read marker", it) }
            }
        }
        return true
    }

    fun getKeyboardActionProviderForEvent(eventId: EventId): KeyboardActionProvider {
        return object : KeyboardActionProvider {
            override fun handleNavigationModeEvent(key: KeyTrigger): Boolean {
                val binding = UiState.keybindingsConfig.value.event.find { it.trigger == key } ?: return false
                return when (binding.action) {
                    Action.Event.MarkRead -> markEventAsRead(eventId, ReceiptType.READ)
                    Action.Event.MarkReadPrivate -> markEventAsRead(eventId, ReceiptType.READ_PRIVATE)
                }
            }
        }
    }

    companion object {
        fun factory(
            sessionId: SessionId,
            roomId: RoomId,
        ) = viewModelFactory {
            initializer {
                ConversationViewModel(sessionId, roomId)
            }
        }
    }
}
