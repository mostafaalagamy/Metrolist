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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player.STATE_READY
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
    val coroutineScope = rememberCoroutineScope()
    val currentView = LocalView.current

    // Keep screen on for lyrics
    DisposableEffect(Unit) {
        currentView.keepScreenOn = true
        onDispose { currentView.keepScreenOn = false }
    }

    // استخدام نفس منطق المشغل لتتبع التقدم
    val playbackState by playerConnection.playbackState.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    
    // هذا هو المفتاح - نراقب currentLyrics مباشرة من playerConnection
    val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)

    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(C.TIME_UNSET) }
    var sliderPosition by remember { mutableStateOf<Long?>(null) }

    val playerBackground by rememberEnumPreference(PlayerBackgroundStyleKey, PlayerBackgroundStyle.DEFAULT)

    // نسخ منطق الألوان بالكامل من Player.kt
    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    var previousThumbnailUrl by remember { mutableStateOf<String?>(null) }
    var previousGradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val gradientColorsCache = remember { mutableMapOf<String, List<Color>>() }

    val defaultGradientColors = listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant)
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()

    // تحديث الحالات السابقة عند تغيير الأغنية
    LaunchedEffect(mediaMetadata.id) {
        val currentThumbnail = mediaMetadata.thumbnailUrl
        if (currentThumbnail != previousThumbnailUrl) {
            previousThumbnailUrl = currentThumbnail
            previousGradientColors = gradientColors
        }
    }

    // استخراج ألوان الـ gradient مع التخزين المؤقت
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
                        context.imageLoader.execute(request).drawable 
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

    // نسخ منطق الألوان بالكامل من Player.kt
    val textBackgroundColor = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onBackground
        PlayerBackgroundStyle.BLUR -> Color.White
        PlayerBackgroundStyle.GRADIENT -> Color.White
        else -> MaterialTheme.colorScheme.onBackground
    }

    // تتبع التقدم بنفس طريقة Player.kt
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
        // خلفية بنفس منطق Player.kt تماماً
        Box(modifier = Modifier.fillMaxSize()) {
            when (playerBackground) {
                PlayerBackgroundStyle.BLUR -> {
                    // الطبقة السابقة للانتقال السلس
                    if (previousThumbnailUrl != null) {
                        AsyncImage(
                            model = previousThumbnailUrl,
                            contentDescription = "Previous blurred background",
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier.fillMaxSize().blur(radius = 150.dp)
                        )
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
                    }
                    
                    // الطبقة الجديدة
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
                    // الطبقة السابقة
                    if (previousGradientColors.isNotEmpty()) {
                        val gradientColorStops = if (previousGradientColors.size >= 3) {
                            arrayOf(
                                0.0f to previousGradientColors[0],
                                0.5f to previousGradientColors[1],
                                1.0f to previousGradientColors[2]
                            )
                        } else {
                            arrayOf(
                                0.0f to previousGradientColors[0],
                                0.6f to previousGradientColors[0].copy(alpha = 0.7f),
                                1.0f to Color.Black
                            )
                        }
                        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colorStops = gradientColorStops)))
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))
                    }
                    
                    // الطبقة الجديدة
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
                                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colorStops = gradientColorStops)))
                                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))
                            }
                        }
                    }
                }
                else -> {
                    // الخلفية الافتراضية
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface))
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.systemBars.asPaddingValues())
        ) {
            // رأس الشاشة مع الصورة المصغرة ومعلومات الأغنية
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // زر الإغلاق
                IconButton(onClick = onBackClick) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = "Close",
                        tint = textBackgroundColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // صورة الأغنية
                AsyncImage(
                    model = mediaMetadata.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                )

                Spacer(modifier = Modifier.width(16.dp))

                // معلومات الأغنية
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mediaMetadata.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = textBackgroundColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = mediaMetadata.artists.joinToString { it.name },
                        style = MaterialTheme.typography.bodyMedium,
                        color = textBackgroundColor.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // زر المزيد
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
                        tint = textBackgroundColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // محتوى الكلمات في وسط الشاشة مع key لإعادة التحميل
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                // استخدام key للتأكد من إعادة تحميل Lyrics عند تغيير الأغنية
                androidx.compose.runtime.key(mediaMetadata.id) {
                    Lyrics(
                        sliderPositionProvider = { sliderPosition ?: position }
                    )
                }
            }

            // أدوات التحكم في الأسفل
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                // شريط التقدم
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

                Spacer(modifier = Modifier.height(16.dp))

                // أزرار التحكم
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // زر السابق
                    IconButton(onClick = { player.seekToPrevious() }) {
                        Icon(
                            painter = painterResource(R.drawable.skip_previous),
                            contentDescription = "Previous",
                            tint = textBackgroundColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // زر التشغيل/الإيقاف
                    IconButton(onClick = { player.togglePlayPause() }) {
                        Icon(
                            painter = painterResource(
                                if (isPlaying) R.drawable.pause else R.drawable.play
                            ),
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = textBackgroundColor,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    // زر التالي
                    IconButton(onClick = { player.seekToNext() }) {
                        Icon(
                            painter = painterResource(R.drawable.skip_next),
                            contentDescription = "Next",
                            tint = textBackgroundColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
