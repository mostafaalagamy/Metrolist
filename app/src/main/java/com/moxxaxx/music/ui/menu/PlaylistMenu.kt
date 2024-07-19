package com.moxxaxx.music.ui.menu

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastSumBy
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.malopieds.innertube.YouTube
import com.malopieds.innertube.models.SongItem
import com.malopieds.innertube.utils.completed
import com.moxxaxx.music.LocalDatabase
import com.moxxaxx.music.LocalDownloadUtil
import com.moxxaxx.music.LocalPlayerConnection
import com.moxxaxx.music.R
import com.moxxaxx.music.db.entities.Playlist
import com.moxxaxx.music.db.entities.PlaylistSong
import com.moxxaxx.music.db.entities.PlaylistSongMap
import com.moxxaxx.music.db.entities.Song
import com.moxxaxx.music.extensions.toMediaItem
import com.moxxaxx.music.models.toMediaMetadata
import com.moxxaxx.music.playback.ExoDownloadService
import com.moxxaxx.music.playback.queues.ListQueue
import com.moxxaxx.music.ui.component.DefaultDialog
import com.moxxaxx.music.ui.component.DownloadGridMenu
import com.moxxaxx.music.ui.component.GridMenu
import com.moxxaxx.music.ui.component.GridMenuItem
import com.moxxaxx.music.ui.component.PlaylistListItem
import com.moxxaxx.music.ui.component.TextFieldDialog
import com.moxxaxx.music.utils.makeTimeString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@Composable
fun PlaylistMenu(
    playlist: Playlist,
    coroutineScope: CoroutineScope,
    onDismiss: () -> Unit,
    autoPlaylist: Boolean? = false,
    downloadPlaylist: Boolean? = false,
    songList: List<Song>? = emptyList(),
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val playerConnection = LocalPlayerConnection.current ?: return
    var songs by remember {
        mutableStateOf(emptyList<Song>())
    }
    val playlistLength =
        remember(songs) {
            songs.fastSumBy { it.song.duration }
        }

    LaunchedEffect(Unit) {
        if (autoPlaylist == false) {
            database.playlistSongs(playlist.id).collect {
                songs = it.map(PlaylistSong::song)
            }
        } else {
            if (songList != null) {
                songs = songList
            }
        }
    }

    var downloadState by remember {
        mutableIntStateOf(Download.STATE_STOPPED)
    }

    LaunchedEffect(songs) {
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it.id]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songs.all {
                        downloads[it.id]?.state == Download.STATE_QUEUED ||
                            downloads[it.id]?.state == Download.STATE_DOWNLOADING ||
                            downloads[it.id]?.state == Download.STATE_COMPLETED
                    }
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }

    var showEditDialog by remember {
        mutableStateOf(false)
    }

    if (showEditDialog) {
        TextFieldDialog(
            icon = { Icon(painter = painterResource(R.drawable.edit), contentDescription = null) },
            title = { Text(text = stringResource(R.string.edit_playlist)) },
            onDismiss = { showEditDialog = false },
            initialTextFieldValue =
                TextFieldValue(
                    playlist.playlist.name,
                    TextRange(playlist.playlist.name.length),
                ),
            onDone = { name ->
                onDismiss()
                database.query {
                    update(playlist.playlist.copy(name = name, lastUpdateTime = LocalDateTime.now()))
                }
            },
        )
    }

    var showRemoveDownloadDialog by remember {
        mutableStateOf(false)
    }

    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.remove_download_playlist_confirm, playlist.playlist.name),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                    },
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
                                song.song.id,
                                false,
                            )
                        }
                    },
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
        )
    }

    var showDeletePlaylistDialog by remember {
        mutableStateOf(false)
    }

    if (showDeletePlaylistDialog) {
        DefaultDialog(
            onDismiss = { showDeletePlaylistDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.delete_playlist_confirm, playlist.playlist.name),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                TextButton(
                    onClick = {
                        showDeletePlaylistDialog = false
                    },
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }

                TextButton(
                    onClick = {
                        showDeletePlaylistDialog = false
                        onDismiss()
                        database.query {
                            delete(playlist.playlist)
                        }
                    },
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
        )
    }

    PlaylistListItem(
        playlist = playlist,
        trailingContent = {
            Text(
                text = makeTimeString(playlistLength * 1000L),
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 12.dp),
            )
        },
    )

    Divider()

    GridMenu(
        contentPadding =
            PaddingValues(
                start = 8.dp,
                top = 8.dp,
                end = 8.dp,
                bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
            ),
    ) {
        GridMenuItem(
            icon = R.drawable.play,
            title = R.string.play,
        ) {
            onDismiss()
            playerConnection.playQueue(
                ListQueue(
                    title = playlist.playlist.name,
                    items = songs.map { it.toMediaItem() },
                ),
            )
        }

        GridMenuItem(
            icon = R.drawable.shuffle,
            title = R.string.shuffle,
        ) {
            onDismiss()
            playerConnection.playQueue(
                ListQueue(
                    title = playlist.playlist.name,
                    items = songs.shuffled().map { it.toMediaItem() },
                ),
            )
        }

        GridMenuItem(
            icon = R.drawable.queue_music,
            title = R.string.add_to_queue,
        ) {
            onDismiss()
            playerConnection.addToQueue(songs.map { it.toMediaItem() })
        }

        if (autoPlaylist != true) {
            GridMenuItem(
                icon = R.drawable.edit,
                title = R.string.edit,
            ) {
                showEditDialog = true
            }
        }

        if (downloadPlaylist != true) {
            DownloadGridMenu(
                state = downloadState,
                onDownload = {
                    songs.forEach { song ->
                        val downloadRequest =
                            DownloadRequest
                                .Builder(song.id, song.id.toUri())
                                .setCustomCacheKey(song.id)
                                .setData(song.song.title.toByteArray())
                                .build()
                        DownloadService.sendAddDownload(
                            context,
                            ExoDownloadService::class.java,
                            downloadRequest,
                            false,
                        )
                    }
                },
                onRemoveDownload = {
                    showRemoveDownloadDialog = true
                },
            )
        }

        if (autoPlaylist != true) {
            GridMenuItem(
                icon = R.drawable.delete,
                title = R.string.delete,
            ) {
                showDeletePlaylistDialog = true
            }
        }

        if (playlist.playlist.browseId != null) {
            GridMenuItem(
                icon = R.drawable.sync,
                title = R.string.sync,
            ) {
                onDismiss()
                coroutineScope.launch(Dispatchers.IO) {
                    val playlistPage =
                        YouTube.playlist(playlist.playlist.browseId).completed().getOrNull()
                            ?: return@launch
                    database.transaction {
                        clearPlaylist(playlist.id)
                        playlistPage.songs
                            .map(SongItem::toMediaMetadata)
                            .onEach(::insert)
                            .mapIndexed { position, song ->
                                PlaylistSongMap(
                                    songId = song.id,
                                    playlistId = playlist.id,
                                    position = position,
                                )
                            }.forEach(::insert)
                    }
                }
            }
        }
    }
}
