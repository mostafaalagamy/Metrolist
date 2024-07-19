package com.moxxaxx.music.ui.menu

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import coil.compose.AsyncImage
import com.moxxaxx.music.LocalDatabase
import com.moxxaxx.music.LocalDownloadUtil
import com.moxxaxx.music.LocalPlayerConnection
import com.moxxaxx.music.R
import com.moxxaxx.music.constants.ListThumbnailSize
import com.moxxaxx.music.constants.ThumbnailCornerRadius
import com.moxxaxx.music.db.entities.PlaylistSongMap
import com.moxxaxx.music.db.entities.Song
import com.moxxaxx.music.extensions.toMediaItem
import com.moxxaxx.music.models.MediaMetadata
import com.moxxaxx.music.models.toMediaMetadata
import com.moxxaxx.music.playback.ExoDownloadService
import com.moxxaxx.music.playback.queues.ListQueue
import com.moxxaxx.music.ui.component.DefaultDialog
import com.moxxaxx.music.ui.component.DownloadGridMenu
import com.moxxaxx.music.ui.component.GridMenu
import com.moxxaxx.music.ui.component.GridMenuItem
import com.moxxaxx.music.ui.component.ListDialog
import com.moxxaxx.music.ui.component.ListItem
import com.moxxaxx.music.ui.component.SongListItem
import com.moxxaxx.music.utils.joinByBullet
import com.moxxaxx.music.utils.makeTimeString
import java.time.LocalDateTime

