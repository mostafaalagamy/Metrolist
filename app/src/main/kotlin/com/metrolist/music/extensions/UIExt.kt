package com.metrolist.music.extensions

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Smoothly animate scroll to center an item in the LazyList viewport.
 * Inspired by SimpMusic's smooth animation approach.
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
            // Animate to center the item smoothly
            this@animateScrollAndCentralizeItem.animateScrollBy((childCenter - center).toFloat())
        } else {
            // If item is not visible, animate to it directly
            this@animateScrollAndCentralizeItem.animateScrollToItem(index)
        }
    }
}