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
import androidx.hilt.navigation.compose.hiltViewModel
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.YTItem
import com.metrolist.innertube.pages.ChartsPage
import com.metrolist.music.R
import com.metrolist.music.viewmodels.ChartsViewModel

@Composable
fun ChartsScreen(viewModel: ChartsViewModel = hiltViewModel()) {
    val chartsPage by viewModel.chartsPage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(Unit) {
        if (chartsPage == null) {
            viewModel.loadCharts()
        }
    }

    if (isLoading) {
        FullScreenLoading()
    } else if (error != null) {
        FullScreenError(error!!) { viewModel.loadCharts() }
    } else {
        chartsPage?.let { page ->
            LazyColumn {
                page.sections.forEach { section ->
                    item {
                        ChartSectionView(section)
                    }
                }
            }
        }
    }
}

@Composable
fun ChartSectionView(section: ChartsPage.ChartSection) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = section.title ?: "No Title",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        when (section.chartType) {
            ChartsPage.ChartType.TRENDING, ChartsPage.ChartType.TOP -> {
                LazyColumn {
                    items(section.items) { item ->
                        when (item) {
                            is SongItem -> ChartSongItem(item)
                            else -> StandardItem(item)
                        }
                    }
                }
            }
            else -> {
                LazyRow {
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
    Column(modifier = Modifier.padding(8.dp)) {
        Text(text = album.title ?: "No title")
    }
}

@Composable
fun ChartSongItem(song: SongItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
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
                text = song.title,
                style = MaterialTheme.typography.bodyLarge)
            Text(
                text = song.artists.joinToString { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
        
        song.duration?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun StandardItem(item: YTItem) {
    when (item) {
        is SongItem -> Text(text = item.title)
        is AlbumItem -> Text(text = item.title)
        else -> Text(text = "Unknown item type")
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
fun FullScreenError(error: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = error)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}
