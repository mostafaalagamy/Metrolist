/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VolumeSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    val interactionSource = remember { MutableInteractionSource() }

    val volumeOffIcon = rememberVectorPainter(Icons.AutoMirrored.Filled.VolumeOff)
    val volumeMuteIcon = rememberVectorPainter(Icons.AutoMirrored.Filled.VolumeMute)
    val volumeDownIcon = rememberVectorPainter(Icons.AutoMirrored.Filled.VolumeDown)
    val volumeUpIcon = rememberVectorPainter(Icons.AutoMirrored.Filled.VolumeUp)

    val currentIcon = when {
        value <= 0f -> volumeOffIcon
        value < 0.33f -> volumeMuteIcon
        value < 0.66f -> volumeDownIcon
        else -> volumeUpIcon
    }

    val colors = SliderDefaults.colors(
        thumbColor = accentColor,
        activeTrackColor = accentColor,
        activeTickColor = MaterialTheme.colorScheme.onPrimary,
        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
        inactiveTickColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = 0f..1f,
        onValueChangeFinished = onValueChangeFinished,
        colors = colors,
        interactionSource = interactionSource,
        track = { sliderState ->
            val iconSize = DpSize(20.dp, 20.dp)
            val iconPadding = 10.dp
            val thumbTrackGapSize = 6.dp
            val activeIconColor = colors.activeTickColor
            val inactiveIconColor = colors.inactiveTickColor

            SliderDefaults.Track(
                sliderState = sliderState,
                modifier = Modifier
                    .height(36.dp)
                    .drawWithContent {
                        drawContent()
                        val yOffset = size.height / 2 - iconSize.toSize().height / 2
                        val fraction = value.coerceIn(0f, 1f)
                        val activeTrackEnd = size.width * fraction - thumbTrackGapSize.toPx()
                        val inactiveTrackStart = activeTrackEnd + thumbTrackGapSize.toPx() * 2
                        val activeTrackWidth = activeTrackEnd
                        val inactiveTrackWidth = size.width - inactiveTrackStart

                        drawVolumeIcon(
                            icon = currentIcon,
                            iconSize = iconSize,
                            iconPadding = iconPadding,
                            yOffset = yOffset,
                            activeTrackWidth = activeTrackWidth,
                            inactiveTrackStart = inactiveTrackStart,
                            inactiveTrackWidth = inactiveTrackWidth,
                            activeIconColor = activeIconColor,
                            inactiveIconColor = inactiveIconColor,
                            volumeOffIcon = volumeOffIcon
                        )
                    },
                colors = colors,
                enabled = enabled,
                thumbTrackGapSize = thumbTrackGapSize,
                trackCornerSize = 12.dp,
                drawStopIndicator = null
            )
        }
    )
}

private fun DrawScope.drawVolumeIcon(
    icon: VectorPainter,
    iconSize: DpSize,
    iconPadding: androidx.compose.ui.unit.Dp,
    yOffset: Float,
    activeTrackWidth: Float,
    inactiveTrackStart: Float,
    inactiveTrackWidth: Float,
    activeIconColor: Color,
    inactiveIconColor: Color,
    volumeOffIcon: VectorPainter
) {
    val iconSizePx = iconSize.toSize()
    val iconPaddingPx = iconPadding.toPx()
    val minSpaceForIcon = iconSizePx.width + iconPaddingPx * 2

    // Draw icon on active track if there's enough space
    if (activeTrackWidth >= minSpaceForIcon) {
        translate(iconPaddingPx, yOffset) {
            with(icon) {
                draw(iconSizePx, colorFilter = ColorFilter.tint(activeIconColor))
            }
        }
    }
    // Otherwise draw icon on inactive track if there's enough space
    else if (inactiveTrackWidth >= minSpaceForIcon) {
        translate(inactiveTrackStart + iconPaddingPx, yOffset) {
            with(volumeOffIcon) {
                draw(iconSizePx, colorFilter = ColorFilter.tint(inactiveIconColor))
            }
        }
    }
}
