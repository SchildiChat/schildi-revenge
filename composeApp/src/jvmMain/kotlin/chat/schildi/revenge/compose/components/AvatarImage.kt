package chat.schildi.revenge.compose.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import chat.schildi.preferences.ScPrefs
import chat.schildi.preferences.value
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.compose.media.imageLoader
import chat.schildi.theme.ScColors
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.ui.media.MediaRequestData

@Composable
fun AvatarImage(
    source: MediaSource?,
    size: Dp,
    displayName: String,
    sessionId: SessionId? = LocalSessionId.current,
    modifier: Modifier = Modifier,
    shape: Shape = Dimens.avatarShape,
    contentDescription: String? = null,
) {
    if (source == null) {
        AvatarFallback(displayName, shape, size)
        return
    }
    SubcomposeAsyncImage(
        modifier = modifier.size(size).clip(shape),
        imageLoader = imageLoader(sessionId),
        model = MediaRequestData(source, MediaRequestData.Kind.Content),
        contentScale = ContentScale.Crop,
        alignment = Alignment.Center,
        contentDescription = contentDescription,
    ) {
        AnimatedContent(
            painter.state.collectAsState().value,
            transitionSpec = {
                fadeIn(
                    animationSpec = Dimens.tweenSmooth()
                ) togetherWith fadeOut(
                    animationSpec = Dimens.tweenSmooth()
                )
            },
        ) { state ->
            when (state) {
                is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                AsyncImagePainter.State.Empty -> {
                    AvatarFallback(displayName, shape, size)
                }

                is AsyncImagePainter.State.Error -> {
                    AvatarFallback(displayName, shape, size, isError = true)
                }

                is AsyncImagePainter.State.Loading -> {
                    AvatarFallback(displayName, shape, size, isLoading = true)
                }
            }
        }
    }
}

fun String.firstCodePoint(index: Int = 0): String? {
    if (isEmpty()) return null
    val cp = codePointAt(index)
    val count = Character.charCount(cp)
    return substring(index, index + count)
}

@Composable
fun AvatarFallback(
    displayName: String,
    shape: Shape,
    size: Dp,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    isError: Boolean = false,
) {
    val color = if (ScPrefs.RENDER_AVATAR_STATES.value()) animateColorAsState(
        if (isError) {
            MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
        } else if (isLoading) {
            ScColors.colorAccentBlue.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        }
    ).value else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    Box(modifier.size(size).background(color, shape), contentAlignment = Alignment.Center) {
        val text = remember(displayName) {
            val cleanedName = displayName.removePrefix("@")
            val firstChar = cleanedName.firstCodePoint() ?: "?"
            val firstNonAlphaNum = cleanedName.indexOfFirst { !it.isLetterOrDigit() }
            val secondChar = if (firstNonAlphaNum != -1)
                cleanedName.substring(firstNonAlphaNum).firstOrNull { it.isLetterOrDigit() }
            else
                null
            if (secondChar == null) {
                firstChar
            } else {
                "$firstChar$secondChar"
            }
        }
        val textSize = LocalDensity.current.run {
            (size/2).toSp()
        }
        Text(
            text,
            color = MaterialTheme.colorScheme.inverseOnSurface,
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = textSize, lineHeight = textSize),
            maxLines = 1,
        )
    }
}
