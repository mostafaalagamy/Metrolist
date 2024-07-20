package com.moxxaxx.music.ui.screens.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.moxxaxx.innertube.YouTube.SearchFilter.Companion.FILTER_ALBUM
import com.moxxaxx.innertube.YouTube.SearchFilter.Companion.FILTER_ARTIST
import com.moxxaxx.innertube.YouTube.SearchFilter.Companion.FILTER_COMMUNITY_PLAYLIST
import com.moxxaxx.innertube.YouTube.SearchFilter.Companion.FILTER_FEATURED_PLAYLIST
import com.moxxaxx.innertube.YouTube.SearchFilter.Companion.FILTER_SONG
import com.moxxaxx.innertube.YouTube.SearchFilter.Companion.FILTER_VIDEO
import com.moxxaxx.innertube.models.AlbumItem
import com.moxxaxx.innertube.models.ArtistItem
import com.moxxaxx.innertube.models.PlaylistItem
import com.moxxaxx.innertube.models.SongItem
import com.moxxaxx.innertube.models.WatchEndpoint
import com.moxxaxx.innertube.models.YTItem
import com.moxxaxx.music.LocalPlayerAwareWindowInsets
import com.moxxaxx.music.LocalPlayerConnection
import com.moxxaxx.music.R
import com.moxxaxx.music.constants.AppBarHeight
import com.moxxaxx.music.constants.SearchFilterHeight
import com.moxxaxx.music.extensions.togglePlayPause
import com.moxxaxx.music.models.toMediaMetadata
import com.moxxaxx.music.playback.queues.YouTubeQueue
import com.moxxaxx.music.ui.component.ChipsRow
import com.moxxaxx.music.ui.component.EmptyPlaceholder
import com.moxxaxx.music.ui.component.LocalMenuState
import com.moxxaxx.music.ui.component.NavigationTitle
import com.moxxaxx.music.ui.component.YouTubeListItem
import com.moxxaxx.music.ui.component.shimmer.ListItemPlaceHolder
import com.moxxaxx.music.ui.component.shimmer.ShimmerHost
import com.moxxaxx.music.ui.menu.YouTubeAlbumMenu
import com.moxxaxx.music.ui.menu.YouTubeArtistMenu
import com.moxxaxx.music.ui.menu.YouTubePlaylistMenu
import com.moxxaxx.music.ui.menu.YouTubeSongMenu
import com.moxxaxx.music.viewmodels.OnlineSearchViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnlineSearchResult(
    navController: NavController,
    viewModel: OnlineSearchViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    val searchFilter by viewModel.filter.collectAsState()
    val searchSummary = viewModel.summaryPage
    val itemsPage by remember(searchFilter) {
        derivedStateOf {
            searchFilter?.value?.let {
                viewModel.viewStateMap[it]
            }
        }
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow {
            lazyListState.layoutInfo.visibleItemsInfo.any { it.key == "loading" }
        }.collect { shouldLoadMore ->
            if (!shouldLoadMore) return@collect
            viewModel.loadMore()
        }
    }

    val ytItemContent: @Composable LazyItemScope.(YTItem) -> Unit = { item: YTItem ->
        val longClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            menuState.show {
                when (item) {
                    is SongItem ->
                        YouTubeSongMenu(
                            song = item,
                            navController = navController,
                            onDismiss = menuState::dismiss,
                        )

                    is AlbumItem ->
                        YouTubeAlbumMenu(
                            albumItem = item,
                            navController = navController,
                            onDismiss = menuState::dismiss,
                        )

                    is ArtistItem ->
                        YouTubeArtistMenu(
                            artist = item,
                            onDismiss = menuState::dismiss,
                        )

                    is PlaylistItem ->
                        YouTubePlaylistMenu(
                            playlist = item,
                            coroutineScope = coroutineScope,
                            onDismiss = menuState::dismiss,
                        )
                }
            }
        }
        YouTubeListItem(
            item = item,
            isActive =
                when (item) {
                    is SongItem -> mediaMetadata?.id == item.id
                    is AlbumItem -> mediaMetadata?.album?.id == item.id
                    else -> false
                },
            isPlaying = isPlaying,
            trailingContent = {
                IconButton(
                    onClick = longClick,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_vert),
                        contentDescription = null,
                    )
                }
            },
            modifier =
                Modifier
                    .combinedClickable(
                        onClick = {
                            when (item) {
                                is SongItem -> {
                                    if (item.id == mediaMetadata?.id) {
                                        playerConnection.player.togglePlayPause()
                                    } else {
                                        playerConnection.playQueue(YouTubeQueue(WatchEndpoint(videoId = item.id), item.toMediaMetadata()))
                                    }
                                }

                                is AlbumItem -> navController.navigate("album/${item.id}")
                                is ArtistItem -> navController.navigate("artist/${item.id}")
                                is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                            }
                        },
                        onLongClick = longClick,
                    ).animateItemPlacement(),
        )
    }

    LazyColumn(
        state = lazyListState,
        contentPadding =
            LocalPlayerAwareWindowInsets.current
                .add(WindowInsets(top = SearchFilterHeight))
                .asPaddingValues(),
    ) {
        if (searchFilter == null) {
            searchSummary?.summaries?.forEach { summary ->
                item {
                    NavigationTitle(summary.title)
                }

                items(
                    items = summary.items,
                    key = { "${summary.title}/${it.id}" },
                    itemContent = ytItemContent,
                )
            }

            if (searchSummary?.summaries?.isEmpty() == true) {
                item {
                    EmptyPlaceholder(
                        icon = R.drawable.search,
                        text = stringResource(R.string.no_results_found),
                    )
                }
            }
        } else {
            items(
                items = itemsPage?.items.orEmpty(),
                key = { it.id },
                itemContent = ytItemContent,
            )

            if (itemsPage?.continuation != null) {
                item(key = "loading") {
                    ShimmerHost {
                        repeat(3) {
                            ListItemPlaceHolder()
                        }
                    }
                }
            }

            if (itemsPage?.items?.isEmpty() == true) {
                item {
                    EmptyPlaceholder(
                        icon = R.drawable.search,
                        text = stringResource(R.string.no_results_found),
                    )
                }
            }
        }

        if (searchFilter == null && searchSummary == null || searchFilter != null && itemsPage == null) {
            item {
                ShimmerHost {
                    repeat(8) {
                        ListItemPlaceHolder()
                    }
                }
            }
        }
    }

    ChipsRow(
        chips =
            listOf(
                null to stringResource(R.string.filter_all),
                FILTER_SONG to stringResource(R.string.filter_songs),
                FILTER_VIDEO to stringResource(R.string.filter_videos),
                FILTER_ALBUM to stringResource(R.string.filter_albums),
                FILTER_ARTIST to stringResource(R.string.filter_artists),
                FILTER_COMMUNITY_PLAYLIST to stringResource(R.string.filter_community_playlists),
                FILTER_FEATURED_PLAYLIST to stringResource(R.string.filter_featured_playlists),
            ),
        currentValue = searchFilter,
        onValueUpdate = {
            if (viewModel.filter.value != it) {
                viewModel.filter.value = it
            }
            coroutineScope.launch {
                lazyListState.animateScrollToItem(0)
            }
        },
        modifier =
            Modifier
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                .padding(top = AppBarHeight),
    )
}
