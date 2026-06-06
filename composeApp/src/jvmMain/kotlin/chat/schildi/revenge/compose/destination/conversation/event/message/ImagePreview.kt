package chat.schildi.revenge.compose.destination.conversation.event.message

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import chat.schildi.revenge.compose.media.imageLoader
import chat.schildi.revenge.compose.media.rememberAnimatedImageTransform
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.ui.media.MediaRequestData

@Composable
fun ImagePreview(
    source: MediaSource,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.key == Key.Escape) {
                    onDismiss()
                    true
                } else false
            },
        contentAlignment = Alignment.Center,
    ) {
        SubcomposeAsyncImage(
            model = MediaRequestData(source, MediaRequestData.Kind.Content),
            contentDescription = null,
            imageLoader = imageLoader(),
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.High,
            transform = rememberAnimatedImageTransform(),
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 10f)
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                },
        )

        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White,
            )
        }
    }
}
