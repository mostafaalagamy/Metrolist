package com.metrolist.music.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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


import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import com.metrolist.music.ui.screens.settings.LyricsPosition
import com.metrolist.music.constants.LyricsTextPositionKey
import com.metrolist.music.ui.menu.LyricsMenu
import com.metrolist.music.ui.theme.PlayerSliderColors
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsScreen(
    mediaMetadata: MediaMetadata,
    lyrics: LyricsEntity?,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val player = playerConnection.player
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadataProvider = { mediaMetadata }
    val lyricsProvider = { lyrics }
    
    // Handle back button press
    BackHandler(onBack = onBackClick)
    
    var position by remember { mutableLongStateOf(0L) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableLongStateOf(C.TIME_UNSET) }
    
    val playerBackground by rememberEnumPreference(PlayerBackgroundStyleKey, PlayerBackgroundStyle.DEFAULT)
    val lyricsTextPosition by rememberPreference(LyricsTextPositionKey, LyricsPosition.CENTER.name)
    val colorScheme = MaterialTheme.colorScheme
    
    // Color logic from Player.kt
    val textColor = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> colorScheme.onBackground
        PlayerBackgroundStyle.BLUR -> Color.White
        PlayerBackgroundStyle.GRADIENT -> Color.White
        else -> colorScheme.onBackground
    }
    
    // Text alignment based on lyrics position setting
    val textAlign = when (lyricsTextPosition) {
        LyricsPosition.LEFT.name -> TextAlign.Start
        LyricsPosition.RIGHT.name -> TextAlign.End
        else -> TextAlign.Center
    }
    
    // Update position and duration
    position = player.currentPosition
    duration = player.duration
    if (duration != C.TIME_UNSET) {
        sliderPosition = position.toFloat() / duration
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Background Layer - same logic as Player.kt
        Box(modifier = Modifier.fillMaxSize()) {
            when (playerBackground) {
                PlayerBackgroundStyle.BLUR -> {
                    AsyncImage(
                        model = mediaMetadata.thumbnailUrl,
                        contentDescription = "Blurred background",
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxSize().blur(radius = 150.dp)
                    )
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
                }
                PlayerBackgroundStyle.GRADIENT -> {
                    // Solid gradient without any transparency
                    val gradientColors = arrayOf(
                        0.0f to colorScheme.primary,
                        0.6f to colorScheme.primary,
                        1.0f to Color.Black
                    )
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colorStops = gradientColors)))
                }
                else -> {
                    // DEFAULT - solid background
                    Box(modifier = Modifier.fillMaxSize().background(colorScheme.surface))
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.systemBars.asPaddingValues())
        ) {
            // Header with all controls moved up
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
                        contentDescription = "Back",
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
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Track info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
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
                        color = textColor.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Lyrics menu button with smaller circle background
                var showLyricsMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(
                        onClick = { showLyricsMenu = true },
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = textColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(50)
                            )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = "Lyrics options",
                            tint = textColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    if (showLyricsMenu) {
                        LyricsMenu(
                            lyricsProvider = lyricsProvider,
                            mediaMetadataProvider = mediaMetadataProvider,
                            onDismiss = { showLyricsMenu = false }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Lyrics content - simple text display with position settings
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (lyrics?.lyrics?.isNotEmpty() == true) {
                    Text(
                        text = lyrics.lyrics,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 20.sp,
                            lineHeight = 28.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = textColor,
                        textAlign = textAlign,
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    )
                } else {
                    Text(
                        text = "No lyrics available",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = textColor.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            // Bottom player controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Progress slider
                Slider(
                    value = sliderPosition,
                    onValueChange = { newValue ->
                        sliderPosition = newValue
                        if (duration != C.TIME_UNSET) {
                            val newPosition = (newValue * duration).toLong()
                            player.seekTo(newPosition)
                        }
                    },
                    colors = PlayerSliderColors.defaultSliderColors(textColor),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Control buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous button
                    IconButton(
                        onClick = { player.seekToPrevious() }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_previous),
                            contentDescription = "Previous",
                            tint = textColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    // Play/Pause button
                    IconButton(
                        onClick = { player.togglePlayPause() }
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isPlaying) R.drawable.pause else R.drawable.play
                            ),
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = textColor,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    
                    // Next button
                    IconButton(
                        onClick = { player.seekToNext() }
                    ) {
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