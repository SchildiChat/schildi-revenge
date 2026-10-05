package chat.schildi.revenge.model

import androidx.core.content.pm.ShortcutManagerCompat
import chat.schildi.revenge.RevengeApplication
import chat.schildi.revenge.notification.AndroidNotifier
import co.touchlab.kermit.Logger
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId

actual fun platformOnMessageSent(
    sessionId: SessionId,
    roomId: RoomId
) {
    val context = RevengeApplication.instance
    val shortcutId = AndroidNotifier.conversationShortcutId(sessionId, roomId)
    val shortcut = runCatching {
        ShortcutManagerCompat.getShortcuts(
            context,
            ShortcutManagerCompat.FLAG_MATCH_DYNAMIC or ShortcutManagerCompat.FLAG_MATCH_CACHED,
        )
    }.onFailure {
        Logger.withTag("AndroidMessageSent").w("Failed to fetch shortcuts for $shortcutId", it)
    }
        .getOrNull()
        ?.firstOrNull { it.id == shortcutId } ?: return
    runCatching {
        ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
    }.onFailure {
        Logger.withTag("AndroidMessageSent").w("Failed to report conversation shortcut usage for $shortcutId", it)
    }
}
