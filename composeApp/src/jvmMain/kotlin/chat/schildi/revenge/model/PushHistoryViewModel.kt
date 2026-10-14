package chat.schildi.revenge.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chat.schildi.revenge.UiState
import chat.schildi.revenge.database.push.PushExecutionEntity
import chat.schildi.revenge.database.push.PushNotificationEventEntity
import chat.schildi.revenge.database.push.PushReceiptEntity
import chat.schildi.revenge.database.revengeDatabase
import chat.schildi.revenge.flatMerge
import chat.schildi.revenge.push.deserializeUnifiedPushPayload
import chat.schildi.revenge.util.mergeLists
import co.touchlab.kermit.Logger
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.room.RoomInfo
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.jetbrains.compose.resources.StringResource
import shire.res.generated.resources.Res
import shire.res.generated.resources.push_history_type_received
import shire.res.generated.resources.push_history_type_resolved
import shire.res.generated.resources.push_history_type_worker

enum class PushEventType(
    val titleRes: StringResource,
) {
    RESOLVED(Res.string.push_history_type_resolved),
    WORKER(Res.string.push_history_type_worker),
    RECEIVED(Res.string.push_history_type_received),
}

data class PushHistoryUiState(
    val events: ImmutableList<PushEvent>,
    val roomInfoByRoomId: ImmutableMap<ScopedRoomId, RoomInfo?>,
)

sealed interface PushEvent {
    val timestamp: Long
    val sessionId: SessionId
    val roomId: RoomId?
    val eventId: EventId?
    val scopedRoomId: ScopedRoomId?
        get() = roomId?.let { ScopedRoomId(sessionId, it) }

    val renderKey: String

    data class PushNotificationReceived(val entity: PushReceiptEntity) : PushEvent {
        override val timestamp = entity.timestamp
        override val sessionId = SessionId(entity.sessionId)
        override val roomId = entity.roomId?.let(::RoomId)
        override val eventId: EventId? = entity.payload?.let(::deserializeUnifiedPushPayload)?.notification?.eventId?.let(::EventId)
        override val renderKey = "pnrec/${entity.id}"
    }
    data class PushNotificationResolved(val entity: PushNotificationEventEntity) : PushEvent {
        override val timestamp = entity.resolvedTimestamp ?: entity.timestamp
        override val sessionId = SessionId(entity.sessionId)
        override val roomId = RoomId(entity.roomId)
        override val eventId = EventId(entity.eventId)
        override val renderKey = "pnres/${entity.sessionId}/${entity.roomId}/${entity.eventId}"
    }
    data class PushWorker(val entity: PushExecutionEntity) : PushEvent {
        override val timestamp = entity.timestamp
        override val sessionId = SessionId(entity.sessionId)
        override val roomId = RoomId(entity.roomId)
        override val eventId = null
        override val renderKey = "pw/${entity.sessionId}/${entity.roomId}/${entity.timestamp}"
    }
}

class PushHistoryViewModel(
    val sessionId: SessionId?,
) : ViewModel() {

    private val log = Logger.withTag("PushHistory")

    private val dao = revengeDatabase.pushNotificationDao()
    private val since = System.currentTimeMillis() - 24 * 60 * 60 * 1000

    private val loadStateHolder = if (sessionId == null) {
        UiState.globalLoadState
    } else {
        LoadStateHolder(LoadCheckPoint.Client(sessionId))
    }
    val loadState = loadStateHolder.state

    private val _enabledTypes = MutableStateFlow(PushEventType.entries.toSet())
    val enabledTypes = _enabledTypes.asStateFlow()

    fun toggleType(type: PushEventType) {
        _enabledTypes.value = enabledTypes.value.let { if (type in it) it - type else it + type }
    }

    private val clients = if (sessionId == null) {
        UiState.matrixClients
    } else {
        UiState.selectClient(sessionId, viewModelScope, loadStateHolder)
            .map { mapOf(sessionId to it) }
    }

    val accounts = if (sessionId == null) {
        UiState.combinedSessions.flatMerge(
            map = { it.client.userProfile },
            onEmpty = { emptyMap() },
            merge = { it.associateBy { it.userId } },
        )
    } else {
        flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val state = combine(
        clients,
        dao.followPushExecutions(sessionId?.value, since),
        dao.followEventPushReceived(sessionId?.value, since),
        dao.followEventPushResults(sessionId?.value, since),
    ) { clients, executions, received, resolved ->
        val events = mergeLists(
            executions.map(PushEvent::PushWorker),
            received.map(PushEvent::PushNotificationReceived),
            resolved.map(PushEvent::PushNotificationResolved),
            key = { it.renderKey },
            sortKey = { it.timestamp },
        )
        val roomIds = events.mapNotNull { it.scopedRoomId }.distinct()
        val roomInfoByRoomId = roomIds.associateWith { (sessionId, roomId) ->
            runCatching {
                clients[sessionId]?.getRoom(roomId)?.use { it.info() }
            }.onFailure { log.w("Failed to resolve room $roomId", it) }
                .getOrNull()
        }
        PushHistoryUiState(events.toPersistentList(), roomInfoByRoomId.toPersistentMap())
    }.combine(enabledTypes) { state, enabled ->
        if (enabled.size == PushEventType.entries.size) {
            state
        } else {
            state.copy(
                events = state.events.filter {
                    when (it) {
                        is PushEvent.PushNotificationReceived -> PushEventType.RECEIVED in enabled
                        is PushEvent.PushWorker -> PushEventType.WORKER in enabled
                        is PushEvent.PushNotificationResolved -> PushEventType.RESOLVED in enabled
                    }
                }.toPersistentList()
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
