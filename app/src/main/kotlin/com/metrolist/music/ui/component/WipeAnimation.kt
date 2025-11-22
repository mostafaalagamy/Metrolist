package com.metrolist.music.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.metrolist.music.lyrics.Word

@Composable
fun WipeAnimation(
    word: Word,
    currentPosition: Long,
    textColor: Color,
    fadedColor: Color
) {
    val isWordActive = currentPosition in word.startTime..word.endTime
    val translationY by animateFloatAsState(
        targetValue = if (isWordActive) -10f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "translationY"
    )
    val duration = word.endTime - word.startTime
    val glow = duration > 1200 && (duration / word.text.length) > 150
    val animatedGlow by animateFloatAsState(
        targetValue = if (glow && isWordActive) {
            val progress = (currentPosition - word.startTime).toFloat() / (duration / 2f)
            if (progress < 1f) progress else 1f
        } else {
            0f
        },
        animationSpec = spring(),
        label = "glow"
    )

    Box {
        Text(
            text = word.text,
            fontSize = 28.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Transparent
        )
        Row {
            word.syllables.forEach { syllable ->
                val progress = if (currentPosition in syllable.startTime..syllable.endTime) {
                    val duration = (syllable.endTime - syllable.startTime).toFloat()
                    if (duration > 0) {
                        ((currentPosition - syllable.startTime) / duration).coerceIn(0f, 1f)
                    } else {
                        1f
                    }
                } else {
                    if (currentPosition > syllable.endTime) 1f else 0f
                }
                Text(
                    text = syllable.text,
                    fontSize = 28.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .graphicsLayer {
                            this.translationY = translationY
                            this.shadowElevation = animatedGlow * 24f
                        }
                        .drawWithContent {
                            val brush = Brush.horizontalGradient(
                                colors = listOf(textColor, fadedColor),
                                startX = 0f,
                                endX = size.width * progress
                            )
                            drawContent()
                            drawRect(
                                brush = brush,
                            )
                        }
                )
            }
        }
    }
}