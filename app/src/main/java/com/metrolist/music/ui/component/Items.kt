package com.metrolist.music.ui.component

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandIn
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.zIndex
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.Download.STATE_COMPLETED
import androidx.media3.exoplayer.offline.Download.STATE_DOWNLOADING
import androidx.media3.exoplayer.offline.Download.STATE_QUEUED
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.YTItem
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalDownloadUtil
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.ListItemHeight
import com.metrolist.music.constants.GridThumbnailHeight
import com.metrolist.music.constants.ListThumbnailSize
import com.metrolist.music.constants.ThumbnailCornerRadius
import com.metrolist.music.constants.SwipeToSongKey
import com.metrolist.music.db.entities.Song
import com.metrolist.music.db.entities.Album
import com.metrolist.music.db.entities.Artist
import com.metrolist.music.db.entities.Playlist
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.playback.queues.LocalAlbumRadio
import com.metrolist.music.ui.theme.extractThemeColor
import com.metrolist.music.utils.joinByBullet
import com.metrolist.music.utils.makeTimeString
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.utils.reportException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.logging.Logger
import kotlin.math.roundToInt

const val ActiveBoxAlpha = 0.6f

@Composable
inline fun ListItem(
    modifier: Modifier = Modifier,
    title: String,
    noinline subtitle: (@Composable RowScope.() -> Unit)? = null,
    thumbnailContent: @Composable () -> Unit,
    trailingContent: @Composable RowScope.() -> Unit = {},
    isActive: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(ListItemHeight)
            .padding(horizontal = 8.dp)
            .then(if (isActive) Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.secondaryContainer) else Modifier)
    ) {
        Box(Modifier.padding(6.dp), contentAlignment = Alignment.Center) { thumbnailContent() }
        Column(Modifier.weight(1f).padding(horizontal = 6.dp)) {
            Text(
                text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) Row(verticalAlignment = Alignment.CenterVertically) { subtitle() }
        }
        trailingContent()
    }
}

