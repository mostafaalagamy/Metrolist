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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.palette.graphics.Palette
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Size
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.extensions.togglePlayPause
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.component.Lyrics
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.menu.LyricsMenu
import com.metrolist.music.ui.theme.PlayerColorExtractor
import com.metrolist.music.ui.theme.PlayerSliderColors
import com.metrolist.music.utils.rememberEnumPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.runCatching

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsScreen(
    mediaMetadata: MediaMetadata,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val player = playerConnection.player
    val context = LocalContext.current
    val menuState = LocalMenuState.current

    // Get current lyrics from playerConnection (like the original system)
    val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)

    var position by remember { mutableLongStateOf(0L) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableLongStateOf(C.TIME_UNSET) }

    val playerBackground by rememberEnumPreference(PlayerBackgroundStyleKey, PlayerBackgroundStyle.DEFAULT)
    val colorScheme = MaterialTheme.colorScheme

    // Complete background logic from Player.kt - EXACT COPY
    var gradientColors by remember {
        mutableStateOf<List<Color>>(emptyList())
    }
    
    // Previous background states for smooth transitions
    var previousThumbnailUrl by remember { mutableStateOf<String?>(null) }
    var previousGradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    
    // Cache for gradient colors to prevent re-extraction for same songs
    val gradientColorsCache = remember { mutableMapOf<String, List<Color>>() }

    // Default gradient colors for fallback
    val defaultGradientColors = listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant)
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()
    
    // Update previous states when media changes
    LaunchedEffect(mediaMetadata.id) {
        val currentThumbnail = mediaMetadata.thumbnailUrl
        if (currentThumbnail != previousThumbnailUrl) {
            previousThumbnailUrl = currentThumbnail
            previousGradientColors = gradientColors
        }
    }
    
    LaunchedEffect(mediaMetadata.id, playerBackground) {
        if (playerBackground == PlayerBackgroundStyle.GRADIENT) {
            val currentMetadata = mediaMetadata
            if (currentMetadata.thumbnailUrl != null) {
                // Check cache first
                val cachedColors = gradientColorsCache[currentMetadata.id]
                if (cachedColors != null) {
                    gradientColors = cachedColors
                } else {
                    val request = ImageRequest.Builder(context)
                        .data(currentMetadata.thumbnailUrl)
                        .size(Size(PlayerColorExtractor.Config.IMAGE_SIZE, PlayerColorExtractor.Config.IMAGE_SIZE))
                        .allowHardware(false)
                        .memoryCacheKey("gradient_${currentMetadata.id}")
                        .build()

                    val result = runCatching { 
                        context.imageLoader.execute(request).drawable 
                    }.getOrNull()
                    
                    if (result != null) {
                        val bitmap = result.toBitmap()
                        val palette = withContext(Dispatchers.Default) {
                            Palette.Builder(bitmap)
                                .maximumColorCount(PlayerColorExtractor.Config.MAX_COLOR_COUNT)
                                .resizeBitmapArea(PlayerColorExtractor.Config.BITMAP_AREA)
                                .generate()
                        }
                        
                        // Use the new color extraction system
                        val extractedColors = PlayerColorExtractor.extractGradientColors(
                            palette = palette,
                            fallbackColor = fallbackColor
                        )
                        
                        // Cache the extracted colors
                        gradientColorsCache[currentMetadata.id] = extractedColors
                        gradientColors = extractedColors
                    } else {
                        gradientColors = defaultGradientColors
                    }
                }
            } else {
                gradientColors = emptyList()
            }
        } else {
            gradientColors = emptyList()
        }
    }

    // Color logic exactly from Player.kt
    val TextBackgroundColor =
        when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onBackground
            PlayerBackgroundStyle.BLUR -> Color.White
            PlayerBackgroundStyle.GRADIENT -> Color.White
            else -> MaterialTheme.colorScheme.onBackground
        }

    val icBackgroundColor =
        when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.surface
            PlayerBackgroundStyle.BLUR -> Color.Black
            PlayerBackgroundStyle.GRADIENT -> Color.Black
            else -> MaterialTheme.colorScheme.surface
        }

    // Update position and duration
    position = player.currentPosition
    duration = player.duration.coerceAtLeast(0L)
    sliderPosition = if (duration > 0) position.toFloat() / duration else 0f

    BackHandler(onBack = onBackClick)

    Box(modifier = modifier.fillMaxSize()) {
        // Background Layer - EXACT COPY from Player.kt
        Box(modifier = Modifier.fillMaxSize()) {
            when (playerBackground) {
                PlayerBackgroundStyle.BLUR -> {
                    // Layer 1: Previous blur background (stays visible during transition)
                    if (previousThumbnailUrl != null) {
                        AsyncImage(
                            model = previousThumbnailUrl,
                            contentDescription = "Previous blurred background",
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier.fillMaxSize().blur(radius = 150.dp)
                        )
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
                    }
                    
                    // Layer 2: New blur background (animates on top)
                    AnimatedContent(
                        targetState = mediaMetadata.thumbnailUrl,
                        transitionSpec = {
                            fadeIn(tween(500)) togetherWith fadeOut(tween(500))
                        }
                    ) { thumbnailUrl ->
                        if (thumbnailUrl != null) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = thumbnailUrl,
                                    contentDescription = "New blurred background",
                                    contentScale = ContentScale.FillBounds,
                                    modifier = Modifier.fillMaxSize().blur(radius = 150.dp)
                                )
                                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
                            }
                        }
                    }
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
                    // DEFAULT or other modes - no background
                }
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
                        tint = TextBackgroundColor,
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
                        color = TextBackgroundColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = mediaMetadata.artists.joinToString { it.name },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextBackgroundColor.copy(alpha = 0.8f),
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
                                lyricsProvider = { currentLyrics },
                                mediaMetadataProvider = { mediaMetadata },
                                onDismiss = menuState::dismiss
                            )
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_horiz),
                        contentDescription = "More options",
                        tint = TextBackgroundColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Lyrics content in center with proper alignment
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center // Center the lyrics content
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
                    colors = PlayerSliderColors.defaultSliderColors(TextBackgroundColor),
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
                            tint = TextBackgroundColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Play/Pause button
                    IconButton(onClick = { player.togglePlayPause() }) {
                        Icon(
                            painter = painterResource(if (player.isPlaying) R.drawable.pause else R.drawable.play),
                            contentDescription = if (player.isPlaying) "Pause" else "Play",
                            tint = TextBackgroundColor,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    // Next button
                    IconButton(onClick = { player.seekToNext() }) {
                        Icon(
                            painter = painterResource(R.drawable.skip_next),
                            contentDescription = "Next",
                            tint = TextBackgroundColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}