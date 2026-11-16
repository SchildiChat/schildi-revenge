package chat.schildi.revenge.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import chat.schildi.lib.preferences.ScPrefs
import chat.schildi.revenge.RevengeAppGraph
import chat.schildi.revenge.ScCoroutines
import chat.schildi.revenge.util.tryOrNull
import co.touchlab.kermit.Logger
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.room.Receipts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        actionScope.launch {
            executeNotificationAction(intent)
        }
    }
}

private val log = Logger.withTag("NotificationActionReceiver")
private val actionScope = ScCoroutines.scope(Dispatchers.IO, "NotificationAction")

private suspend fun executeNotificationAction(intent: Intent) {
    val sessionId = intent.getStringExtra(AndroidNotifier.EXTRA_SESSION_ID) ?: return
    val roomId = intent.getStringExtra(AndroidNotifier.EXTRA_ROOM_ID) ?: return
    val eventId = intent.getStringExtra(AndroidNotifier.EXTRA_EVENT_ID) ?: return
    val actionName = intent.getStringExtra(AndroidNotifier.EXTRA_ACTION)
    val action = actionName?.let { tryOrNull { ScPrefs.NotificationAction.valueOf(it) } }
    if (action == null) {
        log.e("Invalid notification action: $actionName")
        return
    }
    if (action == ScPrefs.NotificationAction.NONE) return

    val client = RevengeAppGraph.sessionCache.getOrRestore(SessionId(sessionId))
        .onFailure { failure ->
            log.e("Failed to restore session $sessionId for notification action", failure)
        }.getOrNull() ?: return
    val room = client.getRoom(RoomId(roomId))
    if (room != null) {
        room.use { room ->
            val eventId = EventId(eventId)
            val receipts = Receipts(
                fullyRead = eventId.takeIf {
                    action in ScPrefs.NotificationAction.SETS_FULLY_READ_MARKER
                },
                publicReadReceipt = eventId.takeIf {
                    action in ScPrefs.NotificationAction.SETS_PUBLIC_RECEIPT
                },
                privateReadReceipt = eventId.takeIf {
                    action in ScPrefs.NotificationAction.SETS_PRIVATE_RECEIPT
                },
            )
            // TODO: send threaded read receipts for thread events once notifications are split between threads and the main timeline
            room.sendMultipleReceipts(receipts)
                .onFailure { failure ->
                    log.e("Failed to send receipts for $sessionId/$roomId/$eventId", failure)
                }
        }
    } else {
        log.w("Not joined to $roomId, skipping mark-as-read for notification action")
    }
    AndroidNotifier.dismiss(sessionId, roomId)
}
