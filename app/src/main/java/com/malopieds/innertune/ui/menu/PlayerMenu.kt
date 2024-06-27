package com.malopieds.innertune.ui.menu

import android.content.Intent
import android.media.audiofx.AudioEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import com.malopieds.innertune.LocalDatabase
import com.malopieds.innertune.LocalDownloadUtil
import com.malopieds.innertune.LocalPlayerConnection
import com.malopieds.innertune.R
import com.malopieds.innertune.constants.ListItemHeight
import com.malopieds.innertune.db.entities.PlaylistSongMap
import com.malopieds.innertune.models.MediaMetadata
import com.malopieds.innertune.playback.ExoDownloadService
import com.malopieds.innertune.ui.component.BigSeekBar
import com.malopieds.innertune.ui.component.BottomSheetState
import com.malopieds.innertune.ui.component.DownloadGridMenu
import com.malopieds.innertune.ui.component.GridMenu
import com.malopieds.innertune.ui.component.GridMenuItem
import com.malopieds.innertune.ui.component.ListDialog
import java.time.LocalDateTime
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.round

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
    val activityResultLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    val download by LocalDownloadUtil.current.getDownload(mediaMetadata.id).collectAsState(initial = null)

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onAdd = { playlist ->
            database.transaction {
                insert(mediaMetadata)
                insert(
                    PlaylistSongMap(
                        songId = mediaMetadata.id,
                        playlistId = playlist.id,
                        position = playlist.songCount
                    )
                )
                update(playlist.playlist.copy(lastUpdateTime = LocalDateTime.now()))
            }
        },
        onDismiss = {
            showChoosePlaylistDialog = false
        }
    )

    var showSelectArtistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showSelectArtistDialog) {
        ListDialog(
            onDismiss = { showSelectArtistDialog = false }
        ) {
            items(mediaMetadata.artists) { artist ->
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
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
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    var showPitchTempoDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showPitchTempoDialog) {
        PitchTempoDialog(
            onDismiss = { showPitchTempoDialog = false }
        )
    }
    if (isQueueTrigger != true) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = 6.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.volume_up),
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )

            BigSeekBar(
                progressProvider = playerVolume::value,
                onProgressChange = { playerConnection.service.playerVolume.value = it },
                modifier = Modifier.weight(1f)
            )
        }
    }

    GridMenu(
        contentPadding = PaddingValues(
            start = 8.dp,
            top = 8.dp,
            end = 8.dp,
            bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
        )
    ) {
        GridMenuItem(
            icon = R.drawable.radio,
            title = R.string.start_radio
        ) {
            playerConnection.service.startRadioSeamlessly()
            onDismiss()
        }
        GridMenuItem(
            icon = R.drawable.playlist_add,
            title = R.string.add_to_playlist
        ) {
            showChoosePlaylistDialog = true
        }
        DownloadGridMenu(
            state = download?.state,
            onDownload = {
                database.transaction {
                    insert(mediaMetadata)
                }
                val downloadRequest = DownloadRequest.Builder(mediaMetadata.id, mediaMetadata.id.toUri())
                    .setCustomCacheKey(mediaMetadata.id)
                    .setData(mediaMetadata.title.toByteArray())
                    .build()
                DownloadService.sendAddDownload(
                    context,
                    ExoDownloadService::class.java,
                    downloadRequest,
                    false
                )
            },
            onRemoveDownload = {
                DownloadService.sendRemoveDownload(
                    context,
                    ExoDownloadService::class.java,
                    mediaMetadata.id,
                    false
                )
            }
        )
        GridMenuItem(
            icon = R.drawable.artist,
            title = R.string.view_artist
        ) {
            if (mediaMetadata.artists.size == 1) {
                navController.navigate("artist/${mediaMetadata.artists[0].id}")
                playerBottomSheetState.collapseSoft()
                onDismiss()
            } else {
                showSelectArtistDialog = true
            }
        }
        if (mediaMetadata.album != null) {
            GridMenuItem(
                icon = R.drawable.album,
                title = R.string.view_album
            ) {
                navController.navigate("album/${mediaMetadata.album.id}")
                playerBottomSheetState.collapseSoft()
                onDismiss()
            }
        }
        GridMenuItem(
            icon = R.drawable.share,
            title = R.string.share
        ) {
            val intent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=${mediaMetadata.id}")
            }
            context.startActivity(Intent.createChooser(intent, null))
            onDismiss()
        }
        if (isQueueTrigger != true) {
            GridMenuItem(
                icon = R.drawable.info,
                title = R.string.details
            ) {
                onShowDetailsDialog()
                onDismiss()
            }
            GridMenuItem(
                icon = R.drawable.equalizer,
                title = R.string.equalizer
            ) {
                val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                    putExtra(
                        AudioEffect.EXTRA_AUDIO_SESSION,
                        playerConnection.player.audioSessionId
                    )
                    putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                    putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                }
                if (intent.resolveActivity(context.packageManager) != null) {
                    activityResultLauncher.launch(intent)
                }
                onDismiss()
            }
            GridMenuItem(
                icon = R.drawable.tune,
                title = R.string.advanced
            ) {
                showPitchTempoDialog = true
            }
        }
    }
}

@Composable
fun PitchTempoDialog(
    onDismiss: () -> Unit,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    var tempo by remember {
        mutableStateOf(playerConnection.player.playbackParameters.speed)
    }
    var transposeValue by remember {
        mutableStateOf(round(12 * log2(playerConnection.player.playbackParameters.pitch)).toInt())
    }
    val updatePlaybackParameters = {
        playerConnection.player.playbackParameters = PlaybackParameters(tempo, 2f.pow(transposeValue.toFloat() / 12))
    }

    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(
                onClick = {
                    tempo = 1f
                    transposeValue = 0
                    updatePlaybackParameters()
                }
            ) {
                Text(stringResource(R.string.reset))
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        text = {
            Column {
                ValueAdjuster(
                    icon = R.drawable.slow_motion_video,
                    currentValue = tempo,
                    values = (0..35).map { round((0.25f + it * 0.05f) * 100 ) / 100 },
                    onValueUpdate = {
                        tempo = it
                        updatePlaybackParameters()
                    },
                    valueText = { "x$it" },
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                ValueAdjuster(
                    icon = R.drawable.discover_tune,
                    currentValue = transposeValue,
                    values = (-12..12).toList(),
                    onValueUpdate = {
                        transposeValue = it
                        updatePlaybackParameters()
                    },
                    valueText = { "${if (it > 0) "+" else ""}$it" }
                )
            }
        }
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
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(28.dp)
        )

        IconButton(
            enabled = currentValue != values.first(),
            onClick = {
                onValueUpdate(values[values.indexOf(currentValue) - 1])
            }
        ) {
            Icon(
                painter = painterResource(R.drawable.remove),
                contentDescription = null
            )
        }

        Text(
            text = valueText(currentValue),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(80.dp)
        )

        IconButton(
            enabled = currentValue != values.last(),
            onClick = {
                onValueUpdate(values[values.indexOf(currentValue) + 1])
            }
        ) {
            Icon(
                painter = painterResource(R.drawable.add),
                contentDescription = null
            )
        }
    }
}
