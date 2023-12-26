package com.zionhuang.music.ui.player

import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DismissValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDismissState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.navigation.NavController
import com.zionhuang.music.LocalPlayerConnection
import com.zionhuang.music.R
import com.zionhuang.music.constants.ListItemHeight
import com.zionhuang.music.constants.ShowLyricsKey
import com.zionhuang.music.extensions.metadata
import com.zionhuang.music.extensions.move
import com.zionhuang.music.extensions.togglePlayPause
import com.zionhuang.music.ui.component.BottomSheet
import com.zionhuang.music.ui.component.BottomSheetState
import com.zionhuang.music.ui.component.LocalMenuState
import com.zionhuang.music.ui.component.MediaMetadataListItem
import com.zionhuang.music.ui.menu.PlayerMenu
import com.zionhuang.music.utils.makeTimeString
import com.zionhuang.music.utils.rememberPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import kotlin.math.roundToInt

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun Queue(
    state: BottomSheetState,
    playerBottomSheetState: BottomSheetState,
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val menuState = LocalMenuState.current

    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()

    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val currentFormat by playerConnection.currentFormat.collectAsState(initial = null)

    var showLyrics by rememberPreference(ShowLyricsKey, defaultValue = false)

    val sleepTimerEnabled = remember(playerConnection.service.sleepTimer.triggerTime, playerConnection.service.sleepTimer.pauseWhenSongEnd) {
        playerConnection.service.sleepTimer.isActive
    }

    var sleepTimerTimeLeft by remember {
        mutableStateOf(0L)
    }

    LaunchedEffect(sleepTimerEnabled) {
        if (sleepTimerEnabled) {
            while (isActive) {
                sleepTimerTimeLeft = if (playerConnection.service.sleepTimer.pauseWhenSongEnd) {
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
        mutableStateOf(30f)
    }
    if (showSleepTimerDialog) {
        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { showSleepTimerDialog = false },
            icon = { Icon(painter = painterResource(R.drawable.bedtime), contentDescription = null) },
            title = { Text(stringResource(R.string.sleep_timer)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSleepTimerDialog = false
                        playerConnection.service.sleepTimer.start(sleepTimerValue.roundToInt())
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSleepTimerDialog = false }
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = pluralStringResource(R.plurals.minute, sleepTimerValue.roundToInt(), sleepTimerValue.roundToInt()),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Slider(
                        value = sleepTimerValue,
                        onValueChange = { sleepTimerValue = it },
                        valueRange = 5f..120f,
                        steps = (120 - 5) / 5 - 1,
                    )

                    OutlinedButton(
                        onClick = {
                            showSleepTimerDialog = false
                            playerConnection.service.sleepTimer.start(-1)
                        }
                    ) {
                        Text(stringResource(R.string.end_of_song))
                    }
                }
            }
        )
    }

    var showDetailsDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showDetailsDialog) {
        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { showDetailsDialog = false },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.info),
                    contentDescription = null
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showDetailsDialog = false }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .sizeIn(minWidth = 280.dp, maxWidth = 560.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    listOf(
                        stringResource(R.string.song_title) to mediaMetadata?.title,
                        stringResource(R.string.song_artists) to mediaMetadata?.artists?.joinToString { it.name },
                        stringResource(R.string.media_id) to mediaMetadata?.id,
                        "Itag" to currentFormat?.itag?.toString(),
                        stringResource(R.string.mime_type) to currentFormat?.mimeType,
                        stringResource(R.string.codecs) to currentFormat?.codecs,
                        stringResource(R.string.bitrate) to currentFormat?.bitrate?.let { "${it / 1000} Kbps" },
                        stringResource(R.string.sample_rate) to currentFormat?.sampleRate?.let { "$it Hz" },
                        stringResource(R.string.loudness) to currentFormat?.loudnessDb?.let { "$it dB" },
                        stringResource(R.string.volume) to "${(playerConnection.player.volume * 100).toInt()}%",
                        stringResource(R.string.file_size) to currentFormat?.contentLength?.let { Formatter.formatShortFileSize(context, it) }
                    ).forEach { (label, text) ->
                        val displayText = text ?: stringResource(R.string.unknown)
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(displayText))
                                    Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
                                }
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        )
    }

    BottomSheet(
        state = state,
        backgroundColor = MaterialTheme.colorScheme.surfaceColorAtElevation(NavigationBarDefaults.Elevation),
        modifier = modifier,
        collapsedContent = {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(
                        WindowInsets.systemBars
                            .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                    )
            ) {
                IconButton(onClick = { state.expandSoft() }) {
                    Icon(
                        painter = painterResource(R.drawable.queue_music),
                        contentDescription = null
                    )
                }
                IconButton(onClick = { showLyrics = !showLyrics }) {
                    Icon(
                        painter = painterResource(R.drawable.lyrics),
                        contentDescription = null,
                        modifier = Modifier.alpha(if (showLyrics) 1f else 0.5f)
                    )
                }
                AnimatedContent(
                    label = "sleepTimer",
                    targetState = sleepTimerEnabled
                ) { sleepTimerEnabled ->
                    if (sleepTimerEnabled) {
                        Text(
                            text = makeTimeString(sleepTimerTimeLeft),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable(onClick = playerConnection.service.sleepTimer::clear)
                                .padding(8.dp)
                        )
                    } else {
                        IconButton(onClick = { showSleepTimerDialog = true }) {
                            Icon(
                                painter = painterResource(R.drawable.bedtime),
                                contentDescription = null
                            )
                        }
                    }
                }
                IconButton(onClick = playerConnection::toggleLibrary) {
                    Icon(
                        painter = painterResource(if (currentSong?.song?.inLibrary != null) R.drawable.library_add_check else R.drawable.library_add),
                        contentDescription = null
                    )
                }
                IconButton(
                    onClick = {
                        menuState.show {
                            PlayerMenu(
                                mediaMetadata = mediaMetadata,
                                navController = navController,
                                playerBottomSheetState = playerBottomSheetState,
                                onShowDetailsDialog = { showDetailsDialog = true },
                                onDismiss = menuState::dismiss
                            )
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_horiz),
                        contentDescription = null
                    )
                }
            }
        }
    ) {
        val queueTitle by playerConnection.queueTitle.collectAsState()
        val queueWindows by playerConnection.queueWindows.collectAsState()
        val mutableQueueWindows = remember { mutableStateListOf<Timeline.Window>() }
        val queueLength = remember(queueWindows) {
            queueWindows.sumOf { it.mediaItem.metadata!!.duration }
        }

        val coroutineScope = rememberCoroutineScope()
        val reorderableState = rememberReorderableLazyListState(
            onMove = { from, to ->
                mutableQueueWindows.move(from.index, to.index)
            },
            onDragEnd = { fromIndex, toIndex ->
                if (!playerConnection.player.shuffleModeEnabled) {
                    playerConnection.player.moveMediaItem(fromIndex, toIndex)
                } else {
                    playerConnection.player.setShuffleOrder(
                        DefaultShuffleOrder(
                            queueWindows.map { it.firstPeriodIndex }.toMutableList().move(fromIndex, toIndex).toIntArray(),
                            System.currentTimeMillis()
                        )
                    )
                }
            }
        )

        LaunchedEffect(queueWindows) {
            mutableQueueWindows.apply {
                clear()
                addAll(queueWindows)
            }
        }

        LaunchedEffect(mutableQueueWindows) {
            reorderableState.listState.scrollToItem(currentWindowIndex)
        }

        LazyColumn(
            state = reorderableState.listState,
            contentPadding = WindowInsets.systemBars
                .add(
                    WindowInsets(
                        top = ListItemHeight,
                        bottom = ListItemHeight
                    )
                )
                .asPaddingValues(),
            modifier = Modifier
                .reorderable(reorderableState)
                .nestedScroll(state.preUpPostDownNestedScrollConnection)
        ) {
            itemsIndexed(
                items = mutableQueueWindows,
                key = { _, item -> item.uid.hashCode() }
            ) { index, window ->
                ReorderableItem(
                    reorderableState = reorderableState,
                    key = window.uid.hashCode()
                ) {
                    val currentItem by rememberUpdatedState(window)
                    val dismissState = rememberDismissState(
                        positionalThreshold = { totalDistance ->
                            totalDistance
                        },
                        confirmValueChange = { dismissValue ->
                            if (dismissValue == DismissValue.DismissedToEnd || dismissValue == DismissValue.DismissedToStart) {
                                playerConnection.player.removeMediaItem(currentItem.firstPeriodIndex)
                            }
                            true
                        }
                    )
                    SwipeToDismiss(
                        state = dismissState,
                        background = {},
                        dismissContent = {
                            MediaMetadataListItem(
                                mediaMetadata = window.mediaItem.metadata!!,
                                isActive = index == currentWindowIndex,
                                isPlaying = isPlaying,
                                trailingContent = {
                                    IconButton(
                                        onClick = { },
                                        modifier = Modifier
                                            .detectReorder(reorderableState)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.drag_handle),
                                            contentDescription = null
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            if (index == currentWindowIndex) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                playerConnection.player.seekToDefaultPosition(window.firstPeriodIndex)
                                                playerConnection.player.playWhenReady = true
                                            }
                                        },
                                        onLongClick = {
                                            menuState.show {
                                                PlayerMenu(
                                                    mediaMetadata = window.mediaItem.metadata!!,
                                                    navController = navController,
                                                    playerBottomSheetState = playerBottomSheetState,
                                                    isQueueTrigger = true,
                                                    onShowDetailsDialog = { showDetailsDialog = true },
                                                    onDismiss = menuState::dismiss
                                                )
                                            }
                                        }
                                    )
                                //.detectReorderAfterLongPress(reorderableState)
                            )
                        }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme
                        .surfaceColorAtElevation(NavigationBarDefaults.Elevation)
                        .copy(alpha = 0.95f)
                )
                .windowInsetsPadding(
                    WindowInsets.systemBars
                        .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                )
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Text(
                    text = queueTitle.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = pluralStringResource(R.plurals.n_song, queueWindows.size, queueWindows.size),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = makeTimeString(queueLength * 1000L),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState()

        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f))
                .fillMaxWidth()
                .height(
                    ListItemHeight +
                            WindowInsets.systemBars
                                .asPaddingValues()
                                .calculateBottomPadding()
                )
                .align(Alignment.BottomCenter)
                .clickable {
                    state.collapseSoft()
                }
                .windowInsetsPadding(
                    WindowInsets.systemBars
                        .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                )
                .padding(12.dp)
        ) {
            IconButton(
                modifier = Modifier.align(Alignment.CenterStart),
                onClick = {
                    coroutineScope.launch {
                        reorderableState.listState.animateScrollToItem(
                            if (playerConnection.player.shuffleModeEnabled) playerConnection.player.currentMediaItemIndex else 0
                        )
                    }.invokeOnCompletion {
                        playerConnection.player.shuffleModeEnabled = !playerConnection.player.shuffleModeEnabled
                    }
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.shuffle),
                    contentDescription = null,
                    modifier = Modifier.alpha(if (shuffleModeEnabled) 1f else 0.5f)
                )
            }

            Icon(
                painter = painterResource(R.drawable.expand_more),
                contentDescription = null,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
