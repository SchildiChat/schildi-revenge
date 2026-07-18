package chat.schildi.revenge.compose.media

import androidx.compose.ui.graphics.Color
import coil3.BitmapImage
import coil3.Image

internal actual fun extractDominantColor(image: Image): Color? {
    val bitmap = (image as? BitmapImage)?.bitmap ?: return null
    return dominantOpaqueColor(
        IntArray(bitmap.width * bitmap.height) { index ->
            bitmap.getColor(index % bitmap.width, index / bitmap.width)
        }
    )
}
