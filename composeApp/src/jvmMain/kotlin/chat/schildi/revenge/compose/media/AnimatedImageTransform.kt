package chat.schildi.revenge.compose.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultFilterQuality
import coil3.compose.AsyncImagePainter

@Composable
expect fun rememberAnimatedImageTransform(
    filterQuality: FilterQuality = DefaultFilterQuality,
): (AsyncImagePainter.State) -> AsyncImagePainter.State
