package chat.schildi.revenge.notification

import chat.schildi.revenge.ScCoroutines
import chat.schildi.revenge.UiState
import chat.schildi.revenge.model.RevengeRoomListDataSource
import co.touchlab.kermit.Logger
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.room.RoomNotificationMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

object NotifiableRoomSubscriber {
    private val log = Logger.withTag("NotifiableRoomSubscriber")
    private val scope = ScCoroutines.scope(Dispatchers.IO, "NotifiableRoomSubscriber")
    @OptIn(ExperimentalAtomicApi::class)
    private val launched = AtomicBoolean(false)

    private val notifiableRooms = combine(
        UiState.mutedAccounts,
        RevengeRoomListDataSource.allRooms
    ) { muted, allRooms ->
        if (muted == null) {
            emptyList()
        } else {
            allRooms.mapNotNull {
                if (it.sessionId in muted || it.summary.info.userDefinedNotificationMode == RoomNotificationMode.MUTE) {
                    null
                } else {
                    Pair(it.sessionId, it.summary.roomId)
                }
            }.sortedWith(compareBy({ it.first.value }, { it.second.value }))
        }
    }.distinctUntilChanged()

    private val currentSubscribed = mutableSetOf<Pair<SessionId, RoomId>>()

    @OptIn(FlowPreview::class, ExperimentalAtomicApi::class)
    fun launch() {
        if (launched.exchange(true)) {
            return
        }
        combine(
            notifiableRooms.debounce(1000),
            UiState.matrixClients
        ) { roomsOfInterest, clients ->
            val allowedSessions = clients.keys
            currentSubscribed.removeIf { it.first !in allowedSessions }
            val missing = roomsOfInterest.filter { it.first in allowedSessions }.toSet() - currentSubscribed
            missing.forEach { pair ->
                val (sessionId, roomId) = pair
                val room = clients[sessionId]?.getRoom(roomId)
                if (room == null) {
                    log.e { "Cannot subscribe to $roomId via $sessionId" }
                } else {
                    // TODO not an error
                    log.e { "Subscribing to $roomId via $sessionId" }
                    room.subscribeToSync()
                    currentSubscribed += pair
                }
            }
        }.launchIn(scope)
    }
}