@SuppressLint("MutableCollectionMutableState")
@Composable
fun SelectionSongMenu(
    songSelection: List<Song>,
    onDismiss: () -> Unit,
    clearAction: () -> Unit,
    songPosition: List<PlaylistSongMap>? = emptyList(),
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val allInLibrary by remember {
        mutableStateOf(
            songSelection.all {
                it.song.inLibrary != null
            },
        )
    }

    var downloadState by remember {
        mutableIntStateOf(Download.STATE_STOPPED)
    }

    LaunchedEffect(songSelection) {
        if (songSelection.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songSelection.all { downloads[it.id]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songSelection.all {
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

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showErrorPlaylistAddDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val notAddedList by remember {
        mutableStateOf(mutableListOf<Song>())
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onAdd = { playlist ->
            var position = playlist.songCount
            database.query {
                songSelection.forEach { song ->
                    if (checkInPlaylist(playlist.id, song.id) == 0) {
                        insert(
                            PlaylistSongMap(
                                songId = song.id,
                                playlistId = playlist.id,
                                position = position++,
                            ),
                        )
                        update(playlist.playlist.copy(lastUpdateTime = LocalDateTime.now()))
                        onDismiss()
                    } else {
                        notAddedList.add(song)
                        showErrorPlaylistAddDialog = true
                    }
                }
            }
        },
        onDismiss = { showChoosePlaylistDialog = false },
    )

    if (showErrorPlaylistAddDialog) {
        ListDialog(
            onDismiss = {
                showErrorPlaylistAddDialog = false
                onDismiss()
            },
        ) {
            item {
                ListItem(
                    title = "Not added:",
                    thumbnailContent = {
                        Image(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier.size(ListThumbnailSize),
                        )
                    },
                    modifier =
                        Modifier
                            .clickable { showErrorPlaylistAddDialog = false },
                )
            }

            items(notAddedList) { song ->
                SongListItem(song = song)
            }
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
                    text = stringResource(R.string.remove_download_playlist_confirm, "selection"),
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
                        songSelection.forEach { song ->
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
                    title = "Selection",
                    items = songSelection.map { it.toMediaItem() },
                ),
            )
            clearAction()
        }

        GridMenuItem(
            icon = R.drawable.shuffle,
            title = R.string.shuffle,
        ) {
            onDismiss()
            playerConnection.playQueue(
                ListQueue(
                    title = "Selection",
                    items = songSelection.shuffled().map { it.toMediaItem() },
                ),
            )
            clearAction()
        }

        if (allInLibrary) {
            GridMenuItem(
                icon = R.drawable.library_add_check,
                title = R.string.remove_from_library,
            ) {
                database.query {
                    songSelection.forEach { song ->
                        inLibrary(song.id, null)
                    }
                }
            }
        } else {
            GridMenuItem(
                icon = R.drawable.library_add,
                title = R.string.add_to_library,
            ) {
                database.transaction {
                    songSelection.forEach { song ->
                        insert(song.toMediaMetadata())
                        inLibrary(song.id, LocalDateTime.now())
                    }
                }
            }
        }

        GridMenuItem(
            icon = R.drawable.queue_music,
            title = R.string.add_to_queue,
        ) {
            onDismiss()
            playerConnection.addToQueue(songSelection.map { it.toMediaItem() })
            clearAction()
        }

        GridMenuItem(
            icon = R.drawable.playlist_add,
            title = R.string.add_to_playlist,
        ) {
            showChoosePlaylistDialog = true
        }

        DownloadGridMenu(
            state = downloadState,
            onDownload = {
                songSelection.forEach { song ->
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

        GridMenuItem(
            icon = if (songSelection.all { it.song.liked }) R.drawable.favorite else R.drawable.favorite_border,
            title = R.string.like_all,
        ) {
            val allLiked =
                songSelection.all {
                    it.song.liked
                }
            onDismiss()
            database.query {
                songSelection.forEach { song ->
                    if ((!allLiked && !song.song.liked) || allLiked) {
                        update(song.song.toggleLike())
                    }
                }
            }
        }

        if (songPosition?.size != 0) {
            GridMenuItem(
                icon = R.drawable.delete,
                title = R.string.delete,
            ) {
                onDismiss()
                var i = 0
                database.query {
                    songPosition?.forEach { cur ->
                        move(cur.playlistId, cur.position - i, Int.MAX_VALUE)
                        delete(cur.copy(position = Int.MAX_VALUE))
                        i++
                    }
                }
                clearAction()
            }
        }
    }
}

@SuppressLint("MutableCollectionMutableState")
@Composable
fun SelectionMediaMetadataMenu(
    songSelection: List<MediaMetadata>,
    currentItems: List<Timeline.Window>,
    onDismiss: () -> Unit,
    clearAction: () -> Unit,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val playerConnection = LocalPlayerConnection.current ?: return

    var downloadState by remember {
        mutableIntStateOf(Download.STATE_STOPPED)
    }

    LaunchedEffect(songSelection) {
        if (songSelection.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songSelection.all { downloads[it.id]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songSelection.all {
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

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showErrorPlaylistAddDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val notAddedList by remember {
        mutableStateOf(mutableListOf<MediaMetadata>())
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onAdd = { playlist ->
            database.query {
                var position = playlist.songCount
                songSelection.forEach { song ->
                    insert(song)
                    if (checkInPlaylist(playlist.id, song.id) == 0) {
                        insert(
                            PlaylistSongMap(
                                songId = song.id,
                                playlistId = playlist.id,
                                position = position++,
                            ),
                        )
                        update(playlist.playlist.copy(lastUpdateTime = LocalDateTime.now()))
                        onDismiss()
                    } else {
                        notAddedList.add(song)
                        showErrorPlaylistAddDialog = true
                    }
                }
            }
        },
        onDismiss = { showChoosePlaylistDialog = false },
    )
    if (showErrorPlaylistAddDialog) {
        ListDialog(
            onDismiss = {
                showErrorPlaylistAddDialog = false
                onDismiss()
            },
        ) {
            item {
                ListItem(
                    title = stringResource(R.string.already_in_playlist),
                    thumbnailContent = {
                        Image(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier.size(ListThumbnailSize),
                        )
                    },
                    modifier =
                        Modifier
                            .clickable { showErrorPlaylistAddDialog = false },
                )
            }

            items(notAddedList) { song ->
                ListItem(
                    title = song.title,
                    thumbnailContent = {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(ListThumbnailSize),
                        ) {
                            AsyncImage(
                                model = song.thumbnailUrl,
                                contentDescription = null,
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                            )
                        }
                    },
                    subtitle =
                        joinByBullet(
                            song.artists.joinToString { it.name },
                            makeTimeString(song.duration * 1000L),
                        ),
                )
            }
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
                    text = stringResource(R.string.remove_download_playlist_confirm, "selection"),
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
                        songSelection.forEach { song ->
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                song.id,
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

    GridMenu(
        contentPadding =
            PaddingValues(
                start = 8.dp,
                top = 8.dp,
                end = 8.dp,
                bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
            ),
    ) {
        if (currentItems.isNotEmpty()) {
            GridMenuItem(
                icon = R.drawable.delete,
                title = R.string.delete,
            ) {
                onDismiss()
                var i = 0
                currentItems.forEach { cur ->
                    playerConnection.player.removeMediaItem(cur.firstPeriodIndex - i)
                    i++
                }
                clearAction()
            }
        }

        GridMenuItem(
            icon = R.drawable.play,
            title = R.string.play,
        ) {
            onDismiss()
            playerConnection.playQueue(
                ListQueue(
                    title = "Selection",
                    items = songSelection.map { it.toMediaItem() },
                ),
            )
            clearAction()
        }

        GridMenuItem(
            icon = R.drawable.shuffle,
            title = R.string.shuffle,
        ) {
            onDismiss()
            playerConnection.playQueue(
                ListQueue(
                    title = "Selection",
                    items = songSelection.shuffled().map { it.toMediaItem() },
                ),
            )
            clearAction()
        }

        GridMenuItem(
            icon = R.drawable.queue_music,
            title = R.string.add_to_queue,
        ) {
            onDismiss()
            playerConnection.addToQueue(songSelection.map { it.toMediaItem() })
            clearAction()
        }

        GridMenuItem(
            icon = R.drawable.playlist_add,
            title = R.string.add_to_playlist,
        ) {
            showChoosePlaylistDialog = true
        }

        DownloadGridMenu(
            state = downloadState,
            onDownload = {
                songSelection.forEach { song ->
                    val downloadRequest =
                        DownloadRequest
                            .Builder(song.id, song.id.toUri())
                            .setCustomCacheKey(song.id)
                            .setData(song.title.toByteArray())
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
}