@Composable
fun ListItem(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String?,
    badges: @Composable RowScope.() -> Unit = {},
    thumbnailContent: @Composable () -> Unit,
    trailingContent: @Composable RowScope.() -> Unit = {},
    isActive: Boolean = false
) = ListItem(
    title = title,
    modifier = modifier,
    isActive = isActive,
    subtitle = {
        badges()
        if (!subtitle.isNullOrEmpty()) {
            Text(text = subtitle, color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    },
    thumbnailContent = thumbnailContent,
    trailingContent = trailingContent
)

@Composable
fun GridItem(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    subtitle: @Composable () -> Unit,
    badges: @Composable RowScope.() -> Unit = {},
    thumbnailContent: @Composable BoxWithConstraintsScope.() -> Unit,
    thumbnailRatio: Float = 1f,
    fillMaxWidth: Boolean = false,
) {
    Column(
        modifier = if (fillMaxWidth) {
            modifier
                .padding(12.dp)
                .fillMaxWidth()
        } else {
            modifier
                .padding(12.dp)
                .width(GridThumbnailHeight * thumbnailRatio)
        }
    ) {
        BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier = if (fillMaxWidth) {
                Modifier.fillMaxWidth()
            } else {
                Modifier.height(GridThumbnailHeight)
            }
                .aspectRatio(thumbnailRatio)
        ) {
            thumbnailContent()
        }

        Spacer(modifier = Modifier.height(6.dp))

        title()

        Row(verticalAlignment = Alignment.CenterVertically) {
            badges()

            subtitle()
        }
    }
}

@Composable
fun GridItem(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    badges: @Composable RowScope.() -> Unit = {},
    thumbnailContent: @Composable BoxWithConstraintsScope.() -> Unit,
    thumbnailRatio: Float = 1f,
    fillMaxWidth: Boolean = false,
) = GridItem(
    modifier = modifier,
    title = {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
    },
    subtitle = {
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    },
    thumbnailContent = thumbnailContent,
    thumbnailRatio = thumbnailRatio,
    fillMaxWidth = fillMaxWidth
)

@Composable
fun SongListItem(
    song: Song,
    modifier: Modifier = Modifier,
    albumIndex: Int? = null,
    showLikedIcon: Boolean = true,
    showInLibraryIcon: Boolean = false,
    showDownloadIcon: Boolean = true,
    badges: @Composable RowScope.() -> Unit = {
        if (showLikedIcon && song.song.liked) {
            Icon.Favorite()
        }
        if (showInLibraryIcon && song.song.inLibrary != null) {
            Icon.Library()
        }
        if (showDownloadIcon) {
            val download by LocalDownloadUtil.current.getDownload(song.id)
                .collectAsState(initial = null)
            Icon.Download(download?.state)
        }
    },
    isSelected: Boolean = false,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    isSwipeable: Boolean = true,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    val swipeEnabled by rememberPreference(SwipeToSongKey, defaultValue = false)

    val content: @Composable () -> Unit = {
        ListItem(
            title = song.song.title,
            subtitle = joinByBullet(
                song.artists.joinToString { it.name },
                makeTimeString(song.song.duration * 1000L)
            ),
            badges = badges,
            thumbnailContent = {
                ItemThumbnail(
                    thumbnailUrl = song.song.thumbnailUrl,
                    albumIndex = albumIndex,
                    isSelected = isSelected,
                    isActive = isActive,
                    isPlaying = isPlaying,
                    shape = RoundedCornerShape(ThumbnailCornerRadius),
                    modifier = Modifier.size(ListThumbnailSize)
                )
            },
            trailingContent = trailingContent,
            modifier = modifier,
            isActive = isActive
        )
    }

    if (isSwipeable && swipeEnabled) {
        SwipeToSongBox(
            mediaItem = song.toMediaItem(),
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    } else {
        content()
    }
}

@Composable
fun SongGridItem(
    song: Song,
    modifier: Modifier = Modifier,
    showLikedIcon: Boolean = true,
    showInLibraryIcon: Boolean = false,
    showDownloadIcon: Boolean = true,
    badges: @Composable RowScope.() -> Unit = {
        if (showLikedIcon && song.song.liked) {
            Icon.Favorite()
        }
        if (showInLibraryIcon && song.song.inLibrary != null) {
            Icon.Library()
        }
        if (showDownloadIcon) {
            val download by LocalDownloadUtil.current.getDownload(song.id).collectAsState(initial = null)
            Icon.Download(download?.state)
        }
    },
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
) = GridItem(
    title = {
        Text(
            text = song.song.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.basicMarquee().fillMaxWidth()
        )
    },
    subtitle = {
        Text(
            text = joinByBullet(
                song.artists.joinToString { it.name },
                makeTimeString(song.song.duration * 1000L)
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    },
    badges = badges,
    thumbnailContent = {
        ItemThumbnail(
            thumbnailUrl = song.song.thumbnailUrl,
            isActive = isActive,
            isPlaying = isPlaying,
            shape = RoundedCornerShape(ThumbnailCornerRadius),
            modifier = Modifier.size(GridThumbnailHeight)
        )
        if (!isActive) {
            OverlayPlayButton(
                visible = true
            )
        }
    },
    fillMaxWidth = fillMaxWidth,
    modifier = modifier
)

@Composable
fun ArtistListItem(
    artist: Artist,
    modifier: Modifier = Modifier,
    badges: @Composable RowScope.() -> Unit = {
        if (artist.artist.bookmarkedAt != null) {
            Icon(
                painter = painterResource(R.drawable.favorite),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .size(18.dp)
                    .padding(end = 2.dp),
            )
        }
    },
    trailingContent: @Composable RowScope.() -> Unit = {},
) = ListItem(
    title = artist.artist.name,
    subtitle = pluralStringResource(R.plurals.n_song, artist.songCount, artist.songCount),
    badges = badges,
    thumbnailContent = {
        AsyncImage(
            model = artist.artist.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(ListThumbnailSize)
                .clip(CircleShape),
        )
    },
    trailingContent = trailingContent,
    modifier = modifier,
)

@Composable
fun ArtistGridItem(
    artist: Artist,
    modifier: Modifier = Modifier,
    badges: @Composable RowScope.() -> Unit = {
        if (artist.artist.bookmarkedAt != null) {
            Icon.Favorite()
        }
    },
    fillMaxWidth: Boolean = false,
) = GridItem(
    title = artist.artist.name,
    subtitle = pluralStringResource(R.plurals.n_song, artist.songCount, artist.songCount),
    badges = badges,
    thumbnailContent = {
        AsyncImage(
            model = artist.artist.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
        )
    },
    fillMaxWidth = fillMaxWidth,
    modifier = modifier
)

@Composable
fun AlbumListItem(
    album: Album,
    modifier: Modifier = Modifier,
    showLikedIcon: Boolean = true,
    badges: @Composable RowScope.() -> Unit = {
        val database = LocalDatabase.current
        val downloadUtil = LocalDownloadUtil.current
        var songs by remember {
            mutableStateOf(emptyList<Song>())
        }

        LaunchedEffect(Unit) {
            database.albumSongs(album.id).collect {
                songs = it
            }
        }

        var downloadState by remember {
            mutableStateOf(Download.STATE_STOPPED)
        }

        LaunchedEffect(songs) {
            if (songs.isEmpty()) return@LaunchedEffect
            downloadUtil.downloads.collect { downloads ->
                downloadState = when {
                    songs.all { downloads[it.id]?.state == STATE_COMPLETED } -> STATE_COMPLETED
                    songs.all { downloads[it.id]?.state in listOf(STATE_QUEUED, STATE_DOWNLOADING, STATE_COMPLETED) } -> STATE_DOWNLOADING
                    else -> Download.STATE_STOPPED
                }
            }
        }

        if (showLikedIcon && album.album.bookmarkedAt != null) {
            Icon.Favorite()
        }

        Icon.Download(downloadState)
    },
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    trailingContent: @Composable RowScope.() -> Unit = {},
) = ListItem(
    title = album.album.title,
    subtitle = joinByBullet(
        album.artists.joinToString { it.name },
        pluralStringResource(R.plurals.n_song, album.album.songCount, album.album.songCount),
        album.album.year?.toString()
    ),
    badges = badges,
    thumbnailContent = {
        ItemThumbnail(
            thumbnailUrl = album.album.thumbnailUrl,
            isActive = isActive,
            isPlaying = isPlaying,
            shape = RoundedCornerShape(ThumbnailCornerRadius),
            modifier = Modifier.size(ListThumbnailSize)
        )
    },
    trailingContent = trailingContent,
    modifier = modifier
)

@Composable
fun AlbumGridItem(
    album: Album,
    modifier: Modifier = Modifier,
    coroutineScope: CoroutineScope,
    badges: @Composable RowScope.() -> Unit = {
        val database = LocalDatabase.current
        val downloadUtil = LocalDownloadUtil.current
        var songs by remember { mutableStateOf(emptyList<Song>()) }

        LaunchedEffect(Unit) {
            database.albumSongs(album.id).collect { songs = it }
        }

        var downloadState by remember { mutableStateOf(Download.STATE_STOPPED) }

        LaunchedEffect(songs) {
            if (songs.isEmpty()) return@LaunchedEffect
            downloadUtil.downloads.collect { downloads ->
                downloadState = when {
                    songs.all { downloads[it.id]?.state == STATE_COMPLETED } -> STATE_COMPLETED
                    songs.all { downloads[it.id]?.state in listOf(STATE_QUEUED, STATE_DOWNLOADING, STATE_COMPLETED) } -> STATE_DOWNLOADING
                    else -> Download.STATE_STOPPED
                }
            }
        }

        if (album.album.bookmarkedAt != null) {
            Icon.Favorite()
        }
        Icon.Download(downloadState)
    },
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
) = GridItem(
    title = {
        Text(
            text = album.album.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.basicMarquee().fillMaxWidth()
        )
    },
    subtitle = {
        Text(
            text = album.artists.joinToString { it.name },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    },
    badges = badges,
    thumbnailContent = {
        val database = LocalDatabase.current
        val playerConnection = LocalPlayerConnection.current ?: return@GridItem

        ItemThumbnail(
            thumbnailUrl = album.album.thumbnailUrl,
            isActive = isActive,
            isPlaying = isPlaying,
            shape = RoundedCornerShape(ThumbnailCornerRadius),
        )

        AlbumPlayButton(
            visible = !isActive,
            onClick = {
                coroutineScope.launch {
                    database.albumWithSongs(album.id).firstOrNull()?.let { albumWithSongs ->
                        playerConnection.playQueue(LocalAlbumRadio(albumWithSongs))
                    }
                }
            }
        )
    },
    fillMaxWidth = fillMaxWidth,
    modifier = modifier
)

@Composable
fun PlaylistListItem(
    playlist: Playlist,
    modifier: Modifier = Modifier,
    autoPlaylist: Boolean = false,
    badges: @Composable RowScope.() -> Unit = {},
    trailingContent: @Composable RowScope.() -> Unit = {}
) = ListItem(
    title = playlist.playlist.name,
    subtitle = if (autoPlaylist) {
        ""
    } else {
        if (playlist.songCount == 0 && playlist.playlist.remoteSongCount != null) {
            pluralStringResource(
                R.plurals.n_song,
                playlist.playlist.remoteSongCount,
                playlist.playlist.remoteSongCount
            )
        } else {
            pluralStringResource(
                R.plurals.n_song,
                playlist.songCount,
                playlist.songCount
            )
        }
    },
    badges = badges,
    thumbnailContent = {
        PlaylistThumbnail(
            thumbnails = playlist.thumbnails,
            size = ListThumbnailSize,
            placeHolder = {
                val painter = when (playlist.playlist.name) {
                    stringResource(R.string.liked) -> R.drawable.favorite_border
                    stringResource(R.string.offline) -> R.drawable.offline
                    stringResource(R.string.cached_playlist) -> R.drawable.cached
                    else -> if (autoPlaylist) R.drawable.trending_up else R.drawable.queue_music
                }
                Icon(
                    painter = painterResource(painter),
                    contentDescription = null,
                    tint = LocalContentColor.current.copy(alpha = 0.8f),
                    modifier = Modifier.size(ListThumbnailSize / 2)
                )
            },
            shape = RoundedCornerShape(ThumbnailCornerRadius)
        )
    },
    trailingContent = trailingContent,
    modifier = modifier
)

@Composable
fun PlaylistGridItem(
    playlist: Playlist,
    modifier: Modifier = Modifier,
    autoPlaylist: Boolean = false,
    badges: @Composable RowScope.() -> Unit = {},
    fillMaxWidth: Boolean = false,
) = GridItem(
    title = {
        Text(
            text = playlist.playlist.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.basicMarquee().fillMaxWidth()
        )
    },
    subtitle = {
        val subtitle = if (autoPlaylist) {
            ""
        } else {
            if (playlist.songCount == 0 && playlist.playlist.remoteSongCount != null) {
                pluralStringResource(
                    R.plurals.n_song,
                    playlist.playlist.remoteSongCount,
                    playlist.playlist.remoteSongCount
                )
            } else {
                pluralStringResource(
                    R.plurals.n_song,
                    playlist.songCount,
                    playlist.songCount
                )
            }
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    },
    badges = badges,
    thumbnailContent = {
        val width = maxWidth
        PlaylistThumbnail(
            thumbnails = playlist.thumbnails,
            size = width,
            placeHolder = {
                val painter = when (playlist.playlist.name) {
                    stringResource(R.string.liked) -> R.drawable.favorite_border
                    stringResource(R.string.offline) -> R.drawable.offline
                    stringResource(R.string.cached_playlist) -> R.drawable.cached
                    else -> if (autoPlaylist) R.drawable.trending_up else R.drawable.queue_music
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        painter = painterResource(painter),
                        contentDescription = null,
                        tint = LocalContentColor.current.copy(alpha = 0.8f),
                        modifier = Modifier.size(width / 2)
                    )
                }
            },
            shape = RoundedCornerShape(ThumbnailCornerRadius)
        )
    },
    fillMaxWidth = fillMaxWidth,
    modifier = modifier
)

@Composable
fun MediaMetadataListItem(
    mediaMetadata: MediaMetadata,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    ListItem(
        title = mediaMetadata.title,
        subtitle = joinByBullet(
            mediaMetadata.artists.joinToString { it.name },
            makeTimeString(mediaMetadata.duration * 1000L)
        ),
        thumbnailContent = {
            ItemThumbnail(
                thumbnailUrl = mediaMetadata.thumbnailUrl,
                albumIndex = null,
                isSelected = isSelected,
                isActive = isActive,
                isPlaying = isPlaying,
                shape = RoundedCornerShape(ThumbnailCornerRadius),
                modifier = Modifier.size(ListThumbnailSize)
            )
        },
        trailingContent = trailingContent,
        modifier = modifier,
        isActive = isActive
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeListItem(
    item: YTItem,
    modifier: Modifier = Modifier,
    albumIndex: Int? = null,
    isSelected: Boolean = false,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    isSwipeable: Boolean = true,
    trailingContent: @Composable RowScope.() -> Unit = {},
    badges: @Composable RowScope.() -> Unit = {
        val database = LocalDatabase.current
        val song by database.song(item.id).collectAsState(initial = null)
        val album by database.album(item.id).collectAsState(initial = null)

        if ((item is SongItem && song?.song?.liked == true) ||
            (item is AlbumItem && album?.album?.bookmarkedAt != null)
        ) {
            Icon.Favorite()
        }
        if (item.explicit) Icon.Explicit()
        if (item is SongItem && song?.song?.inLibrary != null) {
            Icon.Library()
        }
        if (item is SongItem) {
            val downloads by LocalDownloadUtil.current.downloads.collectAsState()
            Icon.Download(downloads[item.id]?.state)
        }
    },
) {
    val swipeEnabled by rememberPreference(SwipeToSongKey, defaultValue = false)

    val content: @Composable () -> Unit = {
        ListItem(
            title = item.title,
            subtitle = when (item) {
                is SongItem -> joinByBullet(item.artists.joinToString { it.name }, makeTimeString(item.duration?.times(1000L)))
                is AlbumItem -> joinByBullet(item.artists?.joinToString { it.name }, item.year?.toString())
                is ArtistItem -> null
                is PlaylistItem -> joinByBullet(item.author?.name, item.songCountText)
                else -> null
            },
            badges = badges,
            thumbnailContent = {
                ItemThumbnail(
                    thumbnailUrl = item.thumbnail,
                    albumIndex = albumIndex,
                    isSelected = isSelected,
                    isActive = isActive,
                    isPlaying = isPlaying,
                    shape = if (item is ArtistItem) CircleShape else RoundedCornerShape(ThumbnailCornerRadius),
                    modifier = Modifier.size(ListThumbnailSize)
                )
            },
            trailingContent = trailingContent,
            modifier = modifier,
            isActive = isActive
        )
    }

    if (item is SongItem && isSwipeable && swipeEnabled) {
        SwipeToSongBox(
            mediaItem = item.toMediaItem(),
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    } else {
        content()
    }
}

@Composable
fun YouTubeGridItem(
    item: YTItem,
    modifier: Modifier = Modifier,
    coroutineScope: CoroutineScope? = null,
    badges: @Composable RowScope.() -> Unit = {
        val database = LocalDatabase.current
        val song by database.song(item.id).collectAsState(initial = null)
        val album by database.album(item.id).collectAsState(initial = null)

        if (item is SongItem && song?.song?.liked == true ||
            item is AlbumItem && album?.album?.bookmarkedAt != null
        ) {
            Icon.Favorite()
        }
        if (item.explicit) Icon.Explicit()
        if (item is SongItem && song?.song?.inLibrary != null) Icon.Library()
        if (item is SongItem) {
            val downloads by LocalDownloadUtil.current.downloads.collectAsState()
            Icon.Download(downloads[item.id]?.state)
        }
    },
    thumbnailRatio: Float = if (item is SongItem) 16f / 9 else 1f,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
) = GridItem(
    title = {
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (item is ArtistItem) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.basicMarquee().fillMaxWidth()
        )
    },
    subtitle = {
        val subtitle = when (item) {
            is SongItem -> joinByBullet(item.artists.joinToString { it.name }, makeTimeString(item.duration?.times(1000L)))
            is AlbumItem -> joinByBullet(item.artists?.joinToString { it.name }, item.year?.toString())
            is ArtistItem -> null
            is PlaylistItem -> joinByBullet(item.author?.name, item.songCountText)
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    },
    thumbnailContent = {
        val database = LocalDatabase.current
        val playerConnection = LocalPlayerConnection.current ?: return@GridItem
        val shape = if (item is ArtistItem) CircleShape else RoundedCornerShape(ThumbnailCornerRadius)

        ItemThumbnail(
            thumbnailUrl = item.thumbnail,
            isActive = isActive,
            isPlaying = isPlaying,
            shape = if (item is ArtistItem) CircleShape else RoundedCornerShape(ThumbnailCornerRadius),
        )

        if (item is SongItem && !isActive) {
            OverlayPlayButton(
                visible = true
            )
        }

        AlbumPlayButton(
            visible = item is AlbumItem && !isActive,
            onClick = {
                coroutineScope?.launch(Dispatchers.IO) {
                    var albumWithSongs = database.albumWithSongs(item.id).first()
                    if (albumWithSongs?.songs.isNullOrEmpty()) {
                        YouTube.album(item.id).onSuccess { albumPage ->
                            database.transaction { insert(albumPage) }
                            albumWithSongs = database.albumWithSongs(item.id).first()
                        }.onFailure { reportException(it) }
                    }
                    albumWithSongs?.let {
                        withContext(Dispatchers.Main) {
                            playerConnection.playQueue(LocalAlbumRadio(it))
                        }
                    }
                }
            }
        )
    },
    thumbnailRatio = thumbnailRatio,
    fillMaxWidth = fillMaxWidth,
    modifier = modifier
)

@Composable
fun LocalSongsGrid(
    title: String,
    subtitle: String,
    badges: @Composable RowScope.() -> Unit = {},
    thumbnailUrl: String?,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
    modifier: Modifier = Modifier
) = GridItem(
    title = title,
    subtitle = subtitle,
    badges = badges,
    thumbnailContent = {
        LocalThumbnail(
            thumbnailUrl = thumbnailUrl,
            isActive = isActive,
            isPlaying = isPlaying,
            shape = RoundedCornerShape(ThumbnailCornerRadius),
            modifier = if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier,
            showCenterPlay = true,
            playButtonVisible = false
        )
    },
    fillMaxWidth = fillMaxWidth,
    modifier = modifier
)

@Composable
fun LocalArtistsGrid(
    title: String,
    subtitle: String,
    badges: @Composable RowScope.() -> Unit = {},
    thumbnailUrl: String?,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
    modifier: Modifier = Modifier
) = GridItem(
    title = title,
    subtitle = subtitle,
    badges = badges,
    thumbnailContent = {
        LocalThumbnail(
            thumbnailUrl = thumbnailUrl,
            isActive = false,
            isPlaying = false,
            shape = CircleShape,
            modifier = if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier,
            showCenterPlay = false,
            playButtonVisible = false
        )
    },
    fillMaxWidth = fillMaxWidth,
    modifier = modifier
)

@Composable
fun LocalAlbumsGrid(
    title: String,
    subtitle: String,
    badges: @Composable RowScope.() -> Unit = {},
    thumbnailUrl: String?,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
    modifier: Modifier = Modifier
) = GridItem(
    title = title,
    subtitle = subtitle,
    badges = badges,
    thumbnailContent = {
        LocalThumbnail(
            thumbnailUrl = thumbnailUrl,
            isActive = isActive,
            isPlaying = isPlaying,
            shape = RoundedCornerShape(ThumbnailCornerRadius),
            modifier = if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier,
            showCenterPlay = false,
            playButtonVisible = true
        )
    },
    fillMaxWidth = fillMaxWidth,
    modifier = modifier
)

@Composable
fun ItemThumbnail(
    thumbnailUrl: String?,
    isActive: Boolean,
    isPlaying: Boolean,
    shape: Shape,
    modifier: Modifier = Modifier,
    albumIndex: Int? = null,
    isSelected: Boolean = false,
    thumbnailRatio: Float = 1f
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .aspectRatio(thumbnailRatio)
            .clip(shape)
    ) {
        if (albumIndex != null) {
            AnimatedVisibility(
                visible = !isActive,
                enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
                exit = shrinkOut(shrinkTowards = Alignment.Center) + fadeOut()
            ) {
                Text(
                    text = albumIndex.toString(),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        } else {
            if (isSelected) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(1f)
                        .clip(shape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        painter = painterResource(R.drawable.done),
                        contentDescription = null
                    )
                }
            }
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
            )
        }

        PlayingIndicatorBox(
            isActive = isActive,
            playWhenReady = isPlaying,
            color = if (albumIndex != null) MaterialTheme.colorScheme.onBackground else Color.White,
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = if (albumIndex != null)
                        Color.Transparent
                    else
                        Color.Black.copy(alpha = ActiveBoxAlpha),
                    shape = shape
                )
        )
    }
}

@Composable
fun LocalThumbnail(
    thumbnailUrl: String?,
    isActive: Boolean,
    isPlaying: Boolean,
    shape: Shape,
    modifier: Modifier = Modifier,
    showCenterPlay: Boolean = false,
    playButtonVisible: Boolean = false,
    thumbnailRatio: Float = 1f
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(thumbnailRatio)
            .clip(shape)
    ) {
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = isActive,
            enter = fadeIn(tween(500)),
            exit = fadeOut(tween(500))
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f), shape)
            ) {
                if (isPlaying) {
                    PlayingIndicator(
                        color = Color.White,
                        modifier = Modifier.height(24.dp)
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }

        if (showCenterPlay) {
            AnimatedVisibility(
                visible = !(isActive && isPlaying),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }

        if (playButtonVisible) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = ActiveBoxAlpha))
                ) {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistThumbnail(
    thumbnails: List<String>,
    size: Dp,
    placeHolder: @Composable () -> Unit,
    shape: Shape
) {
    when (thumbnails.size) {
        0 -> Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            placeHolder()
        }
        1 -> AsyncImage(
            model = thumbnails[0],
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(shape)
        )
        else -> Box(
            modifier = Modifier
                .size(size)
                .clip(shape)
        ) {
            listOf(
                Alignment.TopStart,
                Alignment.TopEnd,
                Alignment.BottomStart,
                Alignment.BottomEnd
            ).fastForEachIndexed { index, alignment ->
                AsyncImage(
                    model = thumbnails.getOrNull(index),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .align(alignment)
                        .size(size / 2)
                )
            }
        }
    }
}

@Composable
fun BoxScope.OverlayPlayButton(
    visible: Boolean
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier
            .align(Alignment.Center)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = ActiveBoxAlpha))
        ) {
            Icon(
                painter = painterResource(R.drawable.play),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun BoxScope.AlbumPlayButton(
    visible: Boolean,
    onClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = ActiveBoxAlpha))
                .clickable(onClick = onClick)
        ) {
            Icon(
                painter = painterResource(R.drawable.play),
                contentDescription = null,
                tint = Color.White
            )
        }
    }
}

