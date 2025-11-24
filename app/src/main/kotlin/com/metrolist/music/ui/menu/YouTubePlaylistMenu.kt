package com.metrolist.music.ui.menu

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import coil3.compose.AsyncImage
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.utils.completed
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalDownloadUtil
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.ListItemHeight
import com.metrolist.music.constants.ListThumbnailSize
import com.metrolist.music.constants.ThumbnailCornerRadius
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.db.entities.PlaylistSongMap
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.ExoDownloadService
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.component.ListDialog
import com.metrolist.music.ui.component.Material3MenuGroup
import com.metrolist.music.ui.component.Material3MenuItem
import com.metrolist.music.ui.component.YouTubeListItem
import com.metrolist.music.ui.utils.resize
import com.metrolist.music.utils.joinByBullet
import com.metrolist.music.utils.makeTimeString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("MutableCollectionMutableState")
@Composable
fun YouTubePlaylistMenu(
    playlist: PlaylistItem,
    songs: List<SongItem> = emptyList(),
    coroutineScope: CoroutineScope,
    onDismiss: () -> Unit,
    selectAction: () -> Unit = {},
    canSelect: Boolean = false,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val dbPlaylist by database.playlistByBrowseId(playlist.id).collectAsState(initial = null)

    var showChoosePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var showImportPlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var showErrorPlaylistAddDialog by rememberSaveable { mutableStateOf(false) }

    val notAddedList by remember {
        mutableStateOf(mutableListOf<MediaMetadata>())
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { targetPlaylist ->
            val allSongs = songs
                .ifEmpty {
                    YouTube.playlist(targetPlaylist.id).completed().getOrNull()?.songs.orEmpty()
                }.map {
                    it.toMediaMetadata()
                }
            database.transaction {
                allSongs.forEach(::insert)
            }
            coroutineScope.launch(Dispatchers.IO) {
                targetPlaylist.playlist.browseId?.let { playlistId ->
                    YouTube.addPlaylistToPlaylist(playlistId, targetPlaylist.id)
                }
            }
            allSongs.map { it.id }
        },
        onDismiss = { showChoosePlaylistDialog = false },
    )

    YouTubeListItem(
        item = playlist,
        trailingContent = {
            if (playlist.id != "LM" && !playlist.isEditable) {
                IconButton(
                    onClick = {
                        if (dbPlaylist?.playlist == null) {
                            database.transaction {
                                val playlistEntity = PlaylistEntity(
                                    name = playlist.title,
                                    browseId = playlist.id,
                                    thumbnailUrl = playlist.thumbnail,
                                    isEditable = false,
                                    remoteSongCount = playlist.songCountText?.let {
                                        Regex("""\d+""").find(it)?.value?.toIntOrNull()
                                    },
                                    playEndpointParams = playlist.playEndpoint?.params,
                                    shuffleEndpointParams = playlist.shuffleEndpoint?.params,
                                    radioEndpointParams = playlist.radioEndpoint?.params
                                ).toggleLike()
                                insert(playlistEntity)
                                coroutineScope.launch(Dispatchers.IO) {
                                    songs.ifEmpty {
                                        YouTube.playlist(playlist.id).completed()
                                            .getOrNull()?.songs.orEmpty()
                                    }.map { it.toMediaMetadata() }
                                        .onEach(::insert)
                                        .mapIndexed { index, song ->
                                            PlaylistSongMap(
                                                songId = song.id,
                                                playlistId = playlistEntity.id,
                                                position = index
                                            )
                                        }
                                        .forEach(::insert)
                                }
                            }
                        } else {
                            database.transaction {
                                // Update playlist information including thumbnail before toggling like
                                val currentPlaylist = dbPlaylist!!.playlist
                                update(currentPlaylist, playlist)
                                update(currentPlaylist.toggleLike())
                            }
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(if (dbPlaylist?.playlist?.bookmarkedAt != null) R.drawable.favorite else R.drawable.favorite_border),
                        tint = if (dbPlaylist?.playlist?.bookmarkedAt != null) MaterialTheme.colorScheme.error else LocalContentColor.current,
                        contentDescription = null
                    )
                }
            }
        }
    )
    HorizontalDivider()

    var downloadState by remember {
        mutableStateOf(Download.STATE_STOPPED)
    }
    LaunchedEffect(songs) {
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it.id]?.state == Download.STATE_COMPLETED })
                    Download.STATE_COMPLETED
                else if (songs.all {
                        downloads[it.id]?.state == Download.STATE_QUEUED
                                || downloads[it.id]?.state == Download.STATE_DOWNLOADING
                                || downloads[it.id]?.state == Download.STATE_COMPLETED
                    })
                    Download.STATE_DOWNLOADING
                else
                    Download.STATE_STOPPED
        }
    }
    var showRemoveDownloadDialog by remember {
        mutableStateOf(false)
    }
    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(
                        R.string.remove_download_playlist_confirm,
                        playlist.title
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            },
            buttons = {
                TextButton(
                    onClick = { showRemoveDownloadDialog = false }
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                        songs.forEach { song ->
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                song.id,
                                false
                            )
                        }
                    }
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            }
        )
    }

    ImportPlaylistDialog(
        isVisible = showImportPlaylistDialog,
        onGetSong = {
            val allSongs = songs
                .ifEmpty {
                    YouTube.playlist(playlist.id).completed().getOrNull()?.songs.orEmpty()
                }.map {
                    it.toMediaMetadata()
                }
            database.transaction {
                allSongs.forEach(::insert)
            }
            allSongs.map { it.id }
        },
        playlistTitle = playlist.title,
        onDismiss = { showImportPlaylistDialog = false }
    )

    if (showErrorPlaylistAddDialog) {
        ListDialog(
            onDismiss = {
                showErrorPlaylistAddDialog = false
                onDismiss()
            },
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                    Modifier
                        .height(ListItemHeight)
                        .clickable { showErrorPlaylistAddDialog = false }
                        .padding(horizontal = 12.dp),
                ) {
                    Image(
                        painter = painterResource(R.drawable.close),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                        modifier = Modifier
                            .size(ListThumbnailSize)
                            .padding(8.dp),
                    )
                    Text(
                        text = stringResource(R.string.already_in_playlist),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier =
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                    )
                }
            }

            items(notAddedList) { song ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                    Modifier
                        .height(ListItemHeight)
                        .padding(horizontal = 12.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(ListThumbnailSize)
                            .padding(8.dp),
                    ) {
                        AsyncImage(
                            model = song.thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = song.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = joinByBullet(
                                song.artists.joinToString { it.name },
                                makeTimeString(song.duration * 1000L),
                            ),
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    LazyColumn(
        contentPadding =
        PaddingValues(bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (songs.isNotEmpty()) {
            item {
                Material3MenuGroup(
                    items = listOf(
                        when (downloadState) {
                            Download.STATE_COMPLETED -> {
                                Material3MenuItem(
                                    title = {
                                        Text(
                                            text = stringResource(R.string.remove_download),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    icon = {
                                        Icon(
                                            painter = painterResource(R.drawable.offline),
                                            contentDescription = null,
                                        )
                                    },
                                    onClick = {
                                        showRemoveDownloadDialog = true
                                    },
                                    cardColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                                    textColor = MaterialTheme.colorScheme.error
                                )
                            }
                            Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> {
                                Material3MenuItem(
                                    title = { Text(text = stringResource(R.string.downloading)) },
                                    icon = {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    },
                                    onClick = {
                                        showRemoveDownloadDialog = true
                                    }
                                )
                            }
                            else -> {
                                Material3MenuItem(
                                    title = { Text(text = stringResource(R.string.action_download)) },
                                    icon = {
                                        Icon(
                                            painter = painterResource(R.drawable.download),
                                            contentDescription = null,
                                        )
                                    },
                                    onClick = {
                                        songs.forEach { song ->
                                            val downloadRequest = DownloadRequest.Builder(song.id, song.id.toUri())
                                                .setCustomCacheKey(song.id)
                                                .setData(song.title.toByteArray())
                                                .build()
                                            DownloadService.sendAddDownload(
                                                context,
                                                ExoDownloadService::class.java,
                                                downloadRequest,
                                                false
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    )
                )
            }
        }
        item {
            Material3MenuGroup(
                items = buildList {
                    playlist.playEndpoint?.let { playEndpoint ->
                        add(
                            Material3MenuItem(
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.play),
                                        contentDescription = null,
                                    )
                                },
                                title = { Text(stringResource(R.string.play)) },
                                onClick = {
                                    playerConnection.playQueue(YouTubeQueue(playEndpoint))
                                    onDismiss()
                                }
                            )
                        )
                    }
                    playlist.shuffleEndpoint?.let { shuffleEndpoint ->
                        add(
                            Material3MenuItem(
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.shuffle),
                                        contentDescription = null,
                                    )
                                },
                                title = { Text(stringResource(R.string.shuffle)) },
                                onClick = {
                                    playerConnection.playQueue(YouTubeQueue(shuffleEndpoint))
                                    onDismiss()
                                }
                            )
                        )
                    }
                    playlist.radioEndpoint?.let { radioEndpoint ->
                        add(
                            Material3MenuItem(
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.radio),
                                        contentDescription = null,
                                    )
                                },
                                title = { Text(stringResource(R.string.start_radio)) },
                                onClick = {
                                    playerConnection.playQueue(YouTubeQueue(radioEndpoint))
                                    onDismiss()
                                }
                            )
                        )
                    }
                }
            )
        }

        item {
            Material3MenuGroup(
                items = listOf(
                    Material3MenuItem(
                        title = { Text(text = stringResource(R.string.play_next)) },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.playlist_play),
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            coroutineScope.launch {
                                songs
                                    .ifEmpty {
                                        withContext(Dispatchers.IO) {
                                            YouTube
                                                .playlist(playlist.id)
                                                .completed()
                                                .getOrNull()
                                                ?.songs
                                                .orEmpty()
                                        }
                                    }.let { songs ->
                                        playerConnection.playNext(songs.map { it.copy(thumbnail = it.thumbnail.resize(544,544)).toMediaItem() })
                                    }
                            }
                            onDismiss()
                        }
                    ),
                    Material3MenuItem(
                        title = { Text(text = stringResource(R.string.add_to_queue)) },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.queue_music),
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            coroutineScope.launch {
                                songs
                                    .ifEmpty {
                                        withContext(Dispatchers.IO) {
                                            YouTube
                                                .playlist(playlist.id)
                                                .completed()
                                                .getOrNull()
                                                ?.songs
                                                .orEmpty()
                                        }
                                    }.let { songs ->
                                        playerConnection.addToQueue(songs.map { it.toMediaItem() })
                                    }
                            }
                            onDismiss()
                        }
                    ),
                    Material3MenuItem(
                        title = { Text(text = stringResource(R.string.add_to_playlist)) },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.playlist_add),
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            showChoosePlaylistDialog = true
                        }
                    )
                )
            )
        }
        item {
            Material3MenuGroup(
                items = buildList {
                    add(
                        Material3MenuItem(
                            title = { Text(text = stringResource(R.string.share)) },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.share),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                val intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, playlist.shareLink)
                                }
                                context.startActivity(Intent.createChooser(intent, null))
                                onDismiss()
                            }
                        )
                    )
                    if (canSelect) {
                        add(
                            Material3MenuItem(
                                title = { Text(text = stringResource(R.string.select)) },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.select_all),
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    onDismiss()
                                    selectAction()
                                }
                            )
                        )
                    }
                }
            )
        }
    }
}
