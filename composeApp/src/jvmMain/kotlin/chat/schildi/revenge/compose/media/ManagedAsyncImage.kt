package chat.schildi.revenge.compose.media

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultFilterQuality
import coil3.Image
import coil3.ImageLoader
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter

@Composable
fun ScAsyncImage(
    model: Any?,
    contentDescription: String?,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: androidx.compose.ui.layout.ContentScale = androidx.compose.ui.layout.ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    filterQuality: FilterQuality = DefaultFilterQuality,
    onError: ((AsyncImagePainter.State.Error) -> Unit)? = null,
) {
    val painter = rememberManagedAsyncImagePainter(
        model = model,
        imageLoader = imageLoader,
        filterQuality = filterQuality,
        onError = onError,
    )
    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier,
        alignment = alignment,
        contentScale = contentScale,
        alpha = alpha,
        colorFilter = colorFilter,
    )
}

@Composable
fun ScSubcomposeAsyncImage(
    model: Any?,
    contentDescription: String?,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: androidx.compose.ui.layout.ContentScale = androidx.compose.ui.layout.ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    filterQuality: FilterQuality = DefaultFilterQuality,
    content: @Composable ScSubcomposeAsyncImageScope.() -> Unit,
) {
    val painter = rememberManagedAsyncImagePainter(
        model = model,
        imageLoader = imageLoader,
        filterQuality = filterQuality,
    )
    val scope = remember(painter, alignment, contentScale, alpha, colorFilter, filterQuality, content) {
        ScSubcomposeAsyncImageScope(
            painter = painter,
            contentDescription = contentDescription,
            alignment = alignment,
            contentScale = contentScale,
            alpha = alpha,
            colorFilter = colorFilter,
            filterQuality = filterQuality,
            content = content,
        )
    }
    Box(modifier = modifier, contentAlignment = alignment) {
        scope.content()
    }
}

class ScSubcomposeAsyncImageScope internal constructor(
    val painter: AsyncImagePainter,
    private val contentDescription: String?,
    private val alignment: Alignment,
    private val contentScale: androidx.compose.ui.layout.ContentScale,
    private val alpha: Float,
    private val colorFilter: ColorFilter?,
    private val filterQuality: FilterQuality,
    internal val content: @Composable ScSubcomposeAsyncImageScope.() -> Unit,
) {
    @Composable
    fun SubcomposeAsyncImageContent(modifier: Modifier = Modifier) {
        Image(
            painter = painter,
            contentDescription = contentDescription,
            modifier = modifier,
            alignment = alignment,
            contentScale = contentScale,
            alpha = alpha,
            colorFilter = colorFilter,
        )
    }
}

@Composable
fun rememberManagedAsyncImagePainter(
    model: Any?,
    imageLoader: ImageLoader,
    filterQuality: FilterQuality = DefaultFilterQuality,
    onError: ((AsyncImagePainter.State.Error) -> Unit)? = null,
): AsyncImagePainter {
    var activeImage by remember { mutableStateOf<Image?>(null) }
    val painter = rememberAsyncImagePainter(
        model = model,
        imageLoader = imageLoader,
        filterQuality = filterQuality,
        onState = { state ->
            val nextImage = state.resultImage()
            if (activeImage !== nextImage) {
                disposeManagedImage(activeImage)
                activeImage = nextImage
            }
            if (state is AsyncImagePainter.State.Error) {
                onError?.invoke(state)
            }
        },
    )
    DisposableEffect(painter) {
        onDispose {
            disposeManagedImage(activeImage)
            activeImage = null
        }
    }
    return painter
}

private fun AsyncImagePainter.State.resultImage(): Image? = when (this) {
    AsyncImagePainter.State.Empty -> null
    is AsyncImagePainter.State.Loading -> null
    is AsyncImagePainter.State.Error -> result.image
    is AsyncImagePainter.State.Success -> result.image
}

private fun disposeManagedImage(image: Image?) {
    if (image == null || image.shareable) return
    (image as? AutoCloseable)?.runCatching { close() }
}
