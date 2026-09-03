package chat.schildi.revenge.compose.destination.conversation.event

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.spring
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.AddReaction
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import chat.schildi.lib.preferences.ScPrefs
import chat.schildi.lib.preferences.ScStringListPref
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.compose.components.thenIf
import chat.schildi.revenge.config.keybindings.Action
import chat.schildi.revenge.preferences.value
import co.touchlab.kermit.Logger
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.uuid.Uuid

data class MessageSwipeActionSpec(
    val kind: ScPrefs.MessageSwipeAction,
    val icon: ImageVector,
    val execute: () -> Unit,
)

enum class MessageSwipePosition {
    Neutral,
    Left,
    Right,
}

@Composable
fun ScStringListPref.swipeActionValue(focusId: Uuid): MessageSwipeActionSpec? {
    val rawValue = value()
    val kind = remember(rawValue) {
        try {
            ScPrefs.MessageSwipeAction.valueOf(rawValue)
        } catch (e: IllegalArgumentException) {
            Logger.withTag("swipeActionValue").e("Invalid message swipe action: $rawValue", e)
            null
        }
    }
    val keyHandler = LocalKeyboardActionHandler.current
    return when (kind) {
        ScPrefs.MessageSwipeAction.REPLY -> MessageSwipeActionSpec(kind, Icons.AutoMirrored.Default.Reply) {
            keyHandler.handleAction(focusId, Action.Event.ComposeReply)
        }
        ScPrefs.MessageSwipeAction.REACT -> MessageSwipeActionSpec(kind, Icons.Default.AddReaction) {
            keyHandler.handleAction(focusId, Action.Event.ComposeReaction)
        }
        ScPrefs.MessageSwipeAction.MARK_READ -> MessageSwipeActionSpec(kind, Icons.Default.Visibility) {
            keyHandler.handleAction(focusId, Action.Event.MarkEventRead)
            keyHandler.handleAction(focusId, Action.Event.MarkEventFullyRead)
        }
        ScPrefs.MessageSwipeAction.MARK_READ_PRIVATE -> MessageSwipeActionSpec(kind, Icons.Outlined.Visibility) {
            keyHandler.handleAction(focusId, Action.Event.MarkEventReadPrivate)
            keyHandler.handleAction(focusId, Action.Event.MarkEventFullyRead)
        }
        ScPrefs.MessageSwipeAction.NONE,
        null -> null
    }
}

@Composable
fun EventSwipeable(
    focusId: Uuid,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    val swipeActionLeft = ScPrefs.MESSAGE_SWIPE_ACTION_LEFT.swipeActionValue(focusId)
    val swipeActionRight = ScPrefs.MESSAGE_SWIPE_ACTION_RIGHT.swipeActionValue(focusId)
    val hasSwipeAction = swipeActionLeft != null || swipeActionRight != null
    val maxSwipe = LocalDensity.current.run { Dimens.Conversation.MessageSwipe.maxOffset.toPx() }
    val threshold = LocalDensity.current.run { Dimens.Conversation.MessageSwipe.threshold.toPx() }
    var offset by remember { mutableFloatStateOf(0f) }
    var swipeDirection by remember { mutableStateOf(MessageSwipePosition.Neutral) }
    var gestureId by remember { mutableIntStateOf(0) }
    val swipeState = rememberDraggableState { delta ->
        // Lock in the swipe direction
        if (swipeDirection == MessageSwipePosition.Neutral) {
            swipeDirection = when {
                delta < 0f && swipeActionLeft != null -> MessageSwipePosition.Left
                delta > 0f && swipeActionRight != null -> MessageSwipePosition.Right
                else -> MessageSwipePosition.Neutral
            }
        }
        offset = when (swipeDirection) {
            MessageSwipePosition.Neutral -> 0f
            MessageSwipePosition.Left -> (offset + delta).coerceIn(-maxSwipe, 0f)
            MessageSwipePosition.Right -> (offset + delta).coerceIn(0f, maxSwipe)
        }
    }
    val decay = rememberSplineBasedDecay<Float>()

    // Increase touch slop to reduce the risk of stealing scroll events.
    val viewConfiguration = LocalViewConfiguration.current
    val swipeViewConfiguration = remember(viewConfiguration) {
        object : ViewConfiguration by viewConfiguration {
            override val touchSlop = viewConfiguration.touchSlop *
                    Dimens.Conversation.MessageSwipe.touchSlopMultiplier
        }
    }
    CompositionLocalProvider(
        LocalViewConfiguration provides swipeViewConfiguration,
    ) {
        Box(Modifier.fillMaxWidth()) {
            MessageSwipeActionIndicator(
                action = when (swipeDirection) {
                    MessageSwipePosition.Neutral -> null
                    MessageSwipePosition.Left -> swipeActionLeft
                    MessageSwipePosition.Right -> swipeActionRight
                },
                direction = swipeDirection,
                offset = offset,
            )
            content(
                modifier
                    .thenIf(hasSwipeAction) {
                        draggable(
                            state = swipeState,
                            orientation = Orientation.Horizontal,
                            onDragStarted = {
                                gestureId++
                                offset = 0f
                                swipeDirection = MessageSwipePosition.Neutral
                            },
                            onDragStopped = { velocity ->
                                val targetOffset = decay.calculateTargetValue(offset, velocity)
                                when (swipeDirection) {
                                    MessageSwipePosition.Neutral -> {}
                                    MessageSwipePosition.Left -> if (targetOffset <= -threshold) {
                                        swipeActionLeft?.execute()
                                    }
                                    MessageSwipePosition.Right -> if (targetOffset >= threshold) {
                                        swipeActionRight?.execute()
                                    }
                                }

                                val stoppedGestureId = gestureId
                                animate(
                                    initialValue = offset,
                                    targetValue = 0f,
                                    animationSpec = spring(stiffness = Spring.StiffnessHigh),
                                ) { value, _ ->
                                    if (gestureId == stoppedGestureId) {
                                        offset = value
                                    }
                                }
                                if (gestureId == stoppedGestureId) {
                                    offset = 0f
                                    swipeDirection = MessageSwipePosition.Neutral
                                }
                            },
                        ).absoluteOffset { IntOffset(offset.roundToInt(), 0) }
                    }
            )
        }
    }
}

@Composable
private fun BoxScope.MessageSwipeActionIndicator(
    action: MessageSwipeActionSpec?,
    direction: MessageSwipePosition,
    offset: Float,
    modifier: Modifier = Modifier,
) {
    if (action == null || direction == MessageSwipePosition.Neutral) return

    val threshold = LocalDensity.current.run { Dimens.Conversation.MessageSwipe.threshold.toPx() }
    val progress = (offset.absoluteValue / threshold).coerceIn(0f, 1f)

    Icon(
        imageVector = action.icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .size(24.dp)
            .align(
                if (direction == MessageSwipePosition.Left) {
                    AbsoluteAlignment.CenterRight
                } else {
                    AbsoluteAlignment.CenterLeft
                }
            )
            .graphicsLayer {
                alpha = progress
                val revealScale = 0.7f + progress * 0.3f
                scaleX = revealScale
                scaleY = revealScale
                translationX = (offset - threshold * offset.sign).coerceIn(
                    if (direction == MessageSwipePosition.Left) 0f else -threshold,
                    if (direction == MessageSwipePosition.Right) 0f else threshold,
                )
            }
    )
}
