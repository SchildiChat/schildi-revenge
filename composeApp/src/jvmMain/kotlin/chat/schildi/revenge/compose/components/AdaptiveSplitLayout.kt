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

private data class AdaptiveSplitParentData(
    val maxWidth: Int?,
    val maxHeight: Int?,
    val weight: Int,
)

private data class AdaptiveSplitDataModifier(
    private val maxWidth: Int?,
    private val maxHeight: Int?,
    private val weight: Int,
) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): Any = AdaptiveSplitParentData(maxWidth, maxHeight, weight)
}

const val WEIGHT_DEFAULT = 100

data class AdaptiveSplitLayoutModifierPair(
    val outer: Modifier,
    val inner: Modifier,
    val weight: Int,
)

@Composable
fun prefWidthModifiers(
    maxWidthDp: Int,
    weight: Int = WEIGHT_DEFAULT,
): AdaptiveSplitLayoutModifierPair {
    return adaptiveLimitedSizeModifiers(
        maxWidth = maxWidthDp.dp,
        weight = weight,
    )
}

@Composable
fun adaptiveLimitedSizeModifiers(
    minWidth: Dp = Dp.Unspecified,
    minHeight: Dp = Dp.Unspecified,
    maxWidth: Dp = Dp.Unspecified,
    maxHeight: Dp = Dp.Unspecified,
    weight: Int = WEIGHT_DEFAULT,
): AdaptiveSplitLayoutModifierPair {
    val density = LocalDensity.current
    return AdaptiveSplitLayoutModifierPair(
        Modifier.reportAdaptiveLimitedSizeToParent(
            density = density,
            maxWidth = maxWidth,
            maxHeight = maxHeight,
            weight = weight,
        ),
        Modifier.sizeIn(
            minWidth = minWidth,
            minHeight = minHeight,
            maxWidth = maxWidth,
            maxHeight = maxHeight,
        ),
        weight = weight,
    )
}

@Stable
private fun Modifier.reportAdaptiveLimitedSizeToParent(
    density: Density,
    maxWidth: Dp = Dp.Unspecified,
    maxHeight: Dp = Dp.Unspecified,
    weight: Int = WEIGHT_DEFAULT,
): Modifier {
    return thenIf(maxWidth.isSpecified || maxHeight.isSpecified || weight != WEIGHT_DEFAULT) {
        AdaptiveSplitDataModifier(
            maxWidth = if (maxWidth.isSpecified) density.run { maxWidth.roundToPx() } else null,
            maxHeight = if (maxHeight.isSpecified) density.run { maxHeight.roundToPx() } else null,
            weight = weight,
        )
    }
}

private data class MeasureInfo(
    val index: Int,
    val measurable: Measurable,
    val data: AdaptiveSplitParentData?,
)

private fun MeasureInfo.weight(): Int = (data?.weight ?: WEIGHT_DEFAULT).coerceAtLeast(1)

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
        if (measurables.size == 1) {
            return@Layout layout(constraints.maxWidth, constraints.maxHeight) {
                measurables.first().measure(constraints).placeRelative(0, 0)
            }
        }
        val items =
            measurables.mapIndexed { index, measurable ->
                MeasureInfo(
                    index,
                    measurable,
                    measurable.parentData as? AdaptiveSplitParentData,
                )
            }

        val maxConstraint = constraints.maxConstraint()
        var fixedRenderedSum = 0
        var itemsRenderedDynamically = items
        while (true) {
            val dynamicWeightSum = itemsRenderedDynamically.sumOf { it.weight() }
            val dynamicAvailableSpace = maxConstraint - fixedRenderedSum
            val canProvideMoreSpace =
                itemsRenderedDynamically.filter { info ->
                    val dimension = info.data?.maxDimension()
                    val weightedSplitChunk =
                        ((dynamicAvailableSpace.toLong() * info.weight()) / dynamicWeightSum).toInt()
                    dimension != null && dimension < weightedSplitChunk
                }
            if (canProvideMoreSpace.isEmpty()) {
                break
            } else {
                itemsRenderedDynamically = itemsRenderedDynamically - canProvideMoreSpace.toSet()
                fixedRenderedSum += canProvideMoreSpace.sumOf { it.data?.maxDimension() ?: 0 }
                if (itemsRenderedDynamically.isEmpty()) {
                    break
                }
            }
        }

        val itemIndicesToRenderDynamically = itemsRenderedDynamically.map { it.index }.toSet()

        val fixedSpace =
            items.sumOf { item ->
                item.data?.maxDimension()?.takeIf { item.index !in itemIndicesToRenderDynamically } ?: 0
            }
        val dynamicSpace = maxConstraint - fixedSpace
        val dynamicWeightSum = itemsRenderedDynamically.sumOf { it.weight() }
        val measuredItems =
            items.map { item ->
                val maxDimension =
                    if (item.index in itemIndicesToRenderDynamically) {
                        ((dynamicSpace.toLong() * item.weight()) / dynamicWeightSum).toInt()
                    } else {
                        item.data?.maxDimension() ?: 0
                    }
                val constraints = constraints.adjustConstraints(maxDimension.coerceAtLeast(1))
                item.measurable.measure(constraints)
            }

        layout(constraints.maxWidth, constraints.maxHeight) {
            val perItemHalfPadding =
                if (itemIndicesToRenderDynamically.isEmpty()) {
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
