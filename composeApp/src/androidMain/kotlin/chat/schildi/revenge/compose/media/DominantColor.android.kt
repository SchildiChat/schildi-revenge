package chat.schildi.revenge.compose.media

import androidx.compose.ui.graphics.Color
import coil3.BitmapImage
import coil3.Image

internal actual fun extractDominantColor(image: Image): Color? {
    val bitmap = (image as? BitmapImage)?.bitmap ?: return null
    return IntArray(bitmap.width * bitmap.height).also { pixels ->
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    }.let(::dominantOpaqueColor)
}
