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
import androidx.compose.material3.SliderState
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
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
    val sliderState = rememberSliderState(
        value = value,
        valueRange = 0f..1f,
        onValueChangeFinished = onValueChangeFinished
    )
    
    // Update slider state when value changes externally
    sliderState.value = value
    
    val interactionSource = remember { MutableInteractionSource() }
    
    // Icons for different volume levels
    val volumeOffIcon = rememberVectorPainter(Icons.AutoMirrored.Filled.VolumeOff)
    val volumeMuteIcon = rememberVectorPainter(Icons.AutoMirrored.Filled.VolumeMute)
    val volumeDownIcon = rememberVectorPainter(Icons.AutoMirrored.Filled.VolumeDown)
    val volumeUpIcon = rememberVectorPainter(Icons.AutoMirrored.Filled.VolumeUp)
    
    val colors = SliderDefaults.colors(
        thumbColor = accentColor,
        activeTrackColor = accentColor,
        activeTickColor = accentColor.copy(alpha = 0.8f),
        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
        inactiveTickColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    )
    
    Slider(
        state = sliderState,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
        onValueChange = onValueChange,
        track = { state ->
            val iconSize = DpSize(20.dp, 20.dp)
            val iconPadding = 8.dp
            val thumbTrackGapSize = 6.dp
            val activeIconColor = colors.activeTickColor
            val inactiveIconColor = colors.inactiveTickColor
            
            // Choose icon based on volume level
            val currentIcon = when {
                state.value <= 0f -> volumeOffIcon
                state.value < 0.33f -> volumeMuteIcon
                state.value < 0.66f -> volumeDownIcon
                else -> volumeUpIcon
            }
            
            // Icon for when volume is at zero (show on inactive track)
            val zeroStateIcon = volumeOffIcon
            
            val trackIconStart: DrawScope.(Offset, Color, Boolean) -> Unit = { offset, color, isZeroState ->
                val iconToDraw = if (isZeroState) zeroStateIcon else currentIcon
                translate(offset.x + iconPadding.toPx(), offset.y) {
                    with(iconToDraw) {
                        draw(iconSize.toSize(), colorFilter = ColorFilter.tint(color))
                    }
                }
            }
            
            SliderDefaults.Track(
                sliderState = state,
                modifier = Modifier
                    .height(36.dp)
                    .drawWithContent {
                        drawContent()
                        val yOffset = size.height / 2 - iconSize.toSize().height / 2
                        val activeTrackStart = 0f
                        val activeTrackEnd = size.width * state.coercedValueAsFraction - thumbTrackGapSize.toPx()
                        val inactiveTrackStart = activeTrackEnd + thumbTrackGapSize.toPx() * 2
                        val activeTrackWidth = activeTrackEnd - activeTrackStart
                        val inactiveTrackWidth = size.width - inactiveTrackStart
                        
                        // Draw icon on active track if there's enough space
                        if (iconSize.toSize().width < activeTrackWidth - iconPadding.toPx() * 2) {
                            trackIconStart(Offset(activeTrackStart, yOffset), activeIconColor, false)
                        }
                        // Draw mute icon on inactive track when volume is very low
                        else if (state.value <= 0.1f && iconSize.toSize().width < inactiveTrackWidth - iconPadding.toPx() * 2) {
                            trackIconStart(Offset(inactiveTrackStart, yOffset), inactiveIconColor, true)
                        }
                    },
                colors = colors,
                enabled = enabled,
                trackCornerSize = 18.dp,
                drawStopIndicator = null,
                thumbTrackGapSize = thumbTrackGapSize
            )
        }
    )
}
