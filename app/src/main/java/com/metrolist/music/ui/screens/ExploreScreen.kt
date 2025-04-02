package com.metrolist.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.NavigationTitle
import com.metrolist.music.ui.component.YouTubeGridItem
import com.metrolist.music.ui.component.YouTubeListItem
import com.metrolist.music.ui.component.shimmer.GridItemPlaceHolder
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.ui.component.shimmer.TextPlaceholder
import com.metrolist.music.ui.menu.YouTubeAlbumMenu
import com.metrolist.music.ui.menu.YouTubeSongMenu
import com.metrolist.music.ui.utils.SnapLayoutInfoProvider
import com.metrolist.music.viewmodels.ChartsViewModel
import com.metrolist.music.viewmodels.ExploreViewModel
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import com.metrolist.music.constants.ListItemHeight
import com.metrolist.music.extensions.togglePlayPause
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.YouTubeQueue

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExploreScreen(
    navController: NavController,
    exploreViewModel: ExploreViewModel = hiltViewModel(),
    chartsViewModel: ChartsViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val explorePage by exploreViewModel.explorePage.collectAsState()
    val chartsPage by chartsViewModel.chartsPage.collectAsState()
    val isLoading by chartsViewModel.isLoading.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier.verticalScroll(scrollState),
        ) {
            Spacer(
                Modifier.height(
                    LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateTopPadding(),
                ),
            )

            chartsPage?.sections?.forEach { section ->
                NavigationTitle(
                    title = section.title ?: stringResource(R.string.charts),
                    modifier = Modifier.animateItem(),
                )

                val horizontalLazyGridItemWidthFactor = 0.475f
                val horizontalLazyGridItemWidth = with(LocalDensity.current) {
                    (LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateLeftPadding() +
                            LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateRightPadding() +
                            (LocalDensity.current.density * 320)).dp * horizontalLazyGridItemWidthFactor
                }

                val lazyGridState = rememberLazyGridState()
                val snapLayoutInfoProvider = remember(lazyGridState) {
                    SnapLayoutInfoProvider(
                        lazyGridState = lazyGridState,
                        positionInLayout = { layoutSize, itemSize ->
                            (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
                        },
                    )
                }

                LazyHorizontalGrid(
                    state = lazyGridState,
                    rows = GridCells.Fixed(4),
                    flingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider),
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
                            isSwipeable = false, // تعطيل السحب هنا
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

            explorePage?.newReleaseAlbums?.let { newReleaseAlbums ->
                NavigationTitle(
                    title = stringResource(R.string.new_release_albums),
                    onClick = {
                        navController.navigate("new_release")
                    },
                )

                LazyRow(
                    contentPadding =
                    WindowInsets.systemBars
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
                            coroutineScope = coroutineScope,
                            modifier =
                            Modifier
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
                                .animateItem(),
                        )
                    }
                }
            }

            explorePage?.moodAndGenres?.let { moodAndGenres ->
                NavigationTitle(
                    title = stringResource(R.string.mood_and_genres),
                    onClick = {
                        navController.navigate("mood_and_genres")
                    },
                )

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
                            modifier =
                            Modifier
                                .padding(6.dp)
                                .width(180.dp),
                        )
                    }
                }
                Spacer(
                    Modifier.height(
                        LocalPlayerAwareWindowInsets.current.asPaddingValues()
                            .calculateBottomPadding()
                    )
                )
            }

            if (explorePage == null) {
                ShimmerHost {
                    TextPlaceholder(
                        height = 36.dp,
                        modifier =
                        Modifier
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
                        modifier =
                        Modifier
                            .padding(vertical = 12.dp, horizontal = 12.dp)
                            .width(250.dp),
                    )
                    repeat(4) {
                        Row {
                            repeat(2) {
                                TextPlaceholder(
                                    height = MoodAndGenresButtonHeight,
                                    modifier =
                                    Modifier
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
