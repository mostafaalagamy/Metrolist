package com.metrolist.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.metrolist.innertube.models.*
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.*
import com.metrolist.music.ui.menu.*
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.utils.backToMain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChartsScreen(
    navController: NavController,
    viewModel: ChartsViewModel = hiltViewModel()
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.charts)) },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                    .asPaddingValues()
            ) {
                if (isLoading) {
                    item {
                        ShimmerHost(modifier = Modifier.fillMaxWidth()) {
                            repeat(3) {
                                TextPlaceholder(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                        .fillMaxWidth(0.6f)
                                        .height(24.dp)
                                )
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    items(5) {
                                        GridItemPlaceHolder()
                                    }
                                }
                            }
                        }
                    }
                } else if (error != null) {
                    item {
                        FullScreenError(
                            error = AnnotatedString(error!!),
                            onRetry = { viewModel.loadCharts() }
                        )
                    }
                } else {
                    chartsPage?.sections?.forEach { section ->
                        item {
                            NavigationTitle(
                                title = section.title ?: stringResource(R.string.charts),
                                modifier = Modifier.animateItem()
                            )
                        }

                        item {
                            when (section.chartType) {
                                ChartsPage.ChartType.TRENDING, 
                                ChartsPage.ChartType.TOP -> {
                                    VerticalChartSection(
                                        items = section.items,
                                        isPlaying = isPlaying,
                                        mediaMetadata = mediaMetadata,
                                        navController = navController,
                                        playerConnection = playerConnection,
                                        menuState = menuState,
                                        haptic = haptic,
                                        coroutineScope = coroutineScope
                                    )
                                }
                                else -> {
                                    HorizontalChartSection(
                                        items = section.items,
                                        isPlaying = isPlaying,
                                        mediaMetadata = mediaMetadata,
                                        navController = navController,
                                        menuState = menuState,
                                        haptic = haptic,
                                        coroutineScope = coroutineScope
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VerticalChartSection(
    items: List<YTItem>,
    isPlaying: Boolean,
    mediaMetadata: MediaMetadata?,
    navController: NavController,
    playerConnection: PlayerConnection,
    menuState: MenuState,
    haptic: HapticFeedback,
    coroutineScope: CoroutineScope
) {
    LazyColumn(modifier = Modifier.animateItem()) {
        items(items) { item ->
            when (item) {
                is SongItem -> SongListItem(
                    song = item.toMediaMetadata(),
                    isActive = item.id == mediaMetadata?.id,
                    isPlaying = isPlaying,
                    showInLibraryIcon = false,
                    trailingContent = {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                    YouTubeSongMenu(
                                        song = item,
                                        navController = navController,
                                        onDismiss = menuState::dismiss
                                    )
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                if (item.id == mediaMetadata?.id) {
                                    playerConnection.player.togglePlayPause()
                                } else {
                                    playerConnection.playQueue(
                                        YouTubeQueue(
                                            item.endpoint ?: WatchEndpoint(videoId = item.id),
                                            item.toMediaMetadata()
                                        )
                                    )
                                }
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                    YouTubeSongMenu(
                                        song = item,
                                        navController = navController,
                                        onDismiss = menuState::dismiss
                                    )
                                }
                            }
                        )
                )
                is AlbumItem -> YouTubeListItem(
                    item = item,
                    isActive = item.id == mediaMetadata?.album?.id,
                    isPlaying = isPlaying,
                    trailingContent = {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                    YouTubeAlbumMenu(
                                        albumItem = item,
                                        navController = navController,
                                        onDismiss = menuState::dismiss
                                    )
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                navController.navigate("album/${item.id}")
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                    YouTubeAlbumMenu(
                                        albumItem = item,
                                        navController = navController,
                                        onDismiss = menuState::dismiss
                                    )
                                }
                            }
                        )
                )
                is PlaylistItem -> YouTubeListItem(
                    item = item,
                    isActive = false,
                    isPlaying = false,
                    trailingContent = {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                    YouTubePlaylistMenu(
                                        playlist = item,
                                        coroutineScope = coroutineScope,
                                        onDismiss = menuState::dismiss
                                    )
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                navController.navigate("online_playlist/${item.id}")
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                    YouTubePlaylistMenu(
                                        playlist = item,
                                        coroutineScope = coroutineScope,
                                        onDismiss = menuState::dismiss
                                    )
                                }
                            }
                        )
                )
                is ArtistItem -> YouTubeListItem(
                    item = item,
                    isActive = false,
                    isPlaying = false,
                    trailingContent = {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                    YouTubeArtistMenu(
                                        artist = item,
                                        onDismiss = menuState::dismiss
                                    )
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                navController.navigate("artist/${item.id}")
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                    YouTubeArtistMenu(
                                        artist = item,
                                        onDismiss = menuState::dismiss
                                    )
                                }
                            }
                        )
                )
            }
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

@Composable
private fun HorizontalChartSection(
    items: List<YTItem>,
    isPlaying: Boolean,
    mediaMetadata: MediaMetadata?,
    navController: NavController,
    menuState: MenuState,
    haptic: HapticFeedback,
    coroutineScope: CoroutineScope
) {
    LazyRow(
        modifier = Modifier.animateItem(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(items) { item ->
            when (item) {
                is AlbumItem -> YouTubeGridItem(
                    item = item,
                    isActive = item.id == mediaMetadata?.album?.id,
                    isPlaying = isPlaying,
                    thumbnailRatio = 1f,
                    modifier = Modifier
                        .combinedClickable(
                            onClick = {
                                navController.navigate("album/${item.id}")
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                    YouTubeAlbumMenu(
                                        albumItem = item,
                                        navController = navController,
                                        onDismiss = menuState::dismiss
                                    )
                                }
                            }
                        )
                )
                is SongItem -> YouTubeGridItem(
                    item = item,
                    isActive = item.id == mediaMetadata?.id,
                    isPlaying = isPlaying,
                    thumbnailRatio = 1f,
                    modifier = Modifier
                        .combinedClickable(
                            onClick = {
                                if (item.id == mediaMetadata?.id) {
                                    playerConnection.player.togglePlayPause()
                                } else {
                                    playerConnection.playQueue(
                                        YouTubeQueue(
                                            item.endpoint ?: WatchEndpoint(videoId = item.id),
                                            item.toMediaMetadata()
                                        )
                                    )
                                }
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                    YouTubeSongMenu(
                                        song = item,
                                        navController = navController,
                                        onDismiss = menuState::dismiss
                                    )
                                }
                            }
                        )
                )
                is PlaylistItem -> YouTubeGridItem(
                    item = item,
                    isActive = false,
                    isPlaying = false,
                    thumbnailRatio = 1f,
                    modifier = Modifier
                        .combinedClickable(
                            onClick = {
                                navController.navigate("online_playlist/${item.id}")
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                    YouTubePlaylistMenu(
                                        playlist = item,
                                        coroutineScope = coroutineScope,
                                        onDismiss = menuState::dismiss
                                    )
                                }
                            }
                        )
                )
                is ArtistItem -> YouTubeGridItem(
                    item = item,
                    isActive = false,
                    isPlaying = false,
                    thumbnailRatio = 1f,
                    modifier = Modifier
                        .combinedClickable(
                            onClick = {
                                navController.navigate("artist/${item.id}")
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                    YouTubeArtistMenu(
        
