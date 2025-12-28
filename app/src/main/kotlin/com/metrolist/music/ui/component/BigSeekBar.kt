/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.dp

@Composable
fun BigSeekBar(
    progressProvider: () -> Float,
    onProgressChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.13f),
    color: Color = MaterialTheme.colorScheme.primary,
) {
    var width by remember {
        mutableFloatStateOf(0f)
    }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .onPlaced {
                width = it.size.width.toFloat()
            }.pointerInput(progressProvider) {
                detectHorizontalDragGestures { _, dragAmount ->
                    onProgressChange(
                        (progressProvider() + dragAmount * 1.2f / width).coerceIn(
                            0f,
                            1f
                        )
                    )
                }
            },
    ) {
        drawRect(color = background)

        drawRect(
            color = color,
            size = size.copy(width = size.width * progressProvider()),
        )
    }
}
