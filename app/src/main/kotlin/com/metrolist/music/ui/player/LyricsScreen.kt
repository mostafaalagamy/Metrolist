package com.metrolist.music.ui.player

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
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
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_READY
import androidx.palette.graphics.Palette
import androidx.core.graphics.drawable.toBitmap
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.size.Size
import coil3.toBitmap
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.constants.SliderStyle
import com.metrolist.music.constants.SliderStyleKey
import com.metrolist.music.constants.ThumbnailCornerRadius
import com.metrolist.music.db.entities.LyricsEntity
import com.metrolist.music.extensions.togglePlayPause
import com.metrolist.music.extensions.toggleRepeatMode
import com.metrolist.music.lyrics.LyricsHelper
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.component.Lyrics
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.PlayerSliderTrack
import com.metrolist.music.ui.component.BigSeekBar
import androidx.navigation.NavController
import me.saket.squiggles.SquigglySlider
import com.metrolist.music.ui.menu.LyricsMenu
import com.metrolist.music.ui.theme.PlayerColorExtractor
import com.metrolist.music.ui.theme.PlayerSliderColors
import com.metrolist.music.utils.rememberEnumPreference
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.runCatching
import com.metrolist.music.utils.makeTimeString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsScreen(
    mediaMetadata: MediaMetadata,
    onBackClick: () -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val player = playerConnection.player
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()

    val playbackState by playerConnection.playbackState.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()
    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState()
    val playerVolume = playerConnection.service.playerVolume.collectAsState()
    
    // slider style preference
    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.DEFAULT)
    val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)

    // Auto-fetch lyrics when no lyrics found (same logic as refetch)
    LaunchedEffect(mediaMetadata.id, currentLyrics) {
        if (currentLyrics == null) {
            // Small delay to ensure database state is stable
            delay(500)
            
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    // Get LyricsHelper from Hilt
                    val entryPoint = EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        com.metrolist.music.di.LyricsHelperEntryPoint::class.java
                    )
                    val lyricsHelper = entryPoint.lyricsHelper()
                    
                    // Fetch lyrics automatically
                    val lyrics = lyricsHelper.getLyrics(mediaMetadata)
                    
                    // Save to database
                    database.query {
                        upsert(LyricsEntity(mediaMetadata.id, lyrics))
                    }
                } catch (e: Exception) {
                    // Handle error silently - user can manually refetch if needed
                }
            }
        }
    }

    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(C.TIME_UNSET) }
    var sliderPosition by remember { mutableStateOf<Long?>(null) }

    val playerBackground by rememberEnumPreference(PlayerBackgroundStyleKey, PlayerBackgroundStyle.DEFAULT)

    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val gradientColorsCache = remember { mutableMapOf<String, List<Color>>() }

    val defaultGradientColors = listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant)
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()

    LaunchedEffect(mediaMetadata.id, playerBackground) {
        if (playerBackground == PlayerBackgroundStyle.GRADIENT) {
            if (mediaMetadata.thumbnailUrl != null) {
                val cachedColors = gradientColorsCache[mediaMetadata.id]
                if (cachedColors != null) {
                    gradientColors = cachedColors
                } else {
                    val request = ImageRequest.Builder(context)
                        .data(mediaMetadata.thumbnailUrl)
                        .size(Size(PlayerColorExtractor.Config.IMAGE_SIZE, PlayerColorExtractor.Config.IMAGE_SIZE))
                        .allowHardware(false)
                        .memoryCacheKey("gradient_${mediaMetadata.id}")
                        .build()

                    val result = runCatching {
                        context.imageLoader.execute(request).image
                    }.getOrNull()
                    
                    if (result != null) {
                        val bitmap = result.toBitmap()
                        val palette = withContext(Dispatchers.Default) {
                            Palette.from(bitmap)
                                .maximumColorCount(PlayerColorExtractor.Config.MAX_COLOR_COUNT)
                                .resizeBitmapArea(PlayerColorExtractor.Config.BITMAP_AREA)
                                .generate()
                        }
                        
                        val extractedColors = PlayerColorExtractor.extractGradientColors(
                            palette = palette,
                            fallbackColor = fallbackColor
                        )
                        
                        gradientColorsCache[mediaMetadata.id] = extractedColors
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

    val textBackgroundColor = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onBackground
        PlayerBackgroundStyle.BLUR -> Color.White
        PlayerBackgroundStyle.GRADIENT -> Color.White
    }

    val icBackgroundColor = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.surface
        PlayerBackgroundStyle.BLUR -> Color.Black
        PlayerBackgroundStyle.GRADIENT -> Color.Black
    }

    LaunchedEffect(playbackState) {
        if (playbackState == STATE_READY) {
            while (isActive) {
                delay(100)
                position = player.currentPosition
                duration = player.duration
            }
        }
    }

    BackHandler(onBack = onBackClick)

    Box(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (playerBackground) {
                PlayerBackgroundStyle.BLUR -> {
                    if (mediaMetadata.thumbnailUrl != null) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = mediaMetadata.thumbnailUrl,
                                contentDescription = "Blurred background",
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier.fillMaxSize().blur(radius = 150.dp)
                            )
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
                        }
                    }
                }
                PlayerBackgroundStyle.GRADIENT -> {
                    if (gradientColors.isNotEmpty()) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            val gradientColorStops = if (gradientColors.size >= 3) {
                                arrayOf(
                                    0.0f to gradientColors[0],
                                    0.5f to gradientColors[1],
                                    1.0f to gradientColors[2]
                                )
                            } else {
                                arrayOf(
                                    0.0f to gradientColors[0],
                                    0.6f to gradientColors[0].copy(alpha = 0.7f),
                                    1.0f to Color.Black
                                )
                            }
                            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colorStops = gradientColorStops)))
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))
                        }
                    }
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface))
                }
            }

            if (playerBackground != PlayerBackgroundStyle.DEFAULT) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.systemBars.asPaddingValues())
        ) {
            // Header with More button and Down arrow on opposite sides
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Down arrow button (left)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(
                                bounded = true,
                                radius = 16.dp
                            )
                        ) { onBackClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.expand_more),
                        contentDescription = "Close",
                        tint = textBackgroundColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // Centered content
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Now Playing",
                        style = MaterialTheme.typography.titleMedium,
                        color = textBackgroundColor
                    )
                    Text(
                        text = mediaMetadata.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textBackgroundColor.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // More button (right)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(
                                bounded = true,
                                radius = 16.dp
                            )
                        ) {
                            menuState.show {
                                LyricsMenu(
                                    lyricsProvider = { currentLyrics },
                                    mediaMetadataProvider = { mediaMetadata },
                                    onDismiss = menuState::dismiss
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_horiz),
                        contentDescription = "More options",
                        tint = textBackgroundColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Lyrics(
                    sliderPositionProvider = { sliderPosition }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 16.dp)
            ) {
                when (sliderStyle) {
                    SliderStyle.DEFAULT -> {
                        Slider(
                            value = (sliderPosition ?: position).toFloat(),
                            valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                            onValueChange = {
                                sliderPosition = it.toLong()
                            },
                            onValueChangeFinished = {
                                sliderPosition?.let {
                                    player.seekTo(it)
                                    position = it
                                }
                                sliderPosition = null
                            },
                            colors = PlayerSliderColors.defaultSliderColors(textBackgroundColor),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    SliderStyle.SQUIGGLY -> {
                        SquigglySlider(
                            value = (sliderPosition ?: position).toFloat(),
                            valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                            onValueChange = {
                                sliderPosition = it.toLong()
                            },
                            onValueChangeFinished = {
                                sliderPosition?.let {
                                    player.seekTo(it)
                                    position = it
                                }
                                sliderPosition = null
                            },
                            colors = PlayerSliderColors.squigglySliderColors(textBackgroundColor),
                            modifier = Modifier.fillMaxWidth(),
                            squigglesSpec = SquigglySlider.SquigglesSpec(
                                amplitude = if (isPlaying) (2.dp).coerceAtLeast(2.dp) else 0.dp,
                                strokeWidth = 3.dp,
                            )
                        )
                    }
                    SliderStyle.SLIM -> {
                        Slider(
                            value = (sliderPosition ?: position).toFloat(),
                            valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                            onValueChange = {
                                sliderPosition = it.toLong()
                            },
                            onValueChangeFinished = {
                                sliderPosition?.let {
                                    player.seekTo(it)
                                    position = it
                                }
                                sliderPosition = null
                            },
                            thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                            track = { sliderState ->
                                PlayerSliderTrack(
                                    sliderState = sliderState,
                                    colors = PlayerSliderColors.slimSliderColors(textBackgroundColor)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Time display below slider
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = makeTimeString(sliderPosition ?: position),
                        style = MaterialTheme.typography.bodySmall,
                        color = textBackgroundColor.copy(alpha = 0.7f)
                    )
                    Text(
                        text = if (duration != C.TIME_UNSET) makeTimeString(duration) else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = textBackgroundColor.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Optimized control buttons for better fit
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp), // Reduced padding
                    horizontalArrangement = Arrangement.SpaceEvenly, // Even distribution
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Repeat button with clear state indication
                    IconButton(
                        onClick = { playerConnection.player.toggleRepeatMode() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                when (repeatMode) {
                                    Player.REPEAT_MODE_OFF, 
                                    Player.REPEAT_MODE_ALL -> R.drawable.repeat
                                    Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                                    else -> R.drawable.repeat
                                }
                            ),
                            contentDescription = when (repeatMode) {
                                Player.REPEAT_MODE_OFF -> "Repeat Off"
                                Player.REPEAT_MODE_ALL -> "Repeat All"
                                Player.REPEAT_MODE_ONE -> "Repeat One"
                                else -> "Repeat"
                            },
                            tint = if (repeatMode == Player.REPEAT_MODE_OFF) {
                                // Inactive state - low opacity
                                textBackgroundColor.copy(alpha = 0.4f)
                            } else {
                                // Active state - full brightness
                                textBackgroundColor
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Previous button
                    IconButton(
                        onClick = { player.seekToPrevious() },
                        modifier = Modifier.size(40.dp) // Slightly smaller
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_previous),
                            contentDescription = "Previous",
                            tint = textBackgroundColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Play/Pause button (largest)
                    IconButton(
                        onClick = { player.togglePlayPause() },
                        modifier = Modifier.size(56.dp) // Slightly smaller but still prominent
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isPlaying) R.drawable.pause else R.drawable.play
                            ),
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = textBackgroundColor,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Next button
                    IconButton(
                        onClick = { player.seekToNext() },
                        modifier = Modifier.size(40.dp) // Slightly smaller
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_next),
                            contentDescription = "Next",
                            tint = textBackgroundColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Shuffle button with clear state indication
                    IconButton(
                        onClick = { playerConnection.player.shuffleModeEnabled = !shuffleModeEnabled },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.shuffle),
                            contentDescription = if (shuffleModeEnabled) "Shuffle On" else "Shuffle Off",
                            tint = if (shuffleModeEnabled) {
                                // Active state - full brightness
                                textBackgroundColor
                            } else {
                                // Inactive state - low opacity
                                textBackgroundColor.copy(alpha = 0.4f)
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp)) // Proper spacing

                // Volume Control
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        painter = painterResource(R.drawable.volume_off),
                        contentDescription = "Minimum Volume",
                        modifier = Modifier.size(20.dp),
                        tint = textBackgroundColor
                    )

                    BigSeekBar(
                        progressProvider = playerVolume::value,
                        onProgressChange = { playerConnection.service.playerVolume.value = it },
                        color = textBackgroundColor,
                        modifier = Modifier
                            .weight(1f)
                            .height(24.dp)
                            .padding(horizontal = 16.dp)
                    )

                    Icon(
                        painter = painterResource(R.drawable.volume_up),
                        contentDescription = "Maximum Volume",
                        modifier = Modifier.size(20.dp),
                        tint = textBackgroundColor
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
