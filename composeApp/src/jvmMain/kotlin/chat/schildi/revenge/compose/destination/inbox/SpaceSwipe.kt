package chat.schildi.revenge.compose.destination.inbox

import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBackIos
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import chat.schildi.lib.preferences.ScPrefs
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.compose.components.toPx
import chat.schildi.revenge.model.spaces.PSEUDO_SPACE_ID_NO_FILTER
import chat.schildi.revenge.model.spaces.SpaceListDataSource
import chat.schildi.revenge.model.spaces.resolveSelection
import chat.schildi.revenge.preferences.value
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class SpaceSwipeState(
    val offsetX: MutableFloatState,
    val draggableState: DraggableState,
)

data class ActiveSpaceSelectionState(
    val tabs: ImmutableList<SpaceListDataSource.AbstractSpaceHierarchyItem?>,
    val currentSelection: Int,
) {
    val canSwipeUp: Boolean
        get() = currentSelection < tabs.size - 1
    val canSwipeDown: Boolean
        get() = currentSelection > 0
    val canSwipe: Boolean
        get() = tabs.size > 1
    val swipeUpTarget: SpaceListDataSource.AbstractSpaceHierarchyItem?
        get() = tabs.getOrNull(currentSelection + 1)
    val swipeDownTarget: SpaceListDataSource.AbstractSpaceHierarchyItem?
        get() = tabs.getOrNull(currentSelection - 1)
}

@Composable
fun rememberSpaceSwipeState(): SpaceSwipeState {
    val offsetX = remember { mutableFloatStateOf(0f) }
    return SpaceSwipeState(
        offsetX = offsetX,
        draggableState = rememberDraggableState {
            offsetX.floatValue += it
        }
    )
}

@Composable
fun ImmutableList<SpaceListDataSource.AbstractSpaceHierarchyItem>?.toSelectionState(
    spaceSelectionHierarchy: ImmutableList<String>,
): ActiveSpaceSelectionState {
    if (isNullOrEmpty()) {
        return remember {
            ActiveSpaceSelectionState(
                tabs = persistentListOf(null),
                currentSelection = 0,
            )
        }
    }
    val showAllRooms = ScPrefs.PSEUDO_SPACE_ALL_ROOMS.value()
    return remember(this, spaceSelectionHierarchy, showAllRooms) {
        val parent = if (spaceSelectionHierarchy.isNotEmpty()) {
            resolveSelection(spaceSelectionHierarchy.take(spaceSelectionHierarchy.size - 1))
        } else {
            null
        }
        val tabs = if (parent != null) {
            listOf(parent) + parent.spaces
        } else if (showAllRooms) {
            listOf(null) + this
        } else {
            this
        }
        val currentSelection = spaceSelectionHierarchy.lastOrNull() ?: PSEUDO_SPACE_ID_NO_FILTER
        ActiveSpaceSelectionState(
            tabs = tabs.toPersistentList(),
            currentSelection = tabs.indexOfFirst {
                (it?.selectionId ?: PSEUDO_SPACE_ID_NO_FILTER) == currentSelection
            }.coerceAtLeast(0),
        )
    }
}

@Composable
fun Modifier.spaceSwipe(
    swipeState: SpaceSwipeState,
    selectionState: ActiveSpaceSelectionState,
    selectSpace: (SpaceListDataSource.AbstractSpaceHierarchyItem?) -> Unit,
): Modifier {
    if (LocalInputModeManager.current.inputMode != InputMode.Touch) {
        return this
    }
    // Indicator width itself is 96dp.
    // Indicator threshold: how much we move the indicator out of the screen before swiping
    // Swipe threshold: how much the user should swipe to trigger
    val swipeThreshold = 74.dp.toPx()
    val decay: DecayAnimationSpec<Float> = rememberSplineBasedDecay()
    return draggable(
        orientation = Orientation.Horizontal,
        enabled = selectionState.canSwipe,
        onDragStopped = { velocity ->
            val targetOffsetX = decay.calculateTargetValue(
                swipeState.offsetX.floatValue,
                velocity
            )
            // Note: we have spacesList.size+1 tabs, index 0 is always default/parent
            if (targetOffsetX < -swipeThreshold && selectionState.canSwipeUp) {
                selectSpace(selectionState.swipeUpTarget)
            } else if (targetOffsetX > swipeThreshold && selectionState.canSwipeDown) {
                selectSpace(selectionState.swipeDownTarget)
            }
            swipeState.offsetX.floatValue = 0f
        },
        state = swipeState.draggableState,
    )
}

@Composable
fun BoxScope.SpaceSwipeIndicatorOverlay(
    swipeState: SpaceSwipeState,
    selectionState: ActiveSpaceSelectionState,
) {
    val indicatorThreshold = 104.dp.toPx()
    // Swipe down indicator
    val swipeProgress = min(1f, swipeState.offsetX.floatValue.absoluteValue / indicatorThreshold)
    if (selectionState.canSwipeDown) {
        SwipeIndicator(
            space = selectionState.swipeDownTarget,
            upwards = false,
            thresholdProgress = swipeProgress,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset {
                    val x = max(swipeState.offsetX.floatValue, 0f) - indicatorThreshold
                    IntOffset(x.roundToInt(), 0)
                }
        )
    }
    // Swipe up indicator
    if (selectionState.canSwipeUp) {
        SwipeIndicator(
            space = selectionState.swipeUpTarget,
            upwards = true,
            thresholdProgress = swipeProgress,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset {
                    val x = min(swipeState.offsetX.floatValue, 0f) + indicatorThreshold
                    IntOffset(x.roundToInt(), 0)
                }
        )
    }
}

@Composable
private fun SwipeIndicator(
    space: SpaceListDataSource.AbstractSpaceHierarchyItem?,
    upwards: Boolean,
    thresholdProgress: Float,
    modifier: Modifier = Modifier,
) {
    Row(modifier) {
        if (upwards) {
            SwipeIndicatorArrow(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBackIos,
                thresholdProgress = thresholdProgress,
            )
        }
        Box(
            Modifier
                .alpha(thresholdProgress)
                .background(MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.65f), CircleShape)
                .padding(8.dp)
        ) {
            AbstractSpaceIcon(
                space,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                size = Dimens.Inbox.spaceSwipeIndicator,
                shape = CircleShape,
            )
        }
        if (!upwards) {
            SwipeIndicatorArrow(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                thresholdProgress = thresholdProgress,
            )
        }
    }
}

@Composable
private fun RowScope.SwipeIndicatorArrow(
    imageVector: ImageVector,
    thresholdProgress: Float,
    modifier: Modifier = Modifier,
) {
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.65f),
        modifier = modifier
            .size(Dimens.Inbox.spaceSwipeIndicator)
            .align(Alignment.CenterVertically)
            .alpha(thresholdProgress)
    )
}
