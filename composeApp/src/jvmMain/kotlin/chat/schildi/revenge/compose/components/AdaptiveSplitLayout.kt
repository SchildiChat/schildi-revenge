package chat.schildi.revenge.compose.components

import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import chat.schildi.preferences.ScPref
import chat.schildi.preferences.value

private data class AdaptiveSplitParentData(
    val maxWidth: Int?,
    val maxHeight: Int?,
)

private data class AdaptiveSplitDataModifier(
    private val maxWidth: Int?,
    private val maxHeight: Int?,
) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): Any = AdaptiveSplitParentData(maxWidth, maxHeight)
}

data class AdaptiveSplitLayoutModifierPair(
    val outer: Modifier,
    val inner: Modifier,
)

@Composable
fun prefWidthModifiers(
    maxWidth: ScPref<Int>,
): AdaptiveSplitLayoutModifierPair {
    val prefValue = maxWidth.value()
    return adaptiveLimitedSizeModifiers(
        maxWidth = prefValue.dp,
    )
}

@Composable
fun adaptiveLimitedSizeModifiers(
    minWidth: Dp = Dp.Unspecified,
    minHeight: Dp = Dp.Unspecified,
    maxWidth: Dp = Dp.Unspecified,
    maxHeight: Dp = Dp.Unspecified,
): AdaptiveSplitLayoutModifierPair {
    val density = LocalDensity.current
    return AdaptiveSplitLayoutModifierPair(
        Modifier.reportAdaptiveLimitedSizeToParent(
            density = density,
            maxWidth = maxWidth,
            maxHeight = maxHeight,
        ),
        Modifier.sizeIn(
            minWidth = minWidth,
            minHeight = minHeight,
            maxWidth = maxWidth,
            maxHeight = maxHeight,
        ),
    )
}

@Stable
private fun Modifier.reportAdaptiveLimitedSizeToParent(
    density: Density,
    maxWidth: Dp = Dp.Unspecified,
    maxHeight: Dp = Dp.Unspecified,
): Modifier {
    return thenIf(maxWidth.isSpecified || maxHeight.isSpecified) {
        AdaptiveSplitDataModifier(
            maxWidth = if (maxWidth.isSpecified) density.run { maxWidth.roundToPx() } else null,
            maxHeight = if (maxHeight.isSpecified) density.run { maxHeight.roundToPx() } else null,
        )
    }
}

private data class MeasureInfo(
    val index: Int,
    val measurable: Measurable,
    val data: AdaptiveSplitParentData?,
)

/**
 * A Row-like layout that spaces children equally in the available width.
 * Children are rendered smaller than the equal size if they use the adaptiveLimitedSize() modifier with a width
 * smaller than the otherwise available space, to allow other children to grow bigger in return.
 */
@Composable
fun AdaptiveRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AdaptiveSplitLayout(
        maxDimension = { this.maxWidth },
        maxConstraint = { this.maxWidth },
        dimension = { this.width },
        adjustConstraints = { copy(minWidth = 0, maxWidth = it) },
        placeOffset = { IntOffset(it, 0) },
        modifier = modifier,
        content = content,
    )
}

/**
 * A Column-like layout that spaces children equally in the available height.
 * Children are rendered smaller than the equal size if they use the adaptiveLimitedSize() modifier with a height
 * smaller than the otherwise available space, to allow other children to grow bigger in return.
 */
@Composable
fun AdaptiveColumn(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AdaptiveSplitLayout(
        maxDimension = { this.maxHeight },
        maxConstraint = { this.maxHeight },
        dimension = { this.height },
        adjustConstraints = { copy(minHeight = 0, maxHeight = it) },
        placeOffset = { IntOffset(0, it) },
        modifier = modifier,
        content = content,
    )
}

/**
 * A layout that spaces children equally in either a row or column layout fashion (depending on passed parameters),
 * but renders children smaller if they use the adaptiveLimitedSize() modifier with a dimension smaller than the
 * otherwise available space, to allow other children to grow bigger in return.
 */
@Composable
private fun AdaptiveSplitLayout(
    maxDimension: AdaptiveSplitParentData.() -> Int?,
    maxConstraint: Constraints.() -> Int,
    dimension: Placeable.() -> Int,
    adjustConstraints: Constraints.(Int) -> Constraints,
    placeOffset: (Int) -> IntOffset,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier,
        content = content,
    ) { measurables, constraints ->
        if (measurables.isEmpty()) {
            return@Layout layout(constraints.maxWidth, constraints.maxHeight) {}
        }
        val items = measurables.mapIndexed { index, measurable ->
            MeasureInfo(
                index,
                measurable,
                measurable.parentData as? AdaptiveSplitParentData,
            )
        }

        var equallySplitChunk = constraints.maxConstraint() / items.size
        var fixedRenderedSum = 0
        var itemsRenderedDynamically = items
        while (true) {
            val canProvideMoreSpace = itemsRenderedDynamically.filter { info ->
                val dimension = info.data?.maxDimension()
                dimension != null && dimension < equallySplitChunk
            }
            if (canProvideMoreSpace.isEmpty()) {
                break
            } else {
                itemsRenderedDynamically = itemsRenderedDynamically - canProvideMoreSpace.toSet()
                fixedRenderedSum += canProvideMoreSpace.sumOf { it.data?.maxDimension() ?: 0 }
                if (itemsRenderedDynamically.isEmpty()) {
                    break
                }
                equallySplitChunk = (constraints.maxConstraint() - fixedRenderedSum) / itemsRenderedDynamically.size
            }
        }

        val itemIndicesToRenderDynamically = itemsRenderedDynamically.map { it.index }

        val fixedSpace = items.sumOf { item ->
            item.data?.maxDimension()?.takeIf { item.index !in itemIndicesToRenderDynamically } ?: 0
        }
        val dynamicSpace = constraints.maxConstraint() - fixedSpace
        val dynamicSpacePerItem = if (itemIndicesToRenderDynamically.isEmpty()) {
            0
        } else {
            dynamicSpace / itemsRenderedDynamically.size
        }
        val measuredItems = items.map { item ->
            val maxDimension = if (item.index in itemIndicesToRenderDynamically) {
                dynamicSpacePerItem
            } else {
                item.data?.maxDimension() ?: 0
            }
            val constraints = constraints.adjustConstraints(maxDimension.coerceAtLeast(1))
            item.measurable.measure(constraints)
        }

        layout(constraints.maxWidth, constraints.maxHeight) {
            val perItemHalfPadding = if (itemIndicesToRenderDynamically.isEmpty()) {
                // Everything renders at its max, need to fill some gaps
                dynamicSpace / measuredItems.size / 2
            } else {
                // We can fill all bounds
                0
            }
            var offset = 0
            measuredItems.forEach { item ->
                item.placeRelative(placeOffset(offset + perItemHalfPadding))
                offset += item.dimension() + perItemHalfPadding * 2
            }
        }
    }
}
