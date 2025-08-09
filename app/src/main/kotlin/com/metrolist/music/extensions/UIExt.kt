package com.metrolist.music.extensions

import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Smoothly animate scroll to center an item in the LazyList viewport.
 * Inspired by SimpMusic's smooth animation approach with extremely slow transitions (5 seconds).
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
            // Animate to center the item extremely slowly (5 seconds)
            this@animateScrollAndCentralizeItem.animateScrollBy(
                value = (childCenter - center).toFloat(),
                animationSpec = tween(durationMillis = 5000) // Extremely slow 5-second animation
            )
        } else {
            // If item is not visible, animate to it directly with slow animation
            this@animateScrollAndCentralizeItem.animateScrollToItem(
                index = index,
                animationSpec = tween(durationMillis = 5000) // Extremely slow 5-second animation
            )
        }
    }
}