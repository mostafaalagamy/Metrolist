package com.metrolist.music.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.max

@Composable
fun DraggableScrollbar(
    scrollState: LazyListState,
    modifier: Modifier = Modifier,
    thumbColor: Color = Color.Gray.copy(alpha = 0.7f),
    thumbColorActive: Color = MaterialTheme.colorScheme.primary,
    thumbHeight: Dp = 72.dp,
    thumbWidth: Dp = 8.dp,
    thumbCornerRadius: Dp = 4.dp,
    trackWidth: Dp = 32.dp,
    minItemCountForScroll: Int = 15,
    minScrollRangeForDrag: Int = 4,
    headerItems: Int = 0    // <== Pass your header count here
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    val animatedThumbY = remember { Animatable(0f) }

    val isScrollable by remember {
        derivedStateOf {
            val layoutInfo = scrollState.layoutInfo
            val total = layoutInfo.totalItemsCount
            val visible = layoutInfo.visibleItemsInfo.size
            val contentCount = total - headerItems
            contentCount > minItemCountForScroll && contentCount > visible
        }
    }

    if (!isScrollable) return

    var lastTargetIndex by remember { mutableIntStateOf(-1) }
    var lastTargetOffset by remember { mutableIntStateOf(-1) }

    BoxWithConstraints(
        modifier = modifier
            .width(trackWidth)
            .fillMaxHeight()
            .pointerInput(scrollState) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        lastTargetIndex = -1 // reset
                        lastTargetOffset = -1
                    },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false }
                ) { change, _ ->
                    val layoutInfo = scrollState.layoutInfo
                    val visibleItems = layoutInfo.visibleItemsInfo
                    if (visibleItems.isEmpty()) return@detectDragGestures

                    val totalContentItems = layoutInfo.totalItemsCount - headerItems
                    val maxScrollIndex = max(1, totalContentItems - visibleItems.size)

                    if (maxScrollIndex > minScrollRangeForDrag) {
                        val touchProgress = (change.position.y / size.height).coerceIn(0f, 1f)
                        val targetFractionalIndex = touchProgress * maxScrollIndex
                        val targetIndex = headerItems + targetFractionalIndex.toInt()
                        val targetFraction = targetFractionalIndex - targetFractionalIndex.toInt()

                        val avgItemHeightPx = visibleItems.first().size
                        val targetOffset = (targetFraction * avgItemHeightPx).toInt()
                        val clampedIndex =
                            targetIndex.coerceIn(headerItems, layoutInfo.totalItemsCount - 1)

                        if (clampedIndex != lastTargetIndex || targetOffset != lastTargetOffset) {
                            lastTargetIndex = clampedIndex
                            lastTargetOffset = targetOffset
                            coroutineScope.launch {
                                scrollState.scrollToItem(clampedIndex)
                            }
                        }
                    }
                }
            }
    ) {
    val viewportHeight = with(density) { this@BoxWithConstraints.maxHeight.toPx() }
        val constThumbHeight = with(density) { thumbHeight.toPx() }

        val targetThumbY by remember {
            derivedStateOf {
                val layoutInfo = scrollState.layoutInfo
                val visibleItems = layoutInfo.visibleItemsInfo
                if (visibleItems.isEmpty()) return@derivedStateOf 0f

                val totalContentItems = layoutInfo.totalItemsCount - headerItems
                val maxScrollIndex = max(1, totalContentItems - visibleItems.size)
                if (maxScrollIndex <= minScrollRangeForDrag) return@derivedStateOf 0f

                val firstItem = visibleItems.first()
                val rawIndex = (scrollState.firstVisibleItemIndex - headerItems).coerceAtLeast(0)
                val firstItemOffsetProgress =
                    if (firstItem.size > 0) scrollState.firstVisibleItemScrollOffset.toFloat() / firstItem.size
                    else 0f

                val granularCurrentIndex = rawIndex + firstItemOffsetProgress

                val scrollProgress = granularCurrentIndex / maxScrollIndex

                val maxThumbY = viewportHeight - constThumbHeight
                (scrollProgress * maxThumbY).coerceIn(0f, maxThumbY)
            }
        }

        LaunchedEffect(targetThumbY, isDragging) {
            if (isDragging) {
                animatedThumbY.snapTo(targetThumbY)
            } else {
                animatedThumbY.animateTo(
                    targetValue = targetThumbY,
                    animationSpec = spring(stiffness = 300f)
                )
            }
        }

        Canvas(
            modifier = Modifier
                .width(thumbWidth)
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
        ) {
            val color = if (isDragging) thumbColorActive else thumbColor

            val cornerRadiusPx = thumbCornerRadius.toPx()

            drawRoundRect(
                color = color,
                topLeft = Offset(0f, animatedThumbY.value),
                size = Size(this.size.width, constThumbHeight),
                cornerRadius = CornerRadius(cornerRadiusPx)
            )
        }
    }
}