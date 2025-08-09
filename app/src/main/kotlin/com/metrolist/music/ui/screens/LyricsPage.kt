package com.metrolist.music.ui.screens

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
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import androidx.palette.graphics.Palette
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.constants.SliderStyleKey
import com.metrolist.music.constants.PlayerColorExtractor
import com.metrolist.music.extensions.togglePlayPause
import com.metrolist.music.extensions.toggleRepeatMode
import com.metrolist.music.extensions.toggleShuffleMode
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.playback.queues.Queue
import com.metrolist.music.ui.component.Lyrics
import com.metrolist.music.ui.component.LocalBottomSheetPageState
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.PlayerSliderTrack
import com.metrolist.music.ui.menu.PlayerMenu
import com.metrolist.music.ui.utils.ShowMediaInfo
import com.metrolist.music.utils.makeTimeString
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.enums.PlayerBackgroundStyle
import com.metrolist.music.enums.SliderStyle
import androidx.compose.animation.core.SpringSpec

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsPage(
    mediaId: String,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()
    val shuffleMode by playerConnection.shuffleMode.collectAsState()
    
    // Return if wrong media is playing
    if (mediaMetadata?.id != mediaId) {
        LaunchedEffect(Unit) {
            navController.navigateUp()
        }
        return
    }
    
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(C.TIME_UNSET) }
    var sliderPosition by remember { mutableStateOf<Long?>(null) }

    val playerBackground by rememberEnumPreference(PlayerBackgroundStyleKey, PlayerBackgroundStyle.DEFAULT)
    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.DEFAULT)

    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val gradientColorsCache = remember { mutableMapOf<String, List<Color>>() }

    val defaultGradientColors = listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant)
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()

    LaunchedEffect(mediaMetadata?.id, playerBackground) {
        if (playerBackground == PlayerBackgroundStyle.GRADIENT) {
            if (mediaMetadata?.thumbnailUrl != null) {
                val cachedColors = gradientColorsCache[mediaMetadata!!.id]
                if (cachedColors != null) {
                    gradientColors = cachedColors
                } else {
                    val request = ImageRequest.Builder(context)
                        .data(mediaMetadata!!.thumbnailUrl)
                        .size(Size(PlayerColorExtractor.Config.IMAGE_SIZE, PlayerColorExtractor.Config.IMAGE_SIZE))
                        .allowHardware(false)
                        .memoryCacheKey("gradient_${mediaMetadata!!.id}")
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
                        
                        val colors = PlayerColorExtractor.extractGradientColors(palette, fallbackColor)
                        gradientColors = colors
                        gradientColorsCache[mediaMetadata!!.id] = colors
                    } else {
                        gradientColors = defaultGradientColors
                    }
                }
            } else {
                gradientColors = defaultGradientColors
            }
        }
    }

    LaunchedEffect(playerConnection.player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    duration = playerConnection.player.duration.takeIf { it != C.TIME_UNSET } ?: 0L
                }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                position = newPosition.positionMs
            }
        }

        playerConnection.player.addListener(listener)
        kotlinx.coroutines.delay(100)
        
        while (true) {
            if (sliderPosition == null) {
                position = playerConnection.player.currentPosition
            }
            kotlinx.coroutines.delay(100)
        }
    }

    BackHandler {
        navController.navigateUp()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (playerBackground) {
                PlayerBackgroundStyle.BLUR -> {
                    if (mediaMetadata?.thumbnailUrl != null) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = mediaMetadata?.thumbnailUrl,
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
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.navigateUp() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = "Back",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Now Playing",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    mediaMetadata?.title?.let { title ->
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = {
                        mediaMetadata?.id?.let {
                            bottomSheetPageState.show {
                                ShowMediaInfo(it)
                            }
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_vert),
                        contentDescription = "More options",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Lyrics
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

            // Slider
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 16.dp)
            ) {
                when (sliderStyle) {
                    SliderStyle.DEFAULT -> {
                        Slider(
                            value = sliderPosition?.toFloat() ?: position.toFloat(),
                            valueRange = 0f..(duration.takeIf { it != C.TIME_UNSET }?.toFloat() ?: 1f),
                            onValueChange = { sliderPosition = it.toLong() },
                            onValueChangeFinished = {
                                sliderPosition?.let { pos ->
                                    playerConnection.player.seekTo(pos)
                                    position = pos
                                    sliderPosition = null
                                }
                            },
                            track = { PlayerSliderTrack(it) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    SliderStyle.SQUIGGLY -> {
                        PlayerSliderTrack(
                            sliderPositions = androidx.compose.material3.SliderPositions(
                                activeRange = 0f..(sliderPosition?.toFloat() ?: position.toFloat()) / (duration.takeIf { it != C.TIME_UNSET }?.toFloat() ?: 1f)
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = makeTimeString(sliderPosition ?: position),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = makeTimeString(duration.takeIf { it != C.TIME_UNSET } ?: 0L),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Control buttons
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { playerConnection.player.toggleRepeatMode() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                when (repeatMode) {
                                    Player.REPEAT_MODE_OFF -> R.drawable.repeat
                                    Player.REPEAT_MODE_ONE -> R.drawable.repeat_one_on
                                    Player.REPEAT_MODE_ALL -> R.drawable.repeat_on
                                    else -> R.drawable.repeat
                                }
                            ),
                            contentDescription = "Repeat",
                            modifier = Modifier.size(20.dp),
                            tint = when (repeatMode) {
                                Player.REPEAT_MODE_OFF -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    }

                    IconButton(
                        onClick = { playerConnection.player.seekToPreviousMediaItem() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_previous),
                            contentDescription = "Previous",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = { playerConnection.player.togglePlayPause() },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = { playerConnection.player.seekToNextMediaItem() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_next),
                            contentDescription = "Next",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = { playerConnection.player.toggleShuffleMode() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.shuffle),
                            contentDescription = "Shuffle",
                            modifier = Modifier.size(20.dp),
                            tint = if (shuffleMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            mediaMetadata?.id?.let {
                                bottomSheetPageState.show {
                                    ShowMediaInfo(it)
                                }
                            }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.info),
                            contentDescription = "Details",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = {
                            // Navigate to queue or show queue
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.queue_music),
                            contentDescription = "Queue",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        if (menuState.isVisible) {
            mediaMetadata?.let { metadata ->
                PlayerMenu(
                    mediaMetadata = metadata,
                    navController = navController,
                    onDismiss = menuState::dismiss,
                )
            }
        }
    }
}