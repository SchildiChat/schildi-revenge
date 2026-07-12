package chat.schildi.revenge.compose.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultFilterQuality
import coil3.compose.AsyncImagePainter

@Composable
actual fun rememberAnimatedImageTransform(
    filterQuality: FilterQuality,
): (AsyncImagePainter.State) -> AsyncImagePainter.State = remember(filterQuality) { { it } }
