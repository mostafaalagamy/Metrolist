package com.malopieds.innertune.ui.menu

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.malopieds.innertube.YouTube
import com.malopieds.innertube.models.PlaylistItem
import com.malopieds.innertube.models.SongItem
import com.malopieds.innertube.utils.completed
import com.malopieds.innertune.LocalDatabase
import com.malopieds.innertune.LocalPlayerConnection
import com.malopieds.innertune.R
import com.malopieds.innertune.constants.ListThumbnailSize
import com.malopieds.innertune.constants.ThumbnailCornerRadius
import com.malopieds.innertune.db.entities.PlaylistSongMap
import com.malopieds.innertune.extensions.toMediaItem
import com.malopieds.innertune.models.MediaMetadata
import com.malopieds.innertune.models.toMediaMetadata
import com.malopieds.innertune.playback.queues.YouTubeQueue
import com.malopieds.innertune.ui.component.GridMenu
import com.malopieds.innertune.ui.component.GridMenuItem
import com.malopieds.innertune.ui.component.ListDialog
import com.malopieds.innertune.ui.component.ListItem
import com.malopieds.innertune.utils.joinByBullet
import com.malopieds.innertune.utils.makeTimeString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

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
    val playerConnection = LocalPlayerConnection.current ?: return

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
        onAdd = { targetPlaylist ->
            coroutineScope.launch(Dispatchers.IO) {
                var position = targetPlaylist.songCount
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
                        database.transaction {
                            songs
                                .map { it.toMediaMetadata() }
                                .onEach(::insert)
                                .forEach { song ->
                                    if (checkInPlaylist(playlist.id, song.id) == 0) {
                                        insert(
                                            PlaylistSongMap(
                                                songId = song.id,
                                                playlistId = targetPlaylist.id,
                                                position = position++,
                                            ),
                                        )
                                        update(targetPlaylist.playlist.copy(lastUpdateTime = LocalDateTime.now()))
                                        onDismiss()
                                    } else {
                                        notAddedList.add(song)
                                        showErrorPlaylistAddDialog = true
                                    }
                                }
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

    GridMenu(
        contentPadding =
            PaddingValues(
                start = 8.dp,
                top = 8.dp,
                end = 8.dp,
                bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
            ),
    ) {
        playlist.playEndpoint?.let {
            GridMenuItem(
                icon = R.drawable.play,
                title = R.string.play,
            ) {
                playerConnection.playQueue(YouTubeQueue(it))
                onDismiss()
            }
        }
        GridMenuItem(
            icon = R.drawable.shuffle,
            title = R.string.shuffle,
        ) {
            playerConnection.playQueue(YouTubeQueue(playlist.shuffleEndpoint))
            onDismiss()
        }
        playlist.radioEndpoint?.let { radioEndpoint ->
            GridMenuItem(
                icon = R.drawable.radio,
                title = R.string.start_radio,
            ) {
                playerConnection.playQueue(YouTubeQueue(radioEndpoint))
                onDismiss()
            }
        }
        GridMenuItem(
            icon = R.drawable.playlist_play,
            title = R.string.play_next,
        ) {
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
                        playerConnection.playNext(songs.map { it.toMediaItem() })
                    }
            }
            onDismiss()
        }
        GridMenuItem(
            icon = R.drawable.queue_music,
            title = R.string.add_to_queue,
        ) {
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
        GridMenuItem(
            icon = R.drawable.playlist_add,
            title = R.string.add_to_playlist,
        ) {
            showChoosePlaylistDialog = true
        }
        GridMenuItem(
            icon = R.drawable.share,
            title = R.string.share,
        ) {
            val intent =
                Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, playlist.shareLink)
                }
            context.startActivity(Intent.createChooser(intent, null))
            onDismiss()
        }

        if (canSelect) {
            GridMenuItem(
                icon = R.drawable.select_all,
                title = R.string.select,
            ) {
                onDismiss()
                selectAction()
            }
        }
    }
}
