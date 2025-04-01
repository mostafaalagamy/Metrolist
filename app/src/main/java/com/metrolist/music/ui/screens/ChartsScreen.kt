package com.metrolist.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.metrolist.innertube.models.*
import com.metrolist.innertube.pages.ChartsPage
import com.metrolist.music.R
import com.metrolist.music.db.entities.Song
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.viewmodels.ChartsViewModel
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.ui.component.NavigationTitle
import com.metrolist.music.ui.component.SongListItem
import com.metrolist.music.constants.ListItemHeight
import com.metrolist.music.constants.ThumbnailCornerRadius
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.metrolist.music.extensions.togglePlayPause

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChartsScreen(
    navController: NavController,
    viewModel: ChartsViewModel = hiltViewModel()
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val chartsPage by viewModel.chartsPage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        if (chartsPage == null) {
            viewModel.loadCharts()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.charts_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = stringResource(R.string.back_button_desc)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Surface(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                isLoading -> FullScreenLoading()
                error != null -> FullScreenError(error = error!!, onRetry = { viewModel.loadCharts() })
                else -> chartsPage?.let { page ->
                    ChartsContent(
                        page = page,
                        isPlaying = isPlaying,
                        currentMediaId = mediaMetadata?.id,
                        playerConnection = playerConnection,
                        haptic = haptic
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartsContent(
    page: ChartsPage,
    isPlaying: Boolean,
    currentMediaId: String?,
    playerConnection: PlayerConnection,
    haptic: HapticFeedback
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        page.sections.forEach { section ->
            item {
                ChartSection(
                    section = section,
                    isPlaying = isPlaying,
                    currentMediaId = currentMediaId,
                    onSongClick = { song ->
                        if (song.id == currentMediaId) {
                            playerConnection.player.togglePlayPause()
                        } else {
                            playerConnection.playQueue(
                                YouTubeQueue(
                                    song.endpoint ?: WatchEndpoint(videoId = song.id),
                                    song.toMediaMetadata()
                                )
                            )
                        }
                    },
                    haptic = haptic
                )
            }
        }
    }
}

@Composable
private fun ChartSection(
    section: ChartsPage.ChartSection,
    isPlaying: Boolean,
    currentMediaId: String?,
    onSongClick: (SongItem) -> Unit,
    haptic: HapticFeedback
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        NavigationTitle(
            title = section.title ?: stringResource(R.string.no_title),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        when (section.chartType) {
            ChartsPage.ChartType.TRENDING, ChartsPage.ChartType.TOP -> 
                SongsGrid(
                    songs = section.items.filterIsInstance<SongItem>(),
                    isPlaying = isPlaying,
                    currentMediaId = currentMediaId,
                    onSongClick = onSongClick,
                    haptic = haptic
                )
            else -> 
                AlbumsRow(
                    items = section.items,
                    haptic = haptic
                )
        }
    }
}

@Composable
private fun SongsGrid(
    songs: List<SongItem>,
    isPlaying: Boolean,
    currentMediaId: String?,
    onSongClick: (SongItem) -> Unit,
    haptic: HapticFeedback
) {
    val gridState = rememberLazyGridState()
    
    LazyHorizontalGrid(
        state = gridState,
        rows = GridCells.Fixed(4),
        modifier = Modifier
            .fillMaxWidth()
            .height(ListItemHeight * 4)
    ) {
        items(songs) { song ->
            SongListItem(
                song = song.toSong(),
                isActive = song.id == currentMediaId,
                isPlaying = isPlaying,
                showInLibraryIcon = false,
                modifier = Modifier
                    .width(ListItemHeight * 3)
                    .combinedClickable(
                        onClick = { onSongClick(song) },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
            )
        }
    }
}

@Composable
private fun AlbumsRow(
    items: List<YTItem>,
    haptic: HapticFeedback
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(items) { item ->
            when (item) {
                is AlbumItem -> AlbumCard(album = item)
                else -> DefaultItem(item = item)
            }
        }
    }
}

@Composable
private fun AlbumCard(album: AlbumItem) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .padding(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = album.thumbnail?.url,
                contentDescription = stringResource(R.string.album_cover_desc),
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(ThumbnailCornerRadius))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = album.title ?: stringResource(R.string.no_title),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = album.artists?.joinToString { it.name } ?: stringResource(R.string.unknown_artist),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DefaultItem(item: YTItem) {
    Text(
        text = when (item) {
            is SongItem -> item.title ?: stringResource(R.string.no_title)
            is AlbumItem -> item.title ?: stringResource(R.string.no_title)
            else -> stringResource(R.string.unknown_item_type)
        },
        style = MaterialTheme.typography.bodyLarge
    )
}

private fun SongItem.toSong(): Song {
    return Song(
        id = this.id,
        title = this.title ?: "",
        artists = this.artists.map { it.name },
        duration = this.duration?.toInt() ?: 0,
        thumbnailUrl = this.thumbnail?.url ?: ""
    )
}

@Composable
private fun FullScreenLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.loading_charts))
        }
    }
}

@Composable
private fun FullScreenError(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text(stringResource(R.string.retry_button))
            }
        }
    }
}
