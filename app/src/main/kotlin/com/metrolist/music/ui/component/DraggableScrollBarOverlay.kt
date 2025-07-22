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
    trackWidth: Dp = 32.dp
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    val animatedThumbY = remember { Animatable(0f) }

    val isScrollable by remember {
        derivedStateOf {
            scrollState.layoutInfo.totalItemsCount > scrollState.layoutInfo.visibleItemsInfo.size
        }
    }

    if (!isScrollable) return

    BoxWithConstraints(
        modifier = modifier
            .width(trackWidth)
            .fillMaxHeight()
            .pointerInput(scrollState) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false }
                ) { change, _ ->
                    val layoutInfo = scrollState.layoutInfo
                    val maxScrollIndex =
                        max(0, layoutInfo.totalItemsCount - layoutInfo.visibleItemsInfo.size)
                    if (maxScrollIndex > 0) {
                        val touchProgress = (change.position.y / size.height).coerceIn(0f, 1f)
                        val targetIndex = (touchProgress * maxScrollIndex).toInt()
                        coroutineScope.launch {
                            scrollState.scrollToItem(targetIndex)
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
                val totalItems = layoutInfo.totalItemsCount
                val maxScrollIndex = max(0, totalItems - layoutInfo.visibleItemsInfo.size)

                val scrollProgress = if (maxScrollIndex > 0) {
                    scrollState.firstVisibleItemIndex.toFloat() / maxScrollIndex
                } else 0f

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