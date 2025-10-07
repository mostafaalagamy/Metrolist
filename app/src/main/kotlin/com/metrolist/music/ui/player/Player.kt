package com.metrolist.music.ui.player

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.Player.STATE_READY
import androidx.palette.graphics.Palette
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.metrolist.music.LocalDownloadUtil
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.DarkModeKey
import com.metrolist.music.constants.UseNewPlayerDesignKey
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.constants.PlayerButtonsStyle
import com.metrolist.music.constants.PlayerButtonsStyleKey
import com.metrolist.music.ui.theme.PlayerColorExtractor
import com.metrolist.music.ui.theme.PlayerSliderColors
import com.metrolist.music.constants.PlayerHorizontalPadding
import com.metrolist.music.constants.QueuePeekHeight
import com.metrolist.music.constants.SliderStyle
import com.metrolist.music.constants.SliderStyleKey
import com.metrolist.music.extensions.togglePlayPause
import com.metrolist.music.extensions.toggleRepeatMode
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.component.BottomSheet
import com.metrolist.music.ui.component.BottomSheetState
import com.metrolist.music.ui.component.LocalBottomSheetPageState
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.PlayerSliderTrack
import com.metrolist.music.ui.component.ResizableIconButton
import com.metrolist.music.ui.component.rememberBottomSheetState
import com.metrolist.music.ui.menu.PlayerMenu
import com.metrolist.music.ui.screens.settings.DarkMode
import com.metrolist.music.ui.utils.ShowMediaInfo
import com.metrolist.music.utils.makeTimeString
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import me.saket.squiggles.SquigglySlider
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetPlayer(
    state: BottomSheetState,
    navController: NavController,
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val (useNewPlayerDesign, onUseNewPlayerDesignChange) = rememberPreference(
        UseNewPlayerDesignKey,
        defaultValue = true
    )
    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.DEFAULT
    )
    val playerButtonsStyle by rememberEnumPreference(
        key = PlayerButtonsStyleKey,
        defaultValue = PlayerButtonsStyle.DEFAULT
    )

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }
    val onBackgroundColor = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.secondary
        else ->
            if (useDarkTheme)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onPrimary
    }
    val useBlackBackground =
        remember(isSystemInDarkTheme, darkTheme, pureBlack) {
            val useDarkTheme =
                if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
            useDarkTheme && pureBlack
        }

    val playbackState by playerConnection.playbackState.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val automix by playerConnection.service.automixItems.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.DEFAULT)

    var position by rememberSaveable(playbackState) {
        mutableLongStateOf(playerConnection.player.currentPosition)
    }
    var duration by rememberSaveable(playbackState) {
        mutableLongStateOf(playerConnection.player.duration)
    }
    var sliderPosition by remember {
        mutableStateOf<Long?>(null)
    }
    var gradientColors by remember {
        mutableStateOf<List<Color>>(emptyList())
    }
    val gradientColorsCache = remember { mutableMapOf<String, List<Color>>() }

    if (!canSkipNext && automix.isNotEmpty()) {
        playerConnection.service.addToQueueAutomix(automix[0], 0)
    }

    val defaultGradientColors = listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant)
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()

    LaunchedEffect(mediaMetadata?.id, playerBackground) {
        if (playerBackground == PlayerBackgroundStyle.GRADIENT) {
            val currentMetadata = mediaMetadata
            if (currentMetadata != null && currentMetadata.thumbnailUrl != null) {
                val cachedColors = gradientColorsCache[currentMetadata.id]
                if (cachedColors != null) {
                    gradientColors = cachedColors
                    return@LaunchedEffect
                }
                withContext(Dispatchers.IO) {
                    val request = ImageRequest.Builder(context)
                        .data(currentMetadata.thumbnailUrl)
                        .size(100, 100)
                        .allowHardware(false)
                        .memoryCacheKey("gradient_${currentMetadata.id}")
                        .build()

                    val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
                    if (result != null) {
                        val bitmap = result.image?.toBitmap()
                        if (bitmap != null) {
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
                            gradientColorsCache[currentMetadata.id] = extractedColors
                            withContext(Dispatchers.Main) { gradientColors = extractedColors }
                        }
                    }
                }
            }
        } else {
            gradientColors = emptyList()
        }
    }

    val TextBackgroundColor =
        when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onBackground
            PlayerBackgroundStyle.BLUR -> Color.White
            PlayerBackgroundStyle.GRADIENT -> Color.White
        }

    val icBackgroundColor =
        when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.surface
            PlayerBackgroundStyle.BLUR -> Color.Black
            PlayerBackgroundStyle.GRADIENT -> Color.Black
        }

    val (textButtonColor, iconButtonColor) = when (playerButtonsStyle) {
        PlayerButtonsStyle.DEFAULT -> Pair(TextBackgroundColor, icBackgroundColor)
        PlayerButtonsStyle.SECONDARY -> Pair(
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.onSecondary
        )
    }

    val download by LocalDownloadUtil.current.getDownload(mediaMetadata?.id ?: "")
        .collectAsState(initial = null)

    val sleepTimerEnabled =
        remember(
            playerConnection.service.sleepTimer.triggerTime,
            playerConnection.service.sleepTimer.pauseWhenSongEnd
        ) {
            playerConnection.service.sleepTimer.isActive
        }

    var sleepTimerTimeLeft by remember {
        mutableLongStateOf(0L)
    }

    LaunchedEffect(sleepTimerEnabled) {
        if (sleepTimerEnabled) {
            while (isActive) {
                sleepTimerTimeLeft =
                    if (playerConnection.service.sleepTimer.pauseWhenSongEnd) {
                        playerConnection.player.duration - playerConnection.player.currentPosition
                    } else {
                        playerConnection.service.sleepTimer.triggerTime - System.currentTimeMillis()
                    }
                delay(1000L)
            }
        }
    }

    var showSleepTimerDialog by remember {
        mutableStateOf(false)
    }

    var sleepTimerValue by remember {
        mutableFloatStateOf(30f)
    }
    if (showSleepTimerDialog) {
        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { showSleepTimerDialog = false },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.bedtime),
                    contentDescription = null
                )
            },
            title = { Text(stringResource(R.string.sleep_timer)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSleepTimerDialog = false
                        playerConnection.service.sleepTimer.start(sleepTimerValue.roundToInt())
                    },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSleepTimerDialog = false },
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.minute,
                            sleepTimerValue.roundToInt(),
                            sleepTimerValue.roundToInt()
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    Slider(
                        value = sleepTimerValue,
                        onValueChange = { sleepTimerValue = it },
                        valueRange = 5f..120f,
                        steps = (120 - 5) / 5 - 1,
                    )

                    OutlinedIconButton(
                        onClick = {
                            showSleepTimerDialog = false
                            playerConnection.service.sleepTimer.start(-1)
                        },
                    ) {
                        Text(stringResource(R.string.end_of_song))
                    }
                }
            },
        )
    }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    LaunchedEffect(playbackState) {
        if (playbackState == STATE_READY) {
            while (isActive) {
                delay(500)
                position = playerConnection.player.currentPosition
                duration = playerConnection.player.duration
            }
        }
    }

    val dismissedBound = QueuePeekHeight + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()

    val queueSheetState = rememberBottomSheetState(
        dismissedBound = dismissedBound,
        expandedBound = state.expandedBound,
        collapsedBound = dismissedBound + 1.dp,
        initialAnchor = 1
    )

    val lyricsSheetState = rememberBottomSheetState(
        dismissedBound = 0.dp,
        expandedBound = state.expandedBound,
        collapsedBound = 0.dp,
        initialAnchor = 1
    )

    val bottomSheetBackgroundColor = when (playerBackground) {
        PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT -> 
            MaterialTheme.colorScheme.surfaceContainer
        else -> 
            if (useBlackBackground) Color.Black 
            else MaterialTheme.colorScheme.surfaceContainer
    }

    val backgroundAlpha = state.progress.coerceIn(0f, 1f)

    BottomSheet(
        state = state,
        modifier = modifier,
        background = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bottomSheetBackgroundColor)
            ) {
                when (playerBackground) {
                    PlayerBackgroundStyle.BLUR -> {
                        AnimatedContent(
                            targetState = mediaMetadata?.thumbnailUrl,
                            transitionSpec = {
                                fadeIn(tween(800)).togetherWith(fadeOut(tween(800)))
                            },
                            label = "blurBackground"
                        ) { thumbnailUrl ->
                            if (thumbnailUrl != null) {
                                Box(modifier = Modifier.alpha(backgroundAlpha)) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(thumbnailUrl)
                                            .size(100, 100)
                                            .allowHardware(false)
                                            .build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .blur(if (useDarkTheme) 150.dp else 100.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.3f))
                                    )
                                }
                            }
                        }
                    }
                    PlayerBackgroundStyle.GRADIENT -> {
                        AnimatedContent(
                            targetState = gradientColors,
                            transitionSpec = {
                                fadeIn(tween(800)).togetherWith(fadeOut(tween(800)))
                            },
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
                                    Modifier
                                        .fillMaxSize()
                                        .alpha(backgroundAlpha)
                                        .background(Brush.verticalGradient(colorStops = gradientColorStops))
                                        .background(Color.Black.copy(alpha = 0.2f))
                                )
                            }
                        }
                    }
                    else -> {
                        PlayerBackgroundStyle.DEFAULT
                    }
                }
            }
        },
        onDismiss = {
            playerConnection.service.clearAutomix()
            playerConnection.player.stop()
            playerConnection.player.clearMediaItems()
        },
        collapsedContent = {
            MiniPlayer(
                position = position,
                duration = duration,
                pureBlack = pureBlack,
            )
        },
    ) {
        val controlsContent: @Composable ColumnScope.(MediaMetadata) -> Unit = { mediaMetadata ->
            val playPauseRoundness by animateDpAsState(
                targetValue = if (isPlaying) 24.dp else 36.dp,
                animationSpec = tween(durationMillis = 90, easing = LinearEasing),
                label = "playPauseRoundness",
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding),
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    AnimatedContent(
                        targetState = mediaMetadata.title,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "",
                    ) { title ->
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = TextBackgroundColor,
                            modifier =
                            Modifier
                                .basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp)
                                .combinedClickable(
                                    enabled = true,
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = {
                                        if (mediaMetadata.album != null) {
                                            navController.navigate("album/${mediaMetadata.album.id}")
                                            state.collapseSoft()
                                        }
                                    },
                                    onLongClick = {
                                        val clip = ClipData.newPlainText("Copied Title", title)
                                        clipboardManager.setPrimaryClip(clip)
                                        Toast
                                            .makeText(context, "Copied Title", Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                )
                            ,
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    if (mediaMetadata.artists.any { it.name.isNotBlank() }) {
                        val annotatedString = buildAnnotatedString {
                            mediaMetadata.artists.forEachIndexed { index, artist ->
                                val tag = "artist_${artist.id.orEmpty()}"
                                pushStringAnnotation(tag = tag, annotation = artist.id.orEmpty())
                                withStyle(SpanStyle(color = TextBackgroundColor, fontSize = 16.sp)) {
                                    append(artist.name)
                                }
                                pop()
                                if (index != mediaMetadata.artists.lastIndex) append(", ")
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp)
                                .padding(end = 12.dp)
                        ) {
                            var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                            var clickOffset by remember { mutableStateOf<Offset?>(null) }
                            Text(
                                text = annotatedString,
                                style = MaterialTheme.typography.titleMedium.copy(color = TextBackgroundColor),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                onTextLayout = { layoutResult = it },
                                modifier = Modifier
                                    .pointerInput(Unit) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val tapPosition = event.changes.firstOrNull()?.position
                                                if (tapPosition != null) {
                                                    clickOffset = tapPosition
                                                }
                                            }
                                        }
                                    }
                                    .combinedClickable(
                                        enabled = true,
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() },
                                        onClick = {
                                            val tapPosition = clickOffset
                                            val layout = layoutResult
                                            if (tapPosition != null && layout != null) {
                                                val offset = layout.getOffsetForPosition(tapPosition)
                                                annotatedString
                                                    .getStringAnnotations(offset, offset)
                                                    .firstOrNull()
                                                    ?.let { ann ->
                                                        val artistId = ann.item
                                                        if (artistId.isNotBlank()) {
                                                            navController.navigate("artist/$artistId")
                                                            state.collapseSoft()
                                                        }
                                                    }
                                            }
                                        },
                                        onLongClick = {
                                            val clip =
                                                ClipData.newPlainText("Copied Artist", annotatedString)
                                            clipboardManager.setPrimaryClip(clip)
                                            Toast
                                                .makeText(
                                                    context,
                                                    "Copied Artist",
                                                    Toast.LENGTH_SHORT
                                                )
                                                .show()
                                        }
                                    )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                if (useNewPlayerDesign) {
                    val shareShape = RoundedCornerShape(
                        topStart = 50.dp, bottomStart = 50.dp,
                        topEnd = 5.dp, bottomEnd = 5.dp
                    )

                    val favShape = RoundedCornerShape(
                        topStart = 5.dp, bottomStart = 5.dp,
                        topEnd = 50.dp, bottomEnd = 50.dp
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(shareShape)
                                .background(textButtonColor)
                                .clickable {
                                    val intent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "https://music.youtube.com/watch?v=${mediaMetadata.id}"
                                        )
                                    }
                                    context.startActivity(Intent.createChooser(intent, null))
                                }
                        ) {
                            Image(
                                painter = painterResource(R.drawable.share),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(iconButtonColor),
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(24.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(favShape)
                                .background(textButtonColor)
                                .clickable {
                                    playerConnection.toggleLike()
                                }
                        ) {
                            Image(
                                painter = painterResource(
                                    if (currentSong?.song?.liked == true)
                                        R.drawable.favorite
                                    else R.drawable.favorite_border
                                ),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(iconButtonColor),
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(24.dp)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier =
                        Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(textButtonColor)
                            .clickable {
                                val intent =
                                    Intent().apply {
                                        action = Intent.ACTION_SEND
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "https://music.youtube.com/watch?v=${mediaMetadata.id}"
                                        )
                                    }
                                context.startActivity(Intent.createChooser(intent, null))
                            },
                    ) {
                        Image(
                            painter = painterResource(R.drawable.share),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(iconButtonColor),
                            modifier =
                            Modifier
                                .align(Alignment.Center)
                                .size(24.dp),
                        )
                    }

                    Spacer(modifier = Modifier.size(12.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                        Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(textButtonColor)
                            .clickable {
                                menuState.show {
                                    PlayerMenu(
                                        mediaMetadata = mediaMetadata,
                                        navController = navController,
                                        playerBottomSheetState = state,
                                        onShowDetailsDialog = {
                                            mediaMetadata.id.let {
                                                bottomSheetPageState.show {
                                                    ShowMediaInfo(it)
                                                }
                                            }
                                        },
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            },
                    ) {
                        Image(
                            painter = painterResource(R.drawable.more_horiz),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(iconButtonColor),
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

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
                                playerConnection.player.seekTo(it)
                                position = it
                            }
                            sliderPosition = null
                        },
                        colors = PlayerSliderColors.defaultSliderColors(textButtonColor, playerBackground, useDarkTheme),
                        modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
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
                                playerConnection.player.seekTo(it)
                                position = it
                            }
                            sliderPosition = null
                        },
                        colors = PlayerSliderColors.squigglySliderColors(textButtonColor, playerBackground, useDarkTheme),
                        modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
                        squigglesSpec =
                        SquigglySlider.SquigglesSpec(
                            amplitude = if (isPlaying) (2.dp).coerceAtLeast(2.dp) else 0.dp,
                            strokeWidth = 3.dp,
                        ),
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
                                playerConnection.player.seekTo(it)
                                position = it
                            }
                            sliderPosition = null
                        },
                        thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                        track = { sliderState ->
                            PlayerSliderTrack(
                                sliderState = sliderState,
                                colors = PlayerSliderColors.slimSliderColors(textButtonColor, playerBackground, useDarkTheme)
                            )
                        },
                        modifier = Modifier.padding(horizontal = PlayerHorizontalPadding)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding + 4.dp),
            ) {
                Text(
                    text = makeTimeString(sliderPosition ?: position),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextBackgroundColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = if (duration != C.TIME_UNSET) makeTimeString(duration) else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextBackgroundColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(12.dp))

            if (useNewPlayerDesign) {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val maxW = maxWidth
                    val playButtonHeight = maxW / 6f
                    val playButtonWidth = playButtonHeight * 1.6f
                    val sideButtonHeight = playButtonHeight * 0.8f
                    val sideButtonWidth = sideButtonHeight * 1.3f

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        FilledTonalIconButton(
                            onClick = playerConnection::seekToPrevious,
                            enabled = canSkipPrevious,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = textButtonColor,
                                contentColor = iconButtonColor
                            ),
                            modifier = Modifier
                                .size(width = sideButtonWidth, height = sideButtonHeight)
                                .clip(RoundedCornerShape(32.dp))
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.skip_previous),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        FilledIconButton(
                            onClick = {
                                if (playbackState == STATE_ENDED) {
                                    playerConnection.player.seekTo(0, 0)
                                    playerConnection.player.playWhenReady = true
                                } else {
                                    playerConnection.player.togglePlayPause()
                                }
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = textButtonColor,
                                contentColor = iconButtonColor
                            ),
                            modifier = Modifier
                                .size(width = playButtonWidth, height = playButtonHeight)
                                .clip(RoundedCornerShape(32.dp))
                        ) {
                            Icon(
                                painter = painterResource(
                                    when {
                                        playbackState == STATE_ENDED -> R.drawable.replay
                                        isPlaying -> R.drawable.pause
                                        else -> R.drawable.play
                                    }
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(42.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        FilledTonalIconButton(
                            onClick = playerConnection::seekToNext,
                            enabled = canSkipNext,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = textButtonColor,
                                contentColor = iconButtonColor
                            ),
                            modifier = Modifier
                                .size(width = sideButtonWidth, height = sideButtonHeight)
                                .clip(RoundedCornerShape(32.dp))
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.skip_next),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PlayerHorizontalPadding),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        ResizableIconButton(
                            icon = when (repeatMode) {
                                Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ALL -> R.drawable.repeat
                                Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                                else -> throw IllegalStateException()
                            },
                            color = TextBackgroundColor,
                            modifier = Modifier
                                .size(32.dp)
                                .padding(4.dp)
                                .align(Alignment.Center)
                                .alpha(if (repeatMode == Player.REPEAT_MODE_OFF) 0.5f else 1f),
                            onClick = {
                                playerConnection.player.toggleRepeatMode()
                            },
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        ResizableIconButton(
                            icon = R.drawable.skip_previous,
                            enabled = canSkipPrevious,
                            color = TextBackgroundColor,
                            modifier =
                            Modifier
                                .size(32.dp)
                                .align(Alignment.Center),
                            onClick = playerConnection::seekToPrevious,
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Box(
                        modifier =
                        Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(playPauseRoundness))
                            .background(textButtonColor)
                            .clickable {
                                if (playbackState == STATE_ENDED) {
                                    playerConnection.player.seekTo(0, 0)
                                    playerConnection.player.playWhenReady = true
                                } else {
                                    playerConnection.player.togglePlayPause()
                                }
                            },
                    ) {
                        Image(
                            painter =
                            painterResource(
                                if (playbackState ==
                                    STATE_ENDED
                                ) {
                                    R.drawable.replay
                                } else if (isPlaying) {
                                    R.drawable.pause
                                } else {
                                    R.drawable.play
                                },
                            ),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(iconButtonColor),
                            modifier =
                            Modifier
                                .align(Alignment.Center)
                                .size(36.dp),
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        ResizableIconButton(
                            icon = R.drawable.skip_next,
                            enabled = canSkipNext,
                            color = TextBackgroundColor,
                            modifier =
                            Modifier
                                .size(32.dp)
                                .align(Alignment.Center),
                            onClick = playerConnection::seekToNext,
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        ResizableIconButton(
                            icon = if (currentSong?.song?.liked == true) R.drawable.favorite else R.drawable.favorite_border,
                            color = if (currentSong?.song?.liked == true) MaterialTheme.colorScheme.error else TextBackgroundColor,
                            modifier =
                            Modifier
                                .size(32.dp)
                                .padding(4.dp)
                                .align(Alignment.Center),
                            onClick = playerConnection::toggleLike,
                        )
                    }
                }
            }
        }

        when (LocalConfiguration.current.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                Row(
                    modifier =
                    Modifier
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                        .padding(bottom = queueSheetState.collapsedBound + 48.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.weight(1f),
                    ) {
                        val screenWidth = LocalConfiguration.current.screenWidthDp
                        val thumbnailSize = (screenWidth * 0.4).dp
                        Thumbnail(
                            sliderPositionProvider = { sliderPosition },
                            modifier = Modifier.size(thumbnailSize),
                            isPlayerExpanded = state.isExpanded
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier =
                        Modifier
                            .weight(1f)
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top)),
                    ) {
                        Spacer(Modifier.weight(1f))

                        mediaMetadata?.let {
                            controlsContent(it)
                        }

                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            else -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier =
                    Modifier
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                        .padding(bottom = queueSheetState.collapsedBound),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.weight(1f),
                    ) {
                        Thumbnail(
                            sliderPositionProvider = { sliderPosition },
                            modifier = Modifier.nestedScroll(state.preUpPostDownNestedScrollConnection),
                            isPlayerExpanded = state.isExpanded
                        )
                    }

                    mediaMetadata?.let {
                        controlsContent(it)
                    }

                    Spacer(Modifier.height(30.dp))
                }
            }
        }

        Queue(
            state = queueSheetState,
            playerBottomSheetState = state,
            navController = navController,
            background =
            if (useBlackBackground) {
                Color.Black
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
            onBackgroundColor = onBackgroundColor,
            TextBackgroundColor = TextBackgroundColor,
            textButtonColor = textButtonColor,
            iconButtonColor = iconButtonColor,
            onShowLyrics = { lyricsSheetState.expandSoft() },
            pureBlack = pureBlack,
        )

        mediaMetadata?.let { metadata ->
            BottomSheet(
                state = lyricsSheetState,
                background = { Box(Modifier.fillMaxSize().background(Color.Unspecified)) },
                onDismiss = { },
                collapsedContent = {
                }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.surface.copy(
                                alpha = lyricsSheetState.progress.coerceIn(0f, 1f)
                            )
                        )
                ) {
                    LyricsScreen(
                        mediaMetadata = metadata,
                        onBackClick = { lyricsSheetState.collapseSoft() },
                        navController = navController,
                        backgroundAlpha = lyricsSheetState.progress.coerceIn(0f, 1f)
                    )
                }
            }
        }
    }
}
