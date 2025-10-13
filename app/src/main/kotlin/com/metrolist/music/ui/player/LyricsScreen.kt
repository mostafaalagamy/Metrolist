package com.metrolist.music.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import android.app.Activity
import android.content.res.Configuration
import android.view.WindowManager
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.db.entities.LyricsEntity
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.Lyrics
import com.metrolist.music.ui.menu.LyricsMenu
import com.metrolist.music.ui.theme.PlayerColorExtractor
import com.metrolist.music.utils.rememberEnumPreference
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LyricsScreen(
    mediaMetadata: MediaMetadata,
    onBackClick: () -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier,
    backgroundAlpha: Float = 1f
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // Keep screen on while lyrics are displayed
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()

    val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)

    // Fetch lyrics if not available
    LaunchedEffect(mediaMetadata.id, currentLyrics) {
        if (currentLyrics == null) {
            delay(500)
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val entryPoint = EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        com.metrolist.music.di.LyricsHelperEntryPoint::class.java
                    )
                    val lyricsHelper = entryPoint.lyricsHelper()
                    val lyrics = lyricsHelper.getLyrics(mediaMetadata)
                    database.query {
                        upsert(LyricsEntity(mediaMetadata.id, lyrics))
                    }
                } catch (e: Exception) {
                    // Handle lyrics fetching error
                }
            }
        }
    }

    val playerBackground by rememberEnumPreference(PlayerBackgroundStyleKey, PlayerBackgroundStyle.DEFAULT)
    val useDarkTheme = isSystemInDarkTheme()

    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val gradientColorsCache = remember { mutableMapOf<String, List<Color>>() }
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()

    // Handle dynamic background based on album art
    LaunchedEffect(mediaMetadata.id, playerBackground) {
        if (playerBackground == PlayerBackgroundStyle.GRADIENT && mediaMetadata.thumbnailUrl != null) {
            val cachedColors = gradientColorsCache[mediaMetadata.id]
            if (cachedColors != null) {
                gradientColors = cachedColors
                return@LaunchedEffect
            }
            withContext(Dispatchers.IO) {
                val request = ImageRequest.Builder(context)
                    .data(mediaMetadata.thumbnailUrl)
                    .size(100, 100)
                    .allowHardware(false)
                    .memoryCacheKey("gradient_${mediaMetadata.id}")
                    .build()
                val result = runCatching { context.imageLoader.execute(request).image }.getOrNull()
                if (result != null) {
                    val bitmap = result.toBitmap()
                    val palette = withContext(Dispatchers.Default) {
                        Palette.from(bitmap)
                            .maximumColorCount(8)
                            .resizeBitmapArea(100 * 100)
                            .generate()
                    }
                    val extractedColors = PlayerColorExtractor.extractGradientColors(
                        palette = palette,
                        fallbackColor = fallbackColor
                    )
                    gradientColorsCache[mediaMetadata.id] = extractedColors
                    withContext(Dispatchers.Main) { gradientColors = extractedColors }
                }
            }
        } else {
            gradientColors = emptyList()
        }
    }

    val textBackgroundColor = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onBackground
        PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT -> Color.White
    }

    BackHandler(onBack = onBackClick)

    Box(modifier = modifier.fillMaxSize()) {
        // Background Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(backgroundAlpha)
        ) {
            when (playerBackground) {
                PlayerBackgroundStyle.BLUR -> {
                    AnimatedContent(
                        targetState = mediaMetadata.thumbnailUrl,
                        transitionSpec = { fadeIn(tween(800)).togetherWith(fadeOut(tween(800))) },
                        label = "blurBackground"
                    ) { thumbnailUrl ->
                        if (thumbnailUrl != null) {
                            AsyncImage(
                                model = thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .blur(if (useDarkTheme) 150.dp else 100.dp)
                            )
                        }
                    }
                }
                PlayerBackgroundStyle.GRADIENT -> {
                    AnimatedContent(
                        targetState = gradientColors,
                        transitionSpec = { fadeIn(tween(800)).togetherWith(fadeOut(tween(800))) },
                        label = "gradientBackground"
                    ) { colors ->
                        if (colors.isNotEmpty()) {
                            val gradientColorStops = if (colors.size >= 3) {
                                arrayOf(
                                    0.0f to colors[0],
                                    0.5f to colors[1],
                                    1.0f to colors[2]
                                )
                            } else {
                                arrayOf(
                                    0.0f to colors[0],
                                    0.6f to colors[0].copy(alpha = 0.7f),
                                    1.0f to Color.Black
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.verticalGradient(colorStops = gradientColorStops))
                            )
                        }
                    }
                }
                else -> { /* Default background is handled by the main theme */ }
            }

            if (playerBackground != PlayerBackgroundStyle.DEFAULT) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                )
            }
        }

        // UI Layer (Header and Lyrics)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .zIndex(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back Button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, radius = 16.dp),
                            onClick = onBackClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.expand_more),
                        contentDescription = stringResource(R.string.close),
                        tint = textBackgroundColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Title Info
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.now_playing),
                        style = MaterialTheme.typography.titleMedium,
                        color = textBackgroundColor
                    )
                    Text(
                        text = mediaMetadata.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = textBackgroundColor.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // More Options Button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, radius = 16.dp)
                        ) {
                            menuState.show {
                                LyricsMenu(
                                    lyricsProvider = { currentLyrics },
                                    songProvider = { currentSong?.song },
                                    mediaMetadataProvider = { mediaMetadata },
                                    onDismiss = menuState::dismiss
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_horiz),
                        contentDescription = stringResource(R.string.more_options),
                        tint = textBackgroundColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Lyrics Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    Alignment.Center
                } else {
                    Alignment.TopCenter
                }
            ) {
                Lyrics(
                    sliderPositionProvider = { null },
                    showControls = false  // Hide controls in fullscreen mode
                )
            }

            // Add some space at the bottom so lyrics don't stick to the edge.
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
