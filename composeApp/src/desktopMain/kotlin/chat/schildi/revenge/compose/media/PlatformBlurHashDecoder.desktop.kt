package chat.schildi.revenge.compose.media

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.vanniktech.blurhash.BlurHash

internal actual fun decodeBlurHash(blurHash: String, width: Int, height: Int): ImageBitmap? =
    BlurHash.decode(blurHash, width, height)?.toComposeImageBitmap()
