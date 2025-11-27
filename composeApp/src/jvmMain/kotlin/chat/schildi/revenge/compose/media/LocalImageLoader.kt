package chat.schildi.revenge.compose.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import chat.schildi.revenge.UiState
import chat.schildi.revenge.compose.components.LocalSessionId
import coil3.ImageLoader
import coil3.PlatformContext
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.ui.media.ImageLoaderHolder

/**
 * Cached image loader for session-specific destination, to avoid going through
 * blocking ImageLoaderHolder.get() each time.
 */
val LocalSessionImageLoader = compositionLocalOf<Pair<SessionId, ImageLoader>?> { null }
val LocalImageLoaderHolder = compositionLocalOf<ImageLoaderHolder?> { null }

@Composable
fun imageLoader(sessionId: SessionId? = LocalSessionId.current): ImageLoader {
    return if (sessionId != null) {
        LocalSessionImageLoader.current?.takeIf { it.first == sessionId }?.second
            ?: LocalImageLoaderHolder.current?.let { holder ->
                remember(sessionId) { holder.getIfExists(sessionId) }
                    ?: UiState.matrixClients.collectAsState().value[sessionId]?.let { holder.get(it) }
            }
    } else {
        LocalImageLoaderHolder.current?.get()
    } ?: remember { ImageLoader(PlatformContext.INSTANCE) }
}
