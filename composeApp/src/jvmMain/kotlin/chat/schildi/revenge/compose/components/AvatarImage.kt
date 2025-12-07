package chat.schildi.revenge.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import chat.schildi.revenge.Dimens
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
        model = MediaRequestData(source, MediaRequestData.Kind.Thumbnail(AVATAR_THUMBNAIL_SIZE)),
        contentDescription = null,
        imageLoader = imageLoader(sessionId),
        onError = ::onAsyncImageError,
        modifier = modifier.size(size).clip(shape),
        contentScale = ContentScale.Crop,
        loading = {
            AvatarFallback(shape, Modifier.size(size))
        },
        error = {
            AvatarFallback(shape, Modifier.size(size))
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
) {
    // TODO
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant, shape))
}
