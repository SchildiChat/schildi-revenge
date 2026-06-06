package chat.schildi.revenge.compose.destination.conversation.event.message

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import chat.schildi.revenge.compose.media.imageLoader
import chat.schildi.revenge.compose.media.rememberAnimatedImageTransform
import chat.schildi.revenge.model.conversation.MediaPreviewState
import coil3.compose.SubcomposeAsyncImage
import io.element.android.libraries.matrix.ui.media.MediaRequestData
import kotlinx.coroutines.delay

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ImagePreview(
    state: MediaPreviewState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentIndex by remember(state) { mutableStateOf(state.initialIndex) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var showArrows by remember { mutableStateOf(true) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(currentIndex) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    LaunchedEffect(showArrows) {
        if (showArrows) {
            delay(3000)
            showArrows = false
        }
    }

    val currentItem = state.items.getOrNull(currentIndex)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onPointerEvent(PointerEventType.Move) { showArrows = true }
            .onKeyEvent { event ->
                when (event.key) {
                    Key.Escape -> {
                        onDismiss(); true
                    }
                    Key.DirectionLeft -> {
                        if (currentIndex > 0) { currentIndex--; true } else false
                    }
                    Key.DirectionRight -> {
                        if (currentIndex < state.items.lastIndex) { currentIndex++; true } else false
                    }
                    else -> false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        currentItem?.let { item ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 10f)
                            if (scale > 1f) {
                                offsetX += pan.x
                                offsetY += pan.y
                            }
                        }
                    }
                    .onPointerEvent(PointerEventType.Scroll) { event ->
                        val delta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                        scale = (scale * (1 - delta * 0.1f)).coerceIn(1f, 10f)
                        event.changes.forEach { it.consume() }
                    }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offsetX
                        translationY = offsetY
                    },
                contentAlignment = Alignment.Center,
            ) {
                SubcomposeAsyncImage(
                    model = MediaRequestData(item.source, MediaRequestData.Kind.Content),
                    contentDescription = null,
                    imageLoader = imageLoader(),
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.High,
                    transform = rememberAnimatedImageTransform(),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        AnimatedVisibility(
            visible = showArrows && currentIndex > 0,
            enter = fadeIn() + slideInHorizontally { -it / 4 },
            exit = fadeOut() + slideOutHorizontally { -it / 4 },
            modifier = Modifier.align(Alignment.CenterStart).padding(8.dp),
        ) {
            val arrowBg = Color.Black.copy(alpha = 0.45f)
            IconButton(
                onClick = { currentIndex-- },
                modifier = Modifier.size(48.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(arrowBg, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showArrows && currentIndex < state.items.lastIndex,
            enter = fadeIn() + slideInHorizontally { it / 4 },
            exit = fadeOut() + slideOutHorizontally { it / 4 },
            modifier = Modifier.align(Alignment.CenterEnd).padding(8.dp),
        ) {
            val arrowBg = Color.Black.copy(alpha = 0.45f)
            IconButton(
                onClick = { currentIndex++ },
                modifier = Modifier.size(48.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(arrowBg, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }

        IconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White,
            )
        }
    }
}
