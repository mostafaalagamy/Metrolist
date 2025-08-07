package com.metrolist.music.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import coil.compose.AsyncImage
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.db.entities.LyricsEntity
import com.metrolist.music.extensions.togglePlayPause
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.component.Lyrics
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.menu.LyricsMenu
import com.metrolist.music.ui.theme.PlayerSliderColors
import com.metrolist.music.utils.rememberEnumPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsScreen(
    mediaMetadata: MediaMetadata,
    lyrics: LyricsEntity?,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val player = playerConnection.player
    val context = LocalContext.current
    val menuState = LocalMenuState.current

    var position by remember { mutableLongStateOf(0L) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableLongStateOf(C.TIME_UNSET) }

    val playerBackground by rememberEnumPreference(PlayerBackgroundStyleKey, PlayerBackgroundStyle.DEFAULT)
    val colorScheme = MaterialTheme.colorScheme

    // Get gradient colors from player
    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    var previousGradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }

    // Color logic exactly from Player.kt
    val textColor = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> colorScheme.onBackground
        PlayerBackgroundStyle.BLUR -> Color.White
        PlayerBackgroundStyle.GRADIENT -> Color.White
        else -> colorScheme.onBackground
    }

    // Update position and duration
    position = player.currentPosition
    duration = player.duration.coerceAtLeast(0L)
    sliderPosition = if (duration > 0) position.toFloat() / duration else 0f

    // Extract gradient colors exactly like Player.kt
    LaunchedEffect(mediaMetadata.id, playerBackground) {
        if (playerBackground == PlayerBackgroundStyle.GRADIENT) {
            previousGradientColors = gradientColors
            // Use default colors for now - in real implementation this would extract from image
            val defaultGradientColors = listOf(
                colorScheme.primary,
                colorScheme.primary.copy(alpha = 0.7f),
                Color.Black
            )
            gradientColors = defaultGradientColors
        } else {
            gradientColors = emptyList()
        }
    }

    BackHandler(onBack = onBackClick)

    Box(modifier = modifier.fillMaxSize()) {
        // Background exactly from Player.kt
        when (playerBackground) {
            PlayerBackgroundStyle.BLUR -> {
                AsyncImage(
                    model = mediaMetadata.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(50.dp)
                )
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
            }
            PlayerBackgroundStyle.GRADIENT -> {
                // Layer 1: Previous gradient background (stays visible during transition)
                if (previousGradientColors.isNotEmpty()) {
                    val gradientColorStops = if (previousGradientColors.size >= 3) {
                        arrayOf(
                            0.0f to previousGradientColors[0], // Top: primary vibrant color
                            0.5f to previousGradientColors[1], // Middle: darker variant
                            1.0f to previousGradientColors[2]  // Bottom: black
                        )
                    } else {
                        arrayOf(
                            0.0f to previousGradientColors[0], // Top: primary color
                            0.6f to previousGradientColors[0].copy(alpha = 0.7f), // Middle: faded variant
                            1.0f to Color.Black // Bottom: black
                        )
                    }
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colorStops = gradientColorStops)))
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))
                }
                
                // Layer 2: New gradient background (animates on top)
                AnimatedContent(
                    targetState = gradientColors,
                    transitionSpec = {
                        fadeIn(tween(500)) togetherWith fadeOut(tween(500))
                    }
                ) { colors ->
                    if (colors.isNotEmpty()) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            val gradientColorStops = if (colors.size >= 3) {
                                arrayOf(
                                    0.0f to colors[0], // Top: primary vibrant color
                                    0.5f to colors[1], // Middle: darker variant
                                    1.0f to colors[2]  // Bottom: black
                                )
                            } else {
                                arrayOf(
                                    0.0f to colors[0], // Top: primary color
                                    0.6f to colors[0].copy(alpha = 0.7f), // Middle: faded variant
                                    1.0f to Color.Black // Bottom: black
                                )
                            }
                            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colorStops = gradientColorStops)))
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))
                        }
                    }
                }
            }
            else -> {
                // DEFAULT or other modes
                Box(modifier = Modifier.fillMaxSize().background(colorScheme.surface))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.systemBars.asPaddingValues())
        ) {
            // Header with thumbnail, track info, and more button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Close button
                IconButton(onClick = onBackClick) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = "Close",
                        tint = textColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Track thumbnail
                AsyncImage(
                    model = mediaMetadata.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(6.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Track info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mediaMetadata.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = mediaMetadata.artists.joinToString { it.name },
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // More button using bottom sheet like the original (no background circle)
                IconButton(
                    onClick = {
                        menuState.show {
                            LyricsMenu(
                                lyricsProvider = { lyrics },
                                mediaMetadataProvider = { mediaMetadata },
                                onDismiss = menuState::dismiss
                            )
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_horiz),
                        contentDescription = "More options",
                        tint = textColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Lyrics content using the original Lyrics component
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Lyrics(
                    sliderPositionProvider = { position }
                )
            }

            // Player controls at bottom with horizontal spacing
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 8.dp) // Increased horizontal padding
            ) {
                // Progress slider
                Slider(
                    value = sliderPosition,
                    onValueChange = { newValue ->
                        sliderPosition = newValue
                        val newPosition = (newValue * duration).toLong()
                        player.seekTo(newPosition)
                    },
                    colors = PlayerSliderColors.defaultSliderColors(textColor),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Control buttons with spacing
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp), // Additional spacing left and right
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous button
                    IconButton(onClick = { player.seekToPrevious() }) {
                        Icon(
                            painter = painterResource(R.drawable.skip_previous),
                            contentDescription = "Previous",
                            tint = textColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Play/Pause button
                    IconButton(onClick = { player.togglePlayPause() }) {
                        Icon(
                            painter = painterResource(if (player.isPlaying) R.drawable.pause else R.drawable.play),
                            contentDescription = if (player.isPlaying) "Pause" else "Play",
                            tint = textColor,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    // Next button
                    IconButton(onClick = { player.seekToNext() }) {
                        Icon(
                            painter = painterResource(R.drawable.skip_next),
                            contentDescription = "Next",
                            tint = textColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}