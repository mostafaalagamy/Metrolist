package com.metrolist.music.ui.player

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.BitmapDrawable
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator // Import M3 LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProgressIndicatorDefaults // Import for animation spec
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.StrokeCap // Import for StrokeCap
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
import androidx.core.graphics.ColorUtils
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.Player.STATE_READY
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.metrolist.music.LocalDownloadUtil
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.DarkModeKey
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.constants.PlayerButtonsStyle
import com.metrolist.music.constants.PlayerButtonsStyleKey
import com.metrolist.music.constants.PlayerHorizontalPadding
import com.metrolist.music.constants.QueuePeekHeight
import com.metrolist.music.constants.ShowLyricsKey
import com.metrolist.music.constants.SliderStyle
import com.metrolist.music.constants.SliderStyleKey
import com.metrolist.music.extensions.togglePlayPause
import com.metrolist.music.extensions.toggleRepeatMode
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.component.BottomSheet
import com.metrolist.music.ui.component.BottomSheetState
import com.metrolist.music.ui.component.LocalBottomSheetPageState
import com.metrolist.music.ui.component.LocalMenuState
// import com.metrolist.music.ui.component.PlayerSliderTrack // Remove import if replacing with LinearProgressIndicator
import com.metrolist.music.ui.component.ResizableIconButton
import com.metrolist.music.ui.component.rememberBottomSheetState
import com.metrolist.music.ui.menu.PlayerMenu
import com.metrolist.music.ui.screens.settings.DarkMode
import com.metrolist.music.ui.theme.extractGradientColors
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
    val backgroundColor = if (useBlackBackground && state.value > state.collapsedBound) {
        lerp(MaterialTheme.colorScheme.surfaceContainer, Color.Black, state.progress)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    val playbackState by playerConnection.playbackState.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val automix by playerConnection.service.automixItems.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()

    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()

    var showLyrics by rememberPreference(ShowLyricsKey, defaultValue = false)

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

    // Update position from player when not seeking
    LaunchedEffect(playerConnection.player) {
        while (isActive) {
            if (sliderPosition == null) { // Only update if user is not interacting with slider
                position = playerConnection.player.currentPosition
                duration = playerConnection.player.duration
            }
            delay(500) // Update interval
        }
    }

    var gradientColors by remember {
        mutableStateOf<List<Color>>(emptyList())
    }

    var changeColor by remember {
        mutableStateOf(false)
    }

    if (!canSkipNext && automix.isNotEmpty()) {
        playerConnection.service.addToQueueAutomix(automix[0], 0)
    }

    LaunchedEffect(mediaMetadata, playerBackground) {
        if (useBlackBackground && playerBackground != PlayerBackgroundStyle.BLUR) {
            gradientColors = listOf(Color.Black, Color.Black)
        }
        if (useBlackBackground && playerBackground != PlayerBackgroundStyle.GRADIENT) {
            gradientColors = listOf(Color.Black, Color.Black)
        } else if (playerBackground == PlayerBackgroundStyle.GRADIENT) {
            withContext(Dispatchers.IO) {
                val result =
                    (
                        ImageLoader(context)
                            .execute(
                                ImageRequest
                                    .Builder(context)
                                    .data(mediaMetadata?.thumbnailUrl)
                                    .allowHardware(false)
                                   .build(),
                            ).drawable as? BitmapDrawable
                        )?.bitmap?.extractGradientColors()

                result?.let {
                    gradientColors = it
                }
            }
        } else {
            gradientColors = emptyList()
        }
    }

    val changeBound = state.expandedBound / 3

    val TextBackgroundColor =
        when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onBackground
            PlayerBackgroundStyle.BLUR -> Color.White
            else -> {
                val whiteContrast =
                    if (gradientColors.size >= 2) {
                        ColorUtils.calculateContrast(
                            gradientColors.first().toArgb(),
                            Color.White.toArgb(),
                        )
                    } else {
                        2.0
                    }
                val blackContrast: Double =
                    if (gradientColors.size >= 2) {
                        ColorUtils.calculateContrast(
                            gradientColors.last().toArgb(),
                            Color.Black.toArgb(),
                        )
                    } else {
                        2.0
                    }
                if (gradientColors.size >= 2 &&
                    whiteContrast < 2f &&
                    blackContrast > 2f
                ) {
                    changeColor = true
                    Color.Black
                } else if (whiteContrast > 2f && blackContrast < 2f) {
                    changeColor = true
                    Color.White
                } else {
                    changeColor = false
                    Color.White
                }
            }
        }

    val icBackgroundColor =
        when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.surface
            PlayerBackgroundStyle.BLUR -> Color.Black
            else -> {
                val whiteContrast =
                    if (gradientColors.size >= 2) {
                        ColorUtils.calculateContrast(
                            gradientColors.first().toArgb(),
                            Color.White.toArgb(),
                        )
                    } else {
                        2.0
                    }
                val blackContrast: Double =
                    if (gradientColors.size >= 2) {
                        ColorUtils.calculateContrast(
                            gradientColors.last().toArgb(),
                            Color.Black.toArgb(),
                        )
                    } else {
                        2.0
                    }
                if (gradientColors.size >= 2 &&
                    whiteContrast < 2f &&
                    blackContrast > 2f
                ) {
                    changeColor = true
                    Color.White
                } else if (whiteContrast > 2f && blackContrast < 2f) {
                    changeColor = true
                    Color.Black
                } else {
                    changeColor = false
                    Color.Black
                }
            }
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
                TextButton(onClick = { showSleepTimerDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            text = {
                Column {
                    Text(
                        text = pluralStringResource(
                            R.plurals.minutes,
                            sleepTimerValue.roundToInt(),
                            sleepTimerValue.roundToInt()
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Slider(
                        value = sleepTimerValue,
                        onValueChange = { sleepTimerValue = it },
                        valueRange = 1f..120f,
                        steps = 118,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "1")
                        Text(text = "120")
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            playerConnection.service.sleepTimer.pauseWhenSongEnd =
                                !playerConnection.service.sleepTimer.pauseWhenSongEnd
                        }
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = playerConnection.service.sleepTimer.pauseWhenSongEnd,
                            onCheckedChange = { checked ->
                                playerConnection.service.sleepTimer.pauseWhenSongEnd = checked
                            }
                        )
                        Text(stringResource(R.string.pause_when_song_end))
                    }
                }
            }
        )
    }

    BottomSheet(
        state = state,
        modifier = modifier,
        backgroundColor = backgroundColor,
        onBackgroundColor = onBackgroundColor,
        collapsedContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                AsyncImage(
                    model =
                    ImageRequest
                        .Builder(context)
                        .data(mediaMetadata?.thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { state.expand() },
                )

                Spacer(Modifier.width(8.dp))

                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = mediaMetadata?.title ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee()
                    )
                    Text(
                        text = mediaMetadata?.artist ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee()
                    )
                }

                Spacer(Modifier.width(8.dp))

                ResizableIconButton(
                    onClick = { playerConnection.togglePlayPause() },
                    modifier = Modifier.size(48.dp),
                ) {
                    AnimatedContent(
                        targetState = isPlaying,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "",
                    ) {
                        Icon(
                            painter = painterResource(if (it) R.drawable.pause else R.drawable.play),
                            contentDescription = null,
                        )
                    }
                }
            }

            // M3 LinearProgressIndicator for collapsed state
            val progress = if (duration <= 0) 0f else position.toFloat() / duration
            LinearProgressIndicator(
                progress = { progress }, // Use lambda for progress
                modifier = Modifier.fillMaxWidth().height(2.dp), // Thin progress bar
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round // M3 style
            )
        },
    ) {
        val configuration = LocalConfiguration.current
        val horizontalPadding by animateDpAsState(
            targetValue = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 64.dp else PlayerHorizontalPadding,
            label = "",
        )

        Box(
            modifier =
            Modifier
                .fillMaxSize()
                .then(
                    if (playerBackground == PlayerBackgroundStyle.BLUR) {
                        Modifier
                    } else {
                        Modifier.background(
                            Brush.verticalGradient(
                                colors = if (gradientColors.size >= 2) gradientColors else listOf(backgroundColor, backgroundColor)
                            )
                        )
                    }
                )
        ) {
            if (playerBackground == PlayerBackgroundStyle.BLUR) {
                AsyncImage(
                    model =
                    ImageRequest
                        .Builder(context)
                        .data(mediaMetadata?.thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                    Modifier
                        .fillMaxSize()
                        .blur(100.dp)
                        .alpha(0.8f),
                )
            }

            Column(
                modifier =
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(
                        WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
                    ),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp),
                ) {
                    ResizableIconButton(
                        onClick = { state.collapse() },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.expand_more),
                            contentDescription = null,
                            tint = textButtonColor
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    if (sleepTimerEnabled) {
                        TextButton(
                            onClick = { showSleepTimerDialog = true },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.bedtime),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = textButtonColor
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = makeTimeString(sleepTimerTimeLeft),
                                color = textButtonColor
                            )
                        }
                    }

                    ResizableIconButton(
                        onClick = {
                            menuState.show {
                                PlayerMenu(
                                    navController = navController,
                                    mediaMetadata = mediaMetadata,
                                    currentSong = currentSong,
                                    showLyrics = showLyrics,
                                    onShowLyricsChange = { showLyrics = it },
                                    onDismiss = menuState::dismiss,
                                    onShowSleepTimerDialog = { showSleepTimerDialog = true },
                                    pureBlack = pureBlack
                                )
                            }
                        },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = null,
                            tint = textButtonColor
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                Box(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding),
                ) {
                    AsyncImage(
                        model =
                        ImageRequest
                            .Builder(context)
                            .data(mediaMetadata?.thumbnailUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = QueuePeekHeight)
                            .clip(RoundedCornerShape(8.dp))
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {},
                                onLongClick = {
                                    val clip = ClipData.newPlainText(
                                        "Song Info",
                                        "${mediaMetadata?.title} - ${mediaMetadata?.artist}"
                                    )
                                    clipboardManager.setPrimaryClip(clip)
                                    Toast
                                        .makeText(
                                            context,
                                            R.string.copied_to_clipboard,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                },
                            ),
                    )

                    QueueScreen(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        pureBlack = pureBlack
                    )
                }

                Spacer(Modifier.weight(1f))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = horizontalPadding),
                ) {
                    Text(
                        text = mediaMetadata?.title ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee(),
                        color = textButtonColor
                    )
                    Text(
                        text = mediaMetadata?.artist ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee(),
                        color = textButtonColor
                    )
                }

                Spacer(Modifier.height(16.dp))

                Column(
                    modifier = Modifier.padding(horizontal = horizontalPadding),
                ) {
                    val sliderValue = sliderPosition ?: position
                    val sliderColors = SliderDefaults.colors(
                        thumbColor = textButtonColor,
                        activeTrackColor = textButtonColor,
                        inactiveTrackColor = textButtonColor.copy(alpha = 0.3f)
                    )
                    when (sliderStyle) {
                        SliderStyle.DEFAULT -> {
                            Slider(
                                value = sliderValue.toFloat(),
                                onValueChange = { sliderPosition = it.toLong() },
                                valueRange = 0f..(duration.takeIf { it > 0 }?.toFloat() ?: 0f),
                                onValueChangeFinished = {
                                    sliderPosition?.let { playerConnection.player.seekTo(it) }
                                    sliderPosition = null
                                },
                                colors = sliderColors
                            )
                        }
                        SliderStyle.SQUIGGLY -> {
                            SquigglySlider(
                                value = sliderValue.toFloat(),
                                onValueChange = { sliderPosition = it.toLong() },
                                valueRange = 0f..(duration.takeIf { it > 0 }?.toFloat() ?: 0f),
                                onValueChangeFinished = {
                                    sliderPosition?.let { playerConnection.player.seekTo(it) }
                                    sliderPosition = null
                                },
                                colors = sliderColors,
                                trackHeight = 8.dp,
                                squigglesSpec = SquigglySlider.SquigglesSpec(strokeWidth = 4.dp)
                            )
                        }
                    }


                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = makeTimeString(sliderValue),
                            style = MaterialTheme.typography.bodySmall,
                            color = textButtonColor
                        )
                        Text(
                            text = makeTimeString(duration),
                            style = MaterialTheme.typography.bodySmall,
                            color = textButtonColor
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ResizableIconButton(
                        onClick = { playerConnection.toggleRepeatMode() },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            painter = painterResource(
                                when (repeatMode) {
                                    Player.REPEAT_MODE_OFF -> R.drawable.repeat
                                    Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                                    Player.REPEAT_MODE_ALL -> R.drawable.repeat
                                    else -> R.drawable.repeat
                                }
                            ),
                            contentDescription = null,
                            tint = if (repeatMode == Player.REPEAT_MODE_OFF) textButtonColor.copy(alpha = 0.5f) else textButtonColor
                        )
                    }

                    ResizableIconButton(
                        onClick = { playerConnection.player.seekToPreviousMediaItem() },
                        enabled = canSkipPrevious,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_previous),
                            contentDescription = null,
                            tint = if (canSkipPrevious) textButtonColor else textButtonColor.copy(alpha = 0.5f)
                        )
                    }

                    ResizableIconButton(
                        onClick = { playerConnection.togglePlayPause() },
                        modifier = Modifier.size(64.dp),
                    ) {
                        AnimatedContent(
                            targetState = isPlaying,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "",
                        ) {
                            Icon(
                                painter = painterResource(if (it) R.drawable.pause else R.drawable.play),
                                contentDescription = null,
                                tint = textButtonColor
                            )
                        }
                    }

                    ResizableIconButton(
                        onClick = { playerConnection.player.seekToNextMediaItem() },
                        enabled = canSkipNext,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_next),
                            contentDescription = null,
                            tint = if (canSkipNext) textButtonColor else textButtonColor.copy(alpha = 0.5f)
                        )
                    }

                    ResizableIconButton(
                        onClick = { playerConnection.player.shuffleModeEnabled = !playerConnection.player.shuffleModeEnabled },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.shuffle),
                            contentDescription = null,
                            tint = if (playerConnection.player.shuffleModeEnabled) textButtonColor else textButtonColor.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

