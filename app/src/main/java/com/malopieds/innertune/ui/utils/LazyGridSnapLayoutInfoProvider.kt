@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.malopieds.innertune.ui.utils

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridLayoutInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastSumBy

@ExperimentalFoundationApi
fun SnapLayoutInfoProvider(
    lazyGridState: LazyGridState,
    positionInLayout: Density.(layoutSize: Float, itemSize: Float) -> Float =
        { layoutSize, itemSize -> (layoutSize / 2f - itemSize / 2f) },
): SnapLayoutInfoProvider = object : SnapLayoutInfoProvider {

    private val layoutInfo: LazyGridLayoutInfo
        get() = lazyGridState.layoutInfo

    // Single page snapping is the default
    override fun Density.calculateApproachOffset(initialVelocity: Float): Float = 0f

    override fun Density.calculateSnappingOffsetBounds(): ClosedFloatingPointRange<Float> {
        var lowerBoundOffset = Float.NEGATIVE_INFINITY
        var upperBoundOffset = Float.POSITIVE_INFINITY

        layoutInfo.visibleItemsInfo.fastForEach { item ->
            val offset =
                calculateDistanceToDesiredSnapPosition(layoutInfo, item, positionInLayout)

            // Find item that is closest to the center
            if (offset <= 0 && offset > lowerBoundOffset) {
                lowerBoundOffset = offset
            }

            // Find item that is closest to center, but after it
            if (offset >= 0 && offset < upperBoundOffset) {
                upperBoundOffset = offset
            }
        }

        return lowerBoundOffset.rangeTo(upperBoundOffset)
    }

    override fun Density.calculateSnapStepSize(): Float = with(layoutInfo) {
        if (visibleItemsInfo.isNotEmpty()) {
            visibleItemsInfo.fastSumBy { it.size.width } / visibleItemsInfo.size.toFloat()
        } else {
            0f
        }
    }
}

@ExperimentalFoundationApi
fun SnapLayoutInfoProvider(
    pagerState: PagerState,
    positionInLayout: Density.(layoutSize: Float, itemSize: Float) -> Float =
        { layoutSize, itemSize -> (layoutSize / 2f - itemSize / 2f) },
): SnapLayoutInfoProvider = object : SnapLayoutInfoProvider {

    private val layoutInfo: LazyListLayoutInfo
        get() = pagerState.layoutInfo

    // Single page snapping is the default
    override fun Density.calculateApproachOffset(initialVelocity: Float): Float = 0f

    override fun Density.calculateSnappingOffsetBounds(): ClosedFloatingPointRange<Float> {
        var lowerBoundOffset = Float.NEGATIVE_INFINITY
        var upperBoundOffset = Float.POSITIVE_INFINITY

        layoutInfo.visibleItemsInfo.fastForEach { item ->
            val offset =
                calculateDistanceToDesiredSnapPosition(layoutInfo, item, positionInLayout)

            // Find item that is closest to the center
            if (offset <= 0 && offset > lowerBoundOffset) {
                lowerBoundOffset = offset
            }

            // Find item that is closest to center, but after it
            if (offset >= 0 && offset < upperBoundOffset) {
                upperBoundOffset = offset
            }
        }

        return lowerBoundOffset.rangeTo(upperBoundOffset)
    }

    override fun Density.calculateSnapStepSize(): Float = with(layoutInfo) {
        if (visibleItemsInfo.isNotEmpty()) {
            visibleItemsInfo.fastSumBy { it.size } / visibleItemsInfo.size.toFloat()
        } else {
            0f
        }
    }
}

fun Density.calculateDistanceToDesiredSnapPosition(
    layoutInfo: LazyListLayoutInfo,
    item: LazyListItemInfo,
    positionInLayout: Density.(layoutSize: Float, itemSize: Float) -> Float,
): Float {
    val containerSize =
        with(layoutInfo) { singleAxisViewportSize - beforeContentPadding - afterContentPadding }

    val desiredDistance =
        positionInLayout(containerSize.toFloat(), item.size.toFloat())

    val itemCurrentPosition = item.offset
    return itemCurrentPosition - desiredDistance
}

private val LazyListLayoutInfo.singleAxisViewportSize: Int
    get() = if (orientation == Orientation.Vertical) viewportSize.height else viewportSize.width

fun Density.calculateDistanceToDesiredSnapPosition(
    layoutInfo: LazyGridLayoutInfo,
    item: LazyGridItemInfo,
    positionInLayout: Density.(layoutSize: Float, itemSize: Float) -> Float,
): Float {
    val containerSize =
        with(layoutInfo) { singleAxisViewportSize - beforeContentPadding - afterContentPadding }

    val desiredDistance = positionInLayout(containerSize.toFloat(), item.size.width.toFloat())
    val itemCurrentPosition = item.offset.x.toFloat()

    return itemCurrentPosition - desiredDistance
}

private val LazyGridLayoutInfo.singleAxisViewportSize: Int
    get() = if (orientation == Orientation.Vertical) viewportSize.height else viewportSize.width
