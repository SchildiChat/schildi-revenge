package chat.schildi.revenge.model.userlist

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import chat.schildi.revenge.UiState
import chat.schildi.revenge.actions.RoomContextSuggestionsProvider
import co.touchlab.kermit.Logger
import io.element.android.features.messages.impl.timeline.TimelineController
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.room.RoomMember
import io.element.android.libraries.matrix.api.room.roomMembers
import io.element.android.libraries.matrix.api.timeline.MatrixTimelineItem
import io.element.android.libraries.matrix.api.timeline.item.event.ReactionSender
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

data class UserReactionItem(
    val reaction: String,
    val shortcode: String?,
    val reactionSender: ReactionSender,
    val senderProfile: RoomMember?,
) : UserListItem {
    override val userId: UserId
        get() = reactionSender.senderId
    override val displayName: String?
        get() = senderProfile?.displayName
    override val avatarUrl: String?
        get() = senderProfile?.avatarUrl
}

@OptIn(ExperimentalCoroutinesApi::class)
class MessageReactionListViewModel(
    override val sessionId: SessionId,
    override val roomId: RoomId,
    val eventId: EventId,
) : AbstractUserListViewModel<UserReactionItem>() {

    private val log = Logger.withTag("ReactionsViewModel")

    private val clientFlow = UiState.selectClient(sessionId, viewModelScope)

    private val roomFlow = clientFlow.map { client ->
        client?.getJoinedRoom(roomId)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val timelineController = flow {
        var controller: TimelineController? = null
        roomFlow.collect {
            controller?.close()
            controller = it?.let { TimelineController(it) }
            controller?.focusOnEvent(eventId, null)
            emit(controller)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val roomMembersState = roomFlow.flatMapLatest { room ->
        room?.membersStateFlow ?: flowOf()
    }

    val activeTimeline = timelineController.flatMapLatest {
        it?.activeTimelineFlow() ?: flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val rawTimelineItems = activeTimeline.flatMapLatest {
        it?.timelineItems ?: flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    override val allEntries: StateFlow<ImmutableList<UserReactionItem>?> = combine(
        roomMembersState,
        rawTimelineItems,
    ) { members, events ->
        val membersLookup = members.roomMembers()?.associateBy { it.userId }
        val timelineEvent = events?.find { (it as? MatrixTimelineItem.Event)?.eventId == eventId }
                as? MatrixTimelineItem.Event
        log.d { "Found reactions: ${timelineEvent?.event?.reactions?.size} (${events?.size} events from timeline)" }
        timelineEvent?.event?.reactions?.flatMap { reaction ->
            reaction.senders.map {
                val member = membersLookup?.get(it.senderId)
                UserReactionItem(reaction.key, reaction.shortcode, it, member)
            }
        }?.sortedByDescending { it.reactionSender.timestamp }?.toPersistentList()
    }
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            null,
        )

    val entries = filteredEntriesFlow().stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        null,
    )

    override val userIdInRoomSuggestions = userIdInRoomSuggestionsFlow()

    val roomContextSuggestionsProvider = RoomContextSuggestionsProvider(
        sessionId = sessionId,
        peekRoom = { roomFlow.value },
    )

    init {
        roomFlow.onEach { room ->
            room?.updateMembers()
            room?.subscribeToSync()
        }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        timelineController.value?.close()
    }

    companion object {
        fun factory(
            sessionId: SessionId,
            roomId: RoomId,
            eventId: EventId,
        ) = viewModelFactory {
            initializer {
                MessageReactionListViewModel(sessionId, roomId, eventId)
            }
        }
    }
}
