package com.metrolist.music.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.hilt.navigation.compose.hiltViewModel
import com.metrolist.innertube.models.*
import com.metrolist.innertube.pages.ChartsPage
import com.metrolist.music.R
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.viewmodels.ChartsViewModel
import com.metrolist.music.LocalPlayerConnection
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.text.AnnotatedString

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChartsScreen(viewModel: ChartsViewModel = hiltViewModel()) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val chartsPage by viewModel.chartsPage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    LaunchedEffect(Unit) {
        if (chartsPage == null) {
            viewModel.loadCharts()
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            FullScreenLoading()
        } else if (error != null) {
            FullScreenError(AnnotatedString(error!!)) { viewModel.loadCharts() }
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
                                        playerConnection.player.playWhenReady = !playerConnection.player.playWhenReady
                                    } else {
                                        playerConnection.playQueue(
                                            YouTubeQueue(
                                                song.endpoint ?: WatchEndpoint(videoId = song.id),
                                                song.toMediaItem()
                                            )
                                        )
                                    }
                                }
                            )
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
    onSongClick: (SongItem) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = section.title ?: "No Title",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        when (section.chartType) {
            ChartsPage.ChartType.TRENDING, ChartsPage.ChartType.TOP -> {
                Column {
                    section.items.forEach { item ->
                        when (item) {
                            is SongItem -> ChartSongItem(
                                song = item,
                                isActive = item.id == currentMediaId,
                                isPlaying = isPlaying,
                                onClick = { onSongClick(item) },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            )
                            else -> StandardItem(item)
                        }
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
            else -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(section.items) { item ->
                        when (item) {
                            is AlbumItem -> ChartAlbumItem(item)
                            else -> StandardItem(item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChartAlbumItem(album: AlbumItem) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .padding(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = album.title ?: "No title",
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = album.artists?.joinToString { it.name } ?: "Unknown artist",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1
            )
        }
    }
}

@Composable
fun ChartSongItem(
    song: SongItem,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = song.chartPosition?.toString() ?: "-",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.width(32.dp)
            )
            
            song.chartChange?.let { change ->
                Icon(
                    painter = painterResource(
                        id = when (change) {
                            "up" -> R.drawable.arrow_upward
                            "down" -> R.drawable.arrow_downward
                            else -> R.drawable.album
                        }
                    ),
                    contentDescription = null,
                    tint = when (change) {
                        "up" -> Color.Green
                        "down" -> Color.Red
                        else -> Color.Blue
                    },
                    modifier = Modifier.size(24.dp)
                )
            } ?: Spacer(modifier = Modifier.width(24.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title ?: "Unknown title",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = song.artists.joinToString { it.name },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            song.duration?.let { duration ->
                Text(
                    text = duration,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (isActive) {
                Icon(
                    painter = painterResource(
                        if (isPlaying) R.drawable.pause else R.drawable.play
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun StandardItem(item: YTItem) {
    Card(
        modifier = Modifier.padding(8.dp)
    ) {
        Text(
            text = when (item) {
                is SongItem -> item.title ?: "Unknown song"
                is AlbumItem -> item.title ?: "Unknown album"
                else -> "Unknown item type"
            },
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
fun FullScreenLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun FullScreenError(error: AnnotatedString, onRetry: () -> Unit) {
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
                Text(text = AnnotatedString("Retry"))
            }
        }
    }
}
