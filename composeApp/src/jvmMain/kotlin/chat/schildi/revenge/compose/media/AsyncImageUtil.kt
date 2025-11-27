package chat.schildi.revenge.compose.media

import co.touchlab.kermit.Logger
import coil3.compose.AsyncImagePainter

fun onAsyncImageError(error: AsyncImagePainter.State.Error) {
    Logger.withTag("AsyncImage").w("Failed to load image: $error")
}
