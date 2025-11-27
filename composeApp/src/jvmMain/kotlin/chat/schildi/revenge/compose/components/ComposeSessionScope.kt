package chat.schildi.revenge.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import chat.schildi.revenge.compose.media.LocalSessionImageLoader
import chat.schildi.revenge.compose.media.imageLoader
import io.element.android.libraries.matrix.api.core.SessionId

val LocalSessionId = compositionLocalOf<SessionId?> { null }

@Composable
fun ComposeSessionScope(sessionId: SessionId, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalSessionId provides sessionId,
        LocalSessionImageLoader provides Pair(sessionId, imageLoader(sessionId)),
        content = content,
    )
}
