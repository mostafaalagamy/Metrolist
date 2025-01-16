@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.metrolist.music.ui.utils

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridLayoutInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastForEach
import kotlin.math.abs

@ExperimentalFoundationApi
fun SnapLayoutInfoProvider(
    lazyGridState: LazyGridState,
): SnapLayoutInfoProvider = object : SnapLayoutInfoProvider {

    private val layoutInfo: LazyGridLayoutInfo
        get() = lazyGridState.layoutInfo

    override fun calculateApproachOffset(velocity: Float, decayOffset: Float): Float {
        return 0f
    }

    override fun calculateSnapOffset(velocity: Float): Float {
        var closestItemOffset = Float.MAX_VALUE

        layoutInfo.visibleItemsInfo.fastForEach { item ->
            val offset = item.offset.x.toFloat()

            // Check if the item is closer to the center than the current closest item
            if (abs(offset) < abs(closestItemOffset)) {
                closestItemOffset = offset
            }
        }

        // Adjust the offset based on the velocity
        return closestItemOffset + velocity
    }
}

@ExperimentalFoundationApi
fun SnapLayoutInfoProvider(
    layoutInfo: LazyListLayoutInfo,
): SnapLayoutInfoProvider = object : SnapLayoutInfoProvider {
    override fun calculateApproachOffset(velocity: Float, decayOffset: Float): Float {
        return 0f
    }

    override fun calculateSnapOffset(velocity: Float): Float {
        var closestItemOffset = Float.MAX_VALUE

        layoutInfo.visibleItemsInfo.fastForEach { item ->
            val offset = item.offset.toFloat()

            // Check if the item is closer to the center than the current closest item
            if (abs(offset) < abs(closestItemOffset)) {
                closestItemOffset = offset
            }
        }

        // Adjust the offset based on the velocity
        return closestItemOffset + velocity
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
