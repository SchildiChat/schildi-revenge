package chat.schildi.revenge.compose.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import coil3.ImageLoader
import coil3.PlatformContext
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.ui.media.ImageLoaderHolder

val LocalImageLoaderHolder = compositionLocalOf<ImageLoaderHolder?> { null }

@Composable
fun imageLoader(client: MatrixClient?): ImageLoader {
    return LocalImageLoaderHolder.current?.let {
        if (client == null) it.get() else it.get(client)
    } ?: remember { ImageLoader(PlatformContext.INSTANCE) }
}
