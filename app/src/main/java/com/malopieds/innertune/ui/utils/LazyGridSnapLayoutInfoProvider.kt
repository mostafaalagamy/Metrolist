@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.malopieds.innertune.ui.utils

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.grid.LazyGridLayoutInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.ui.util.fastForEach
import kotlin.math.abs

@ExperimentalFoundationApi
fun SnapLayoutInfoProvider(lazyGridState: LazyGridState): SnapLayoutInfoProvider =
    object : SnapLayoutInfoProvider {
        private val layoutInfo: LazyGridLayoutInfo
            get() = lazyGridState.layoutInfo

        override fun calculateApproachOffset(initialVelocity: Float): Float = 0f

        override fun calculateSnappingOffset(currentVelocity: Float): Float {
            var closestItemOffset = Float.MAX_VALUE

            layoutInfo.visibleItemsInfo.fastForEach { item ->
                val offset = item.offset.x.toFloat()

                if (abs(offset) < abs(closestItemOffset)) {
                    closestItemOffset = offset
                }
            }

            return closestItemOffset + currentVelocity
        }
    }

@ExperimentalFoundationApi
fun SnapLayoutInfoProvider(layoutInfo: LazyListLayoutInfo): SnapLayoutInfoProvider =
    object : SnapLayoutInfoProvider {
        override fun calculateApproachOffset(initialVelocity: Float): Float = 0f

        override fun calculateSnappingOffset(currentVelocity: Float): Float {
            var closestItemOffset = Float.MAX_VALUE

            layoutInfo.visibleItemsInfo.fastForEach { item ->
                val offset = item.offset.toFloat()

                if (abs(offset) < abs(closestItemOffset)) {
                    closestItemOffset = offset
                }
            }

            return closestItemOffset + currentVelocity
        }
    }
