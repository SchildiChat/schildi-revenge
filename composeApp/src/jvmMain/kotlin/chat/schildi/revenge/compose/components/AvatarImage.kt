package chat.schildi.revenge.compose.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.UiState
import chat.schildi.revenge.compose.media.imageLoader
import chat.schildi.revenge.compose.media.onAsyncImageError
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.ui.media.MediaRequestData

private const val AVATAR_THUMBNAIL_SIZE = 512L

@Composable
fun AvatarImage(
    source: MediaSource?,
    size: Dp,
    sessionId: SessionId? = LocalSessionId.current,
    modifier: Modifier = Modifier,
    shape: Shape = Dimens.avatarShape,
) {
    if (source == null) {
        AvatarFallback(shape, modifier.size(size))
        return
    }
    SubcomposeAsyncImage(
        model = MediaRequestData(source, MediaRequestData.Kind.Content),
        filterQuality = FilterQuality.High,
        contentDescription = null,
        imageLoader = imageLoader(sessionId),
        onError = ::onAsyncImageError,
        modifier = modifier.size(size).clip(shape),
        contentScale = ContentScale.Crop,
        loading = {
            AvatarFallback(shape, Modifier.size(size), isLoading = true)
        },
        error = {
            AvatarFallback(shape, Modifier.size(size), isError = true)
        },
        success = {
            SubcomposeAsyncImageContent(Modifier.size(size))
        }
    )
}

@Composable
fun AvatarFallback(
    shape: Shape,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    isError: Boolean = false,
) {
    // TODO better design
    val color = animateColorAsState(
        if (UiState.showHiddenItems.collectAsState().value) {
            if (isError) {
                MaterialTheme.colorScheme.error
            } else if (isLoading) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            }
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    ).value
    Box(modifier.background(color, shape))
}