@Composable
fun SwipeToSongBox(
    modifier: Modifier = Modifier,
    mediaItem: MediaItem,
    content: @Composable BoxScope.() -> Unit
) {
    val ctx = LocalContext.current
    val player = LocalPlayerConnection.current
    val scope = rememberCoroutineScope()
    val offset = remember { mutableStateOf(0f) }
    val threshold = 300f

    val dragState = rememberDraggableState { delta ->
        offset.value = (offset.value + delta).coerceIn(-threshold, threshold)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .draggable(
                orientation = Orientation.Horizontal,
                state = dragState,
                onDragStopped = {
                    when {
                        offset.value >= threshold -> {
                            player?.playNext(listOf(mediaItem))
                            Toast.makeText(ctx, R.string.play_next, Toast.LENGTH_SHORT).show()
                            reset(offset, scope)
                        }

                        offset.value <= -threshold -> {
                            player?.addToQueue(listOf(mediaItem))
                            Toast.makeText(ctx, R.string.add_to_queue, Toast.LENGTH_SHORT).show()
                            reset(offset, scope)
                        }

                        else -> reset(offset, scope)
                    }
                }
            )
    ) {
        if (offset.value != 0f) {
            val (iconRes, bg, tint, align) = if (offset.value > 0)
                Quadruple(
                    R.drawable.playlist_play,
                    MaterialTheme.colorScheme.secondary,
                    MaterialTheme.colorScheme.onSecondary,
                    Alignment.CenterStart
                ) else
                Quadruple(
                    R.drawable.queue_music,
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.onPrimary,
                    Alignment.CenterEnd
                )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.Center)
                    .background(bg),
                contentAlignment = align
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .size(30.dp)
                        .alpha(0.9f),
                    tint = tint
                )
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface),
            content = content
        )
    }
}

