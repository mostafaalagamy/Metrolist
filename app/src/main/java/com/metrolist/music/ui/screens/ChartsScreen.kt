package com.metrolist.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.metrolist.innertube.models.*
import com.metrolist.innertube.pages.ChartsPage
import com.metrolist.music.R
import com.metrolist.music.db.entities.Song
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.viewmodels.ChartsViewModel
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.ui.component.*
import com.metrolist.music.constants.ListItemHeight
import com.metrolist.music.constants.ThumbnailCornerRadius
import com.metrolist.music.extensions.togglePlayPause
import com.metrolist.music.ui.menu.YouTubeSongMenu
import com.metrolist.music.ui.menu.YouTubeAlbumMenu
import com.metrolist.music.ui.menu.YouTubeArtistMenu
import com.metrolist.music.LocalMenuState
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChartsScreen(
    navController: NavController,
    viewModel: ChartsViewModel = hiltViewModel()
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val chartsPage by viewModel.chartsPage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

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
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Surface(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isLoading) {
                FullScreenLoading()
            } else if (error != null) {
                FullScreenError(error = error!!, onRetry = { viewModel.loadCharts() })
            } else {
                chartsPage?.let { page ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        page.sections.forEach { section ->
                            item {
                                ChartSectionView(
                                    section = section,
                                    isPlaying = isPlaying,
                                    currentMediaId = mediaMetadata?.id,
                                    onSongClick = { song ->
                                        if (song.id == mediaMetadata?.id) {
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
                                    onItemLongClick = { item ->
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        when (item) {
                                            is SongItem -> menuState.show {
                                                YouTubeSongMenu(
                                                    song = item,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss
                                                )
                                            }
                                            is AlbumItem -> menuState.show {
                                                YouTubeAlbumMenu(
                                                    albumItem = item,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss
                                                )
                                            }
                                            is ArtistItem -> menuState.show {
                                                YouTubeArtistMenu(
                                                    artist = item,
                                                    onDismiss = menuState::dismiss
                                                )
                                            }
                                        }
                                    },
                                    navController = navController
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChartSectionView(
    section: ChartsPage.ChartSection,
    isPlaying: Boolean,
    currentMediaId: String?,
    onSongClick: (SongItem) -> Unit,
    onItemLongClick: (YTItem) -> Unit,
    navController: NavController
) {
    val lazyGridState = rememberLazyGridState()
    val horizontalLazyGridItemWidth = ListItemHeight * 3

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        NavigationTitle(
            title = section.title ?: stringResource(R.string.no_title),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        when (section.chartType) {
            ChartsPage.ChartType.TRENDING, ChartsPage.ChartType.TOP -> {
                LazyHorizontalGrid(
                    state = lazyGridState,
                    rows = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ListItemHeight * 4)
                ) {
                    itemsIndexed(
                        items = section.items,
                        key = { _, item -> item.id }
                    ) { index, item ->
                        when (item) {
                            is SongItem -> SongListItem(
                                song = Song(
                                    id = item.id,
                                    title = item.title ?: "",
                                    artists = item.artists.map { it.name },
                                    duration = item.duration?.toInt() ?: 0,
                                    thumbnailUrl = item.thumbnail?.url ?: ""
                                ),
                                isActive = item.id == currentMediaId,
                                isPlaying = isPlaying,
                                showInLibraryIcon = false,
                                modifier = Modifier
                                    .width(horizontalLazyGridItemWidth)
                                    .combinedClickable(
                                        onClick = { onSongClick(item) },
                                        onLongClick = { onItemLongClick(item) }
                                    )
                            )
                            else -> StandardItem(item = item)
                        }
                    }
                }
            }
            else -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    itemsIndexed(
                        items = section.items,
                        key = { _, item -> item.id }
                    ) { _, item ->
                        when (item) {
                            is AlbumItem -> AlbumItemView(
                                album = item,
                                isActive = item.id == currentMediaId,
                                isPlaying = isPlaying,
                                onClick = { navController.navigate("album/${item.id}") },
                                onLongClick = { onItemLongClick(item) }
                            )
                            is ArtistItem -> ArtistItemView(
                                artist = item,
                                onClick = { navController.navigate("artist/${item.id}") },
                                onLongClick = { onItemLongClick(item) }
                            )
                            else -> StandardItem(item = item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumItemView(
    album: AlbumItem,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .width(160.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            AsyncImage(
                model = album.thumbnail?.url,
                contentDescription = stringResource(R.string.album_cover_desc),
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(ThumbnailCornerRadius))
            )
            if (isActive) {
                Icon(
                    painter = painterResource(
                        if (isPlaying) R.drawable.pause else R.drawable.play
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center),
                    tint = Color.White
                )
            }
        }
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = album.title ?: stringResource(R.string.no_title),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1
            )
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
fun ArtistItemView(
    artist: ArtistItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = artist.thumbnail?.url,
            contentDescription = stringResource(R.string.artist_cover_desc),
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
        )
        Text(
            text = artist.name ?: stringResource(R.string.unknown_artist),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun StandardItem(item: YTItem) {
    Text(
        text = when (item) {
            is SongItem -> item.title ?: stringResource(R.string.no_title)
            is AlbumItem -> item.title ?: stringResource(R.string.no_title)
            is ArtistItem -> item.name ?: stringResource(R.string.unknown_artist)
            else -> stringResource(R.string.unknown_item_type)
        },
        style = MaterialTheme.typography.bodyLarge
    )
}

@Composable
fun FullScreenLoading() {
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
fun FullScreenError(
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
