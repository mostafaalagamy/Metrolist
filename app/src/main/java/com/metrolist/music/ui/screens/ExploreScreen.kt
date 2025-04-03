package com.metrolist.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.metrolist.innertube.models.*
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.ListItemHeight
import com.metrolist.music.extensions.togglePlayPause
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.*
import com.metrolist.music.ui.component.shimmer.*
import com.metrolist.music.ui.menu.YouTubeAlbumMenu
import com.metrolist.music.ui.menu.YouTubeSongMenu
import com.metrolist.music.viewmodels.ChartsViewModel
import com.metrolist.music.viewmodels.ExploreViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExploreScreen(
    navController: NavController,
    exploreViewModel: ExploreViewModel = hiltViewModel(),
    chartsViewModel: ChartsViewModel = hiltViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val explorePage by exploreViewModel.explorePage.collectAsState()
    val chartsPage by chartsViewModel.chartsPage.collectAsState()
    val isChartsLoading by chartsViewModel.isLoading.collectAsState()
    val chartsError by chartsViewModel.error.collectAsState()

    val lazyListState = rememberLazyListState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop by backStackEntry?.savedStateHandle
        ?.getStateFlow("scrollToTop", false)?.collectAsState() ?: mutableStateOf(false)

    LaunchedEffect(Unit) {
        if (chartsPage == null) {
            chartsViewModel.loadCharts()
        }
    }

    LaunchedEffect(scrollToTop) {
        if (scrollToTop) {
            lazyListState.animateScrollToItem(0)
            scrollBehavior.state.heightOffset = 0f // Reset scroll behavior
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        state = lazyListState,
    ) {
        item {
            Spacer(
                Modifier.height(
                    LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateTopPadding()
                )
            )
        }

        // Charts Loading State
        if (isChartsLoading && chartsPage == null) {
            item {
                ShimmerHost {
                    TextPlaceholder(
                        height = 36.dp,
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(0.5f),
                    )

                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val horizontalLazyGridItemWidthFactor = if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
                        val horizontalLazyGridItemWidth = maxWidth * horizontalLazyGridItemWidthFactor

                        LazyHorizontalGrid(
                            rows = GridCells.Fixed(4),
                            contentPadding = WindowInsets.systemBars
                                .only(WindowInsetsSides.Horizontal)
                                .asPaddingValues(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ListItemHeight * 4),
                        ) {
                            items(4) {
                                Row(
                                    modifier = Modifier
                                        .width(horizontalLazyGridItemWidth)
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(ListItemHeight - 16.dp)
                                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(
                                        modifier = Modifier.fillMaxHeight(),
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .height(16.dp)
                                                .width(120.dp)
                                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)),
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .height(12.dp)
                                                .width(80.dp)
                                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Charts Content
        chartsPage?.sections?.forEach { section ->
            item {
                NavigationTitle(
                    title = section.title ?: stringResource(R.string.charts),
                )
            }
            
            item {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val horizontalLazyGridItemWidthFactor = if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
                    val horizontalLazyGridItemWidth = maxWidth * horizontalLazyGridItemWidthFactor

                    LazyHorizontalGrid(
                        rows = GridCells.Fixed(4),
                        contentPadding = WindowInsets.systemBars
                            .only(WindowInsetsSides.Horizontal)
                            .asPaddingValues(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ListItemHeight * 4),
                    ) {
                        items(
                            items = section.items.filterIsInstance<SongItem>(),
                            key = { it.id },
                        ) { song ->
                            YouTubeListItem(
                                item = song,
                                isActive = song.id == mediaMetadata?.id,
                                isPlaying = isPlaying,
                                isSwipeable = false,
                                trailingContent = {
                                    IconButton(
                                        onClick = {
                                            menuState.show {
                                                YouTubeSongMenu(
                                                    song = song,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.more_vert),
                                            contentDescription = null,
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .width(horizontalLazyGridItemWidth)
                                    .combinedClickable(
                                        onClick = {
                                            if (song.id == mediaMetadata?.id) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(
                                                    YouTubeQueue(
                                                        endpoint = WatchEndpoint(videoId = song.id),
                                                        preloadItem = song.toMediaMetadata(),
                                                    ),
                                                )
                                            }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                YouTubeSongMenu(
                                                    song = song,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                    ),
                            )
                        }
                    }
                }
            }
        }

        // Charts Error State
        if (chartsError != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = chartsError ?: stringResource(R.string.error_unknown),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        // New Release Albums
        explorePage?.newReleaseAlbums?.let { newReleaseAlbums ->
            item {
                NavigationTitle(
                    title = stringResource(R.string.new_release_albums),
                    onClick = {
                        navController.navigate("new_release")
                    },
                )
            }
            
            item {
                LazyRow(
                    contentPadding = WindowInsets.systemBars
                        .only(WindowInsetsSides.Horizontal)
                        .asPaddingValues(),
                ) {
                    items(
                        items = newReleaseAlbums,
                        key = { it.id },
                    ) { album ->
                        YouTubeGridItem(
                            item = album,
                            isActive = mediaMetadata?.album?.id == album.id,
                            isPlaying = isPlaying,
                            modifier = Modifier
                                .combinedClickable(
                                    onClick = {
                                        navController.navigate("album/${album.id}")
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        menuState.show {
                                            YouTubeAlbumMenu(
                                                albumItem = album,
                                                navController = navController,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    },
                                )
                        )
                    }
                }
            }
        }

        // Mood and Genres
        explorePage?.moodAndGenres?.let { moodAndGenres ->
            item {
                NavigationTitle(
                    title = stringResource(R.string.mood_and_genres),
                    onClick = {
                        navController.navigate("mood_and_genres")
                    },
                )
            }
            
            item {
                LazyHorizontalGrid(
                    rows = GridCells.Fixed(4),
                    contentPadding = PaddingValues(6.dp),
                    modifier = Modifier.height((MoodAndGenresButtonHeight + 12.dp) * 4 + 12.dp),
                ) {
                    items(moodAndGenres) {
                        MoodAndGenresButton(
                            title = it.title,
                            onClick = {
                                navController.navigate("youtube_browse/${it.endpoint.browseId}?params=${it.endpoint.params}")
                            },
                            modifier = Modifier
                                .padding(6.dp)
                                .width(180.dp),
                        )
                    }
                }
            }
        }

        // Bottom spacer for system bars
        item {
            Spacer(
                Modifier.height(
                    LocalPlayerAwareWindowInsets.current.asPaddingValues()
                        .calculateBottomPadding()
                )
            )
        }

        // Explore Loading State
        if (explorePage == null) {
            item {
                ShimmerHost {
                    TextPlaceholder(
                        height = 36.dp,
                        modifier = Modifier
                            .padding(vertical = 12.dp, horizontal = 12.dp)
                            .width(250.dp),
                    )
                    Row {
                        repeat(2) {
                            GridItemPlaceHolder()
                        }
                    }
                    TextPlaceholder(
                        height = 36.dp,
                        modifier = Modifier
                            .padding(vertical = 12.dp, horizontal = 12.dp)
                            .width(250.dp),
                    )
                    repeat(4) {
                        Row {
                            repeat(2) {
                                TextPlaceholder(
                                    height = MoodAndGenresButtonHeight,
                                    modifier = Modifier
                                        .padding(horizontal = 6.dp)
                                        .width(200.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