// Helper to animate reset of swipe offset
private fun reset(offset: MutableState<Float>, scope: CoroutineScope) {
    scope.launch {
        animate(
            initialValue = offset.value,
            targetValue = 0f,
            animationSpec = tween(durationMillis = 300)
        ) { value, _ -> offset.value = value }
    }
}

// Data holder for swipe visuals
data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

private object Icon {
    @Composable
    fun Favorite() {
        Icon(
            painter = painterResource(R.drawable.favorite),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .size(18.dp)
                .padding(end = 2.dp)
        )
    }

    @Composable
    fun Library() {
        Icon(
            painter = painterResource(R.drawable.library_add_check),
            contentDescription = null,
            modifier = Modifier
                .size(18.dp)
                .padding(end = 2.dp)
        )
    }

    @Composable
    fun Download(state: Int?) {
        when (state) {
            STATE_COMPLETED -> Icon(
                painter = painterResource(R.drawable.offline),
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp)
                    .padding(end = 2.dp)
            )
            STATE_QUEUED, STATE_DOWNLOADING -> CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier
                    .size(16.dp)
                    .padding(end = 2.dp)
            )
            else -> { /* no icon */ }
        }
    }

    @Composable
    fun Explicit() {
        Icon(
            painter = painterResource(R.drawable.explicit),
            contentDescription = null,
            modifier = Modifier
                .size(18.dp)
                .padding(end = 2.dp)
        )
    }
}
