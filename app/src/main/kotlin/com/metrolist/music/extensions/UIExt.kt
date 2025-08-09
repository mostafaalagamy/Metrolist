package com.metrolist.music.extensions

import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Smoothly animate scroll to center an item in the LazyList viewport.
 * Inspired by SimpMusic's smooth animation approach with smooth transitions (2.5 seconds).
 */
fun LazyListState.animateScrollAndCentralizeItem(
    index: Int,
    scope: CoroutineScope,
) {
    val itemInfo = this.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    scope.launch {
        if (itemInfo != null) {
            // Calculate the center of the viewport
            val center = this@animateScrollAndCentralizeItem.layoutInfo.viewportEndOffset / 2
            // Calculate the center of the target item
            val childCenter = itemInfo.offset + itemInfo.size / 2
            // Animate to center the item smoothly (2.5 seconds)
            this@animateScrollAndCentralizeItem.animateScrollBy(
                value = (childCenter - center).toFloat(),
                animationSpec = tween(durationMillis = 2500) // Smooth 2.5-second animation
            )
        } else {
            // If item is not visible, scroll to it first then center it slowly
            this@animateScrollAndCentralizeItem.animateScrollToItem(index)
            // After scrolling, try to center it with slow animation
            val newItemInfo = this@animateScrollAndCentralizeItem.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
            if (newItemInfo != null) {
                val center = this@animateScrollAndCentralizeItem.layoutInfo.viewportEndOffset / 2
                val childCenter = newItemInfo.offset + newItemInfo.size / 2
                this@animateScrollAndCentralizeItem.animateScrollBy(
                    value = (childCenter - center).toFloat(),
                    animationSpec = tween(durationMillis = 2500) // Smooth 2.5-second animation
                )
            }
        }
    }
}