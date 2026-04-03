package chat.schildi.revenge.compose.media

import co.touchlab.kermit.Logger
import coil3.compose.AsyncImagePainter

fun onAsyncImageError(error: AsyncImagePainter.State.Error) {
    Logger.withTag("AsyncImage").w("Failed to load image: $error")
}

fun onAsyncImageState(state: AsyncImagePainter.State) {
    if (state is AsyncImagePainter.State.Error) {
        onAsyncImageError(state)
    }
}
