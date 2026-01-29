/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.menu

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import android.content.Context
import android.widget.Toast
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.listentogether.ListenTogetherEvent
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalDownloadUtil
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.ListItemHeight
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.playback.ExoDownloadService
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.BigSeekBar
import com.metrolist.music.ui.component.BottomSheetState
import com.metrolist.music.ui.component.ListDialog
import com.metrolist.music.ui.component.Material3MenuItemData
import com.metrolist.music.ui.component.Material3MenuGroup
import com.metrolist.music.ui.component.NewAction
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.component.NewActionGrid
import com.metrolist.music.ui.component.VolumeSlider
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.round
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import com.metrolist.music.listentogether.ConnectionState

@Composable
fun PlayerMenu(
    mediaMetadata: MediaMetadata?,
    navController: NavController,
    playerBottomSheetState: BottomSheetState,
    isQueueTrigger: Boolean? = false,
    onShowDetailsDialog: () -> Unit,
    onDismiss: () -> Unit,
) {
    mediaMetadata ?: return
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val playerVolume = playerConnection.service.playerVolume.collectAsState()
    
    // Cast state for volume control
    val castHandler = playerConnection.service.castConnectionHandler
    val isCasting by castHandler?.isCasting?.collectAsState() ?: remember { mutableStateOf(false) }
    val castVolume by castHandler?.castVolume?.collectAsState() ?: remember { mutableStateOf(1f) }
    val castDeviceName by castHandler?.castDeviceName?.collectAsState() ?: remember { mutableStateOf<String?>(null) }
    
    val librarySong by database.song(mediaMetadata.id).collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()

    val download by LocalDownloadUtil.current.getDownload(mediaMetadata.id)
        .collectAsState(initial = null)

    val artists =
        remember(mediaMetadata.artists) {
            mediaMetadata.artists.filter { it.id != null }
        }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }
    
    var showListenTogetherDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val listenTogetherManager = LocalListenTogetherManager.current
    val listenTogetherRoleState = listenTogetherManager?.role?.collectAsState(initial = com.metrolist.music.listentogether.RoomRole.NONE)
    val isListenTogetherGuest = listenTogetherRoleState?.value == com.metrolist.music.listentogether.RoomRole.GUEST
    val pendingSuggestions by listenTogetherManager?.pendingSuggestions?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { playlist ->
            database.transaction {
                insert(mediaMetadata)
            }
            coroutineScope.launch(Dispatchers.IO) {
                playlist.playlist.browseId?.let { YouTube.addToPlaylist(it, mediaMetadata.id) }
            }
            listOf(mediaMetadata.id)
        },
        onDismiss = {
            showChoosePlaylistDialog = false
        }
    )

    ListenTogetherDialog(
        visible = showListenTogetherDialog,
        mediaMetadata = mediaMetadata,
        onDismiss = { showListenTogetherDialog = false }
    )

    var showSelectArtistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showSelectArtistDialog) {
        ListDialog(
            onDismiss = { showSelectArtistDialog = false },
        ) {
            items(artists) { artist ->
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier =
                    Modifier
                        .fillParentMaxWidth()
                        .height(ListItemHeight)
                        .clickable {
                            navController.navigate("artist/${artist.id}")
                            showSelectArtistDialog = false
                            playerBottomSheetState.collapseSoft()
                            onDismiss()
                        }
                        .padding(horizontal = 24.dp),
                ) {
                    Text(
                        text = artist.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }

    var showPitchTempoDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showPitchTempoDialog) {
        TempoPitchDialog(
            onDismiss = { showPitchTempoDialog = false },
        )
    }

    if (isQueueTrigger != true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = 6.dp),
        ) {
            // Show Cast indicator when casting
            if (isCasting && castDeviceName != null) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.cast),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.casting_to, castDeviceName ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            VolumeSlider(
                value = if (isCasting) castVolume else playerVolume.value,
                onValueChange = { volume ->
                    if (isCasting) {
                        castHandler?.setVolume(volume)
                    } else {
                        playerConnection.service.playerVolume.value = volume
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                accentColor = MaterialTheme.colorScheme.primary
            )
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    HorizontalDivider()

    Spacer(modifier = Modifier.height(12.dp))

    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    LazyColumn(
        contentPadding = PaddingValues(
            start = 0.dp,
            top = 0.dp,
            end = 0.dp,
            bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
        ),
    ) {
        item {
            val startingRadioText = stringResource(R.string.starting_radio)
            NewActionGrid(
                actions = listOfNotNull(
                    if (!isListenTogetherGuest) {
                        NewAction(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.radio),
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            text = stringResource(R.string.start_radio),
                            onClick = {
                                Toast.makeText(context, startingRadioText, Toast.LENGTH_SHORT).show()
                                playerConnection.startRadioSeamlessly()
                                onDismiss()
                            }
                        )
                    } else null,
                    NewAction(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.playlist_add),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        text = stringResource(R.string.add_to_playlist),
                        onClick = { showChoosePlaylistDialog = true }
                    ),
                    NewAction(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.link),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        text = stringResource(R.string.copy_link),
                        onClick = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Song Link", "https://music.youtube.com/watch?v=${mediaMetadata.id}")
                            clipboard.setPrimaryClip(clip)
                            android.widget.Toast.makeText(context, R.string.link_copied, android.widget.Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    )
                ),
                columns = if (isListenTogetherGuest) 2 else 3,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp)
            )
        }

        item {
            Material3MenuGroup(
                items = buildList {
                    if (artists.isNotEmpty()) {
                        add(
                            Material3MenuItemData(
                                title = { Text(text = stringResource(R.string.view_artist)) },
                                description = {
                                    Text(
                                        text = mediaMetadata.artists.joinToString { it.name },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.artist),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                onClick = {
                                    if (mediaMetadata.artists.size == 1) {
                                        navController.navigate("artist/${mediaMetadata.artists[0].id}")
                                        playerBottomSheetState.collapseSoft()
                                        onDismiss()
                                    } else {
                                        showSelectArtistDialog = true
                                    }
                                }
                            )
                        )
                    }
                    if (mediaMetadata.album != null) {
                        add(
                            Material3MenuItemData(
                                title = { Text(text = stringResource(R.string.view_album)) },
                                description = {
                                    Text(
                                        text = mediaMetadata.album.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.album),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                onClick = {
                                    navController.navigate("album/${mediaMetadata.album.id}")
                                    playerBottomSheetState.collapseSoft()
                                    onDismiss()
                                }
                            )
                        )
                    }
                }
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        item {
            Material3MenuGroup(
                items = listOf(
                    when (download?.state) {
                        Download.STATE_COMPLETED -> {
                            Material3MenuItemData(
                                title = {
                                    Text(
                                        text = stringResource(R.string.remove_download)
                                    )
                                },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.offline),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                onClick = {
                                    DownloadService.sendRemoveDownload(
                                        context,
                                        ExoDownloadService::class.java,
                                        mediaMetadata.id,
                                        false,
                                    )
                                }
                            )
                        }

                        Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> {
                            Material3MenuItemData(
                                title = { Text(text = stringResource(R.string.downloading)) },
                                icon = {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                },
                                onClick = {
                                    DownloadService.sendRemoveDownload(
                                        context,
                                        ExoDownloadService::class.java,
                                        mediaMetadata.id,
                                        false,
                                    )
                                }
                            )
                        }

                        else -> {
                            Material3MenuItemData(
                                title = { Text(text = stringResource(R.string.action_download)) },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.download),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                onClick = {
                                    database.transaction {
                                        insert(mediaMetadata)
                                    }
                                    val downloadRequest =
                                        DownloadRequest
                                            .Builder(mediaMetadata.id, mediaMetadata.id.toUri())
                                            .setCustomCacheKey(mediaMetadata.id)
                                            .setData(mediaMetadata.title.toByteArray())
                                            .build()
                                    DownloadService.sendAddDownload(
                                        context,
                                        ExoDownloadService::class.java,
                                        downloadRequest,
                                        false,
                                    )
                                }
                            )
                        }
                    }
                )
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        item {
            Material3MenuGroup(
                items = buildList {
                    add(
                        Material3MenuItemData(
                            title = { Text(text = stringResource(R.string.listen_together)) },
                            icon = {
                                // Show a small badge when there are pending suggestions
                                Box {
                                    Icon(
                                        painter = painterResource(R.drawable.group),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    if (pendingSuggestions.isNotEmpty()) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .offset(x = 8.dp, y = (-6).dp)
                                                .align(Alignment.TopEnd)
                                        ) {
                                            Text(
                                                text = pendingSuggestions.size.toString(),
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                            },
                            onClick = { showListenTogetherDialog = true }
                        )
                    )
                    if (isListenTogetherGuest) {
                        add(
                            Material3MenuItemData(
                                title = { Text(text = stringResource(R.string.resync)) },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.replay),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                onClick = {
                                    listenTogetherManager.requestSync()
                                    onDismiss()
                                }
                            )
                        )
                    }
                }
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        item {
            Material3MenuGroup(
                items = buildList {
                    add(
                        Material3MenuItemData(
                            title = { Text(text = stringResource(R.string.details)) },
                            description = { Text(text = stringResource(R.string.details_desc)) },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.info),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            onClick = {
                                onShowDetailsDialog()
                                onDismiss()
                            }
                        )
                    )

                    if (isQueueTrigger != true) {
                        add(
                            Material3MenuItemData(
                                title = { Text(text = stringResource(R.string.equalizer)) },
                                description = { Text(text = stringResource(R.string.equalizer_desc)) },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.equalizer),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                onClick = {
                                    navController.navigate("equalizer")
                                    onDismiss()
                                }
                            )
                        )
                        add(
                            Material3MenuItemData(
                                title = { Text(text = stringResource(R.string.advanced)) },
                                description = { Text(text = stringResource(R.string.advanced_desc)) },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.tune),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                onClick = {
                                    showPitchTempoDialog = true
                                }
                            )
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun TempoPitchDialog(onDismiss: () -> Unit) {
    val playerConnection = LocalPlayerConnection.current ?: return
    var tempo by remember {
        mutableFloatStateOf(playerConnection.player.playbackParameters.speed)
    }
    var transposeValue by remember {
        mutableIntStateOf(round(12 * log2(playerConnection.player.playbackParameters.pitch)).toInt())
    }
    val updatePlaybackParameters = {
        playerConnection.player.playbackParameters =
            PlaybackParameters(tempo, 2f.pow(transposeValue.toFloat() / 12))
    }
    val listenTogetherManager = com.metrolist.music.LocalListenTogetherManager.current
    val isInRoom = listenTogetherManager?.isInRoom ?: false

    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.tempo_and_pitch))
        },
        dismissButton = {
            TextButton(
                onClick = {
                    tempo = 1f
                    transposeValue = 0
                    updatePlaybackParameters()
                },
            ) {
                Text(stringResource(R.string.reset))
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        text = {
            Column {
                if (!isInRoom) {
                    ValueAdjuster(
                        icon = R.drawable.speed,
                        currentValue = tempo,
                        values = (0..35).map { round((0.25f + it * 0.05f) * 100) / 100 },
                        onValueUpdate = {
                            tempo = it
                            updatePlaybackParameters()
                        },
                        valueText = { "x$it" },
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
                ValueAdjuster(
                    icon = R.drawable.discover_tune,
                    currentValue = transposeValue,
                    values = (-12..12).toList(),
                    onValueUpdate = {
                        transposeValue = it
                        updatePlaybackParameters()
                    },
                    valueText = { "${if (it > 0) "+" else ""}$it" },
                )
            }
        },
    )
}

@Composable
fun <T> ValueAdjuster(
    @DrawableRes icon: Int,
    currentValue: T,
    values: List<T>,
    onValueUpdate: (T) -> Unit,
    valueText: (T) -> String,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
        )

        IconButton(
            enabled = currentValue != values.first(),
            onClick = {
                onValueUpdate(values[values.indexOf(currentValue) - 1])
            },
        ) {
            Icon(
                painter = painterResource(R.drawable.remove),
                contentDescription = null,
            )
        }

        Text(
            text = valueText(currentValue),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(80.dp),
        )

        IconButton(
            enabled = currentValue != values.last(),
            onClick = {
                onValueUpdate(values[values.indexOf(currentValue) + 1])
            },
        ) {
            Icon(
                painter = painterResource(R.drawable.add),
                contentDescription = null,
            )
        }
    }
}

@Composable
fun ListenTogetherDialog(
    visible: Boolean,
    mediaMetadata: MediaMetadata?,
    onDismiss: () -> Unit
) {
    if (!visible) return
    
    val context = LocalContext.current
    val listenTogetherManager = com.metrolist.music.LocalListenTogetherManager.current
    
    // Handle case where manager is not available
    if (listenTogetherManager == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.listen_together)) },
            text = {
                Text(
                    text = stringResource(R.string.listen_together_not_configured),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        )
        return
    }
    
    val connectionState by listenTogetherManager.connectionState.collectAsState()
    val roomState by listenTogetherManager.roomState.collectAsState()
    val userId by listenTogetherManager.userId.collectAsState()
    val pendingJoinRequests by listenTogetherManager.pendingJoinRequests.collectAsState()
    val pendingSuggestions by listenTogetherManager.pendingSuggestions.collectAsState()
    
    // Load saved username
    var savedUsername by rememberPreference(com.metrolist.music.constants.ListenTogetherUsernameKey, "")
    var roomCodeInput by rememberSaveable { mutableStateOf("") }
    var usernameInput by rememberSaveable { mutableStateOf(savedUsername) }

    // Local UI state for join/create actions
    var isCreatingRoom by rememberSaveable { mutableStateOf(false) }
    var isJoiningRoom by rememberSaveable { mutableStateOf(false) }
    var joinErrorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    // Localized helper strings
    val waitingForApprovalText = stringResource(R.string.waiting_for_approval)
    val invalidRoomCodeText = stringResource(R.string.invalid_room_code)
    val joinRequestDeniedText = stringResource(R.string.join_request_denied)

    // Sync usernameInput when savedUsername changes
    LaunchedEffect(savedUsername) {
        if (usernameInput.isBlank() && savedUsername.isNotBlank()) {
            usernameInput = savedUsername
        }
    }

    // Listen to low level events to update UI state (join rejected, approved, room created)
    LaunchedEffect(listenTogetherManager) {
        listenTogetherManager.events.collect { event ->
            when (event) {
                is ListenTogetherEvent.JoinRejected -> {
                    val reason = event.reason
                    joinErrorMessage = when {
                        reason.isNullOrBlank() -> joinRequestDeniedText
                        reason.contains("invalid", ignoreCase = true) == true -> invalidRoomCodeText
                        else -> "$joinRequestDeniedText: $reason"
                    }
                    isJoiningRoom = false
                    isCreatingRoom = false
                }

                is ListenTogetherEvent.JoinApproved -> {
                    isJoiningRoom = false
                    joinErrorMessage = null
                }

                is ListenTogetherEvent.RoomCreated -> {
                    isCreatingRoom = false
                }

                else -> { /* ignore other events here */ }
            }
        }
    }

    // Check if already in a room
    val isInRoom = listenTogetherManager.isInRoom
    val isHost = roomState?.hostId == userId
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.share),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = if (isInRoom) {
                        if (isHost) stringResource(R.string.hosting_room) else stringResource(R.string.in_room)
                    } else {
                        stringResource(R.string.listen_together)
                    }
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Connection status indicator and controls
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (connectionState) {
                            ConnectionState.CONNECTED -> MaterialTheme.colorScheme.primaryContainer
                            ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> MaterialTheme.colorScheme.secondaryContainer
                            ConnectionState.ERROR -> MaterialTheme.colorScheme.errorContainer
                            ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = when (connectionState) {
                                ConnectionState.CONNECTED -> stringResource(R.string.listen_together_connected)
                                ConnectionState.CONNECTING -> stringResource(R.string.listen_together_connecting)
                                ConnectionState.RECONNECTING -> stringResource(R.string.listen_together_reconnecting)
                                ConnectionState.ERROR -> stringResource(R.string.listen_together_error)
                                ConnectionState.DISCONNECTED -> stringResource(R.string.listen_together_disconnected)
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        if (connectionState == ConnectionState.CONNECTING || connectionState == ConnectionState.RECONNECTING) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (connectionState == ConnectionState.DISCONNECTED || connectionState == ConnectionState.ERROR) {
                                Button(
                                    onClick = { listenTogetherManager.connect() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(R.string.connect))
                                }
                            } else {
                                Button(
                                    onClick = { listenTogetherManager.disconnect() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(R.string.disconnect))
                                }
                                FilledTonalButton(
                                    onClick = { listenTogetherManager.forceReconnect() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Reconnect")
                                }
                            }
                        }
                    }
                }
                
                if (connectionState == ConnectionState.CONNECTED && !isInRoom) {
                    Text(
                        text = stringResource(R.string.listen_together_background_disconnect_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isInRoom) {
                    // Room status card
                    roomState?.let { room ->
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = stringResource(R.string.room_code),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = room.roomCode,
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 4.sp
                                    )
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                            val clip = android.content.ClipData.newPlainText("Room Code", room.roomCode)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.content_copy),
                                            contentDescription = stringResource(R.string.copy),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Connected users (filter out disconnected users in grace period)
                        val connectedUsers = room.users.filter { it.isConnected }
                        Text(
                            text = stringResource(R.string.connected_users, connectedUsers.size),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                        
                        room.users.forEach { user ->
                            // Only show connected users in the UI
                            if (!user.isConnected) return@forEach
                            
                            Surface(
                                color = if (user.isHost) {
                                    MaterialTheme.colorScheme.tertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.person),
                                            contentDescription = null,
                                            tint = if (user.isHost) {
                                                MaterialTheme.colorScheme.onTertiaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = user.username,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (user.userId == userId) FontWeight.Bold else FontWeight.Normal,
                                                color = if (user.isHost) {
                                                    MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.9f)
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                            if (user.isHost) {
                                                Text(
                                                    text = stringResource(R.string.host_label),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                                                )
                                            } else if (user.userId == userId) {
                                                Text(
                                                    text = stringResource(R.string.you_label),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                    }
                                    
                                    // Kick button (only show for host and not for themselves)
                                    if (isHost && user.userId != userId) {
                                        IconButton(
                                            onClick = {
                                                listenTogetherManager.kickUser(user.userId, "Removed by host")
                                            }
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.close),
                                                contentDescription = stringResource(R.string.kick_user),
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Pending join requests (host only)
                        if (isHost && pendingJoinRequests.isNotEmpty()) {
                            HorizontalDivider()
                            Text(
                                text = stringResource(R.string.pending_requests),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            pendingJoinRequests.forEach { request ->
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.group_add),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = request.username,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                        
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(
                                                onClick = {
                                                    listenTogetherManager.approveJoin(request.userId)
                                                }
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.check),
                                                    contentDescription = stringResource(R.string.approve),
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    listenTogetherManager.rejectJoin(request.userId, "Rejected by host")
                                                }
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.close),
                                                    contentDescription = stringResource(R.string.reject),
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Pending suggestions (host only)
                        if (isHost && pendingSuggestions.isNotEmpty()) {
                            HorizontalDivider()
                            Text(
                                text = stringResource(R.string.pending_suggestions),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            pendingSuggestions.forEach { suggestion ->
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.queue_music),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = suggestion.trackInfo.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = suggestion.fromUsername,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(
                                                onClick = {
                                                    listenTogetherManager.approveSuggestion(suggestion.suggestionId)
                                                }
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.check),
                                                    contentDescription = stringResource(R.string.approve),
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    listenTogetherManager.rejectSuggestion(suggestion.suggestionId, "Rejected by host")
                                                }
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.close),
                                                    contentDescription = stringResource(R.string.reject),
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Quick join card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.listen_together_description),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                            OutlinedTextField(
                                value = usernameInput,
                                onValueChange = { usernameInput = it },
                                label = { Text(stringResource(R.string.username)) },
                                placeholder = { Text(stringResource(R.string.enter_username)) },
                                leadingIcon = {
                                    Icon(painterResource(R.drawable.person), null)
                                },
                                trailingIcon = {
                                    if (usernameInput.isNotBlank()) {
                                        IconButton(onClick = { usernameInput = "" }) {
                                            Icon(painterResource(R.drawable.close), null)
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = stringResource(R.string.join_existing_room),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                
                                OutlinedTextField(
                                    value = roomCodeInput,
                                    onValueChange = { roomCodeInput = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(8) },
                                    label = { Text(stringResource(R.string.room_code)) },
                                    placeholder = { Text("ABCD1234") },
                                    supportingText = {
                                        Text(
                                            text = "${roomCodeInput.length}/8",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(painterResource(R.drawable.token), null)
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Status messages
                            if (isJoiningRoom) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        painterResource(R.drawable.hourglass_empty),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = waitingForApprovalText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            joinErrorMessage?.let { msg ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        painterResource(R.drawable.error),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isInRoom) {
                FilledTonalButton(
                    onClick = {
                        listenTogetherManager.leaveRoom()
                        onDismiss()
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.logout),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.leave_room))
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (roomCodeInput.length == 8) {
                        Button(
                            onClick = {
                                val username = usernameInput.takeIf { it.isNotBlank() } ?: savedUsername
                                val finalUsername = username.trim()
                                if (finalUsername.isNotBlank()) {
                                    savedUsername = finalUsername
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.joining_room, roomCodeInput),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    // Keep the dialog open while waiting for host approval
                                    isJoiningRoom = true
                                    isCreatingRoom = false
                                    joinErrorMessage = null
                                    listenTogetherManager.connect()
                                    listenTogetherManager.joinRoom(roomCodeInput, finalUsername)
                                } else {
                                    Toast.makeText(context, R.string.error_username_empty, Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = (usernameInput.trim().isNotBlank() || savedUsername.isNotBlank())
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.login),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.join_room))
                        }
                    }
                    Button(
                        onClick = {
                            val username = usernameInput.takeIf { it.isNotBlank() } ?: savedUsername
                            val finalUsername = username.trim()
                            if (finalUsername.isNotBlank()) {
                                savedUsername = finalUsername
                                Toast.makeText(
                                    context,
                                    R.string.creating_room,
                                    Toast.LENGTH_SHORT
                                ).show()
                                // Keep the dialog open while creating the room
                                isCreatingRoom = true
                                isJoiningRoom = false
                                joinErrorMessage = null
                                listenTogetherManager.connect()
                                listenTogetherManager.createRoom(finalUsername)
                            } else {
                                Toast.makeText(context, R.string.error_username_empty, Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = (usernameInput.trim().isNotBlank() || savedUsername.isNotBlank())
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.add),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.create_room))
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
