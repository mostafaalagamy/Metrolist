package com.metrolist.music.ui.utils

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer

fun Modifier.horizontalFade(
    isWordActive: Boolean,
    durationMillis: Int
): Modifier = composed {
    val animatedProgress = remember { Animatable(0f) }

    if (isWordActive) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = LinearEasing
            )
        )
    }

    this
        .graphicsLayer(alpha = 0.99f)
        .drawWithContent {
            val brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, Color.Black),
                startX = 0f,
                endX = animatedProgress.value * size.width
            )
            drawContent()
            drawRect(
                brush = brush,
                blendMode = BlendMode.DstIn
            )
        }
}