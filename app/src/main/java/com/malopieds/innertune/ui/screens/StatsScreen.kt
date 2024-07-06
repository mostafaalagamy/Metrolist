package com.malopieds.innertune.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.malopieds.innertube.models.WatchEndpoint
import com.malopieds.innertune.LocalPlayerAwareWindowInsets
import com.malopieds.innertune.LocalPlayerConnection
import com.malopieds.innertune.R
import com.malopieds.innertune.constants.StatPeriod
import com.malopieds.innertune.extensions.togglePlayPause
import com.malopieds.innertune.models.toMediaMetadata
import com.malopieds.innertune.playback.queues.YouTubeQueue
import com.malopieds.innertune.ui.component.AlbumGridItem
import com.malopieds.innertune.ui.component.ArtistGridItem
import com.malopieds.innertune.ui.component.ChipsRow
import com.malopieds.innertune.ui.component.IconButton
import com.malopieds.innertune.ui.component.LocalMenuState
import com.malopieds.innertune.ui.component.NavigationTitle
import com.malopieds.innertune.ui.component.SongListItem
import com.malopieds.innertune.ui.menu.AlbumMenu
import com.malopieds.innertune.ui.menu.ArtistMenu
import com.malopieds.innertune.ui.menu.SongMenu
import com.malopieds.innertune.ui.utils.backToMain
import com.malopieds.innertune.viewmodels.StatsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StatsScreen(
    navController: NavController,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val statPeriod by viewModel.statPeriod.collectAsState()
    val mostPlayedSongs by viewModel.mostPlayedSongs.collectAsState()
    val mostPlayedArtists by viewModel.mostPlayedArtists.collectAsState()
    val mostPlayedAlbums by viewModel.mostPlayedAlbums.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        contentPadding =
            LocalPlayerAwareWindowInsets.current
                .only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                ).asPaddingValues(),
        modifier = Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)),
    ) {
        item {
            ChipsRow(
                chips =
                    listOf(
                        StatPeriod.WEEK_1 to pluralStringResource(R.plurals.n_week, 1, 1),
                        StatPeriod.MONTH_1 to pluralStringResource(R.plurals.n_month, 1, 1),
                        StatPeriod.MONTH_3 to pluralStringResource(R.plurals.n_month, 3, 3),
                        StatPeriod.MONTH_6 to pluralStringResource(R.plurals.n_month, 6, 6),
                        StatPeriod.YEAR_1 to pluralStringResource(R.plurals.n_year, 1, 1),
                        StatPeriod.ALL to stringResource(R.string.filter_all),
                    ),
                currentValue = statPeriod,
                onValueUpdate = { viewModel.statPeriod.value = it },
            )
        }

        item(key = "mostPlayedSongs") {
            NavigationTitle(
                title = stringResource(R.string.most_played_songs),
                modifier = Modifier.animateItemPlacement(),
            )
        }

        items(
            items = mostPlayedSongs,
            key = { it.id },
        ) { song ->
            SongListItem(
                song = song,
                isActive = song.id == mediaMetadata?.id,
                isPlaying = isPlaying,
                trailingContent = {
                    IconButton(
                        onClick = {
                            menuState.show {
                                SongMenu(
                                    originalSong = song,
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
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                if (song.id == mediaMetadata?.id) {
                                    playerConnection.player.togglePlayPause()
                                } else {
                                    playerConnection.playQueue(
                                        YouTubeQueue(
                                            endpoint = WatchEndpoint(song.id),
                                            preloadItem = song.toMediaMetadata(),
                                        ),
                                    )
                                }
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                    SongMenu(
                                        originalSong = song,
                                        navController = navController,
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            },
                        ).animateItemPlacement(),
            )
        }

        item(key = "mostPlayedArtists") {
            NavigationTitle(
                title = stringResource(R.string.most_played_artists),
                modifier = Modifier.animateItemPlacement(),
            )

            LazyRow(
                modifier = Modifier.animateItemPlacement(),
            ) {
                items(
                    items = mostPlayedArtists,
                    key = { it.id },
                ) { artist ->
                    ArtistGridItem(
                        artist = artist,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        navController.navigate("artist/${artist.id}")
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        menuState.show {
                                            ArtistMenu(
                                                originalArtist = artist,
                                                coroutineScope = coroutineScope,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    },
                                ).animateItemPlacement(),
                    )
                }
            }
        }

        if (mostPlayedAlbums.isNotEmpty()) {
            item(key = "mostPlayedAlbums") {
                NavigationTitle(
                    title = stringResource(R.string.most_played_albums),
                    modifier = Modifier.animateItemPlacement(),
                )

                LazyRow(
                    modifier = Modifier.animateItemPlacement(),
                ) {
                    items(
                        items = mostPlayedAlbums,
                        key = { it.id },
                    ) { album ->
                        AlbumGridItem(
                            album = album,
                            isActive = album.id == mediaMetadata?.album?.id,
                            isPlaying = isPlaying,
                            coroutineScope = coroutineScope,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("album/${album.id}")
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                AlbumMenu(
                                                    originalAlbum = album,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                    ).animateItemPlacement(),
                        )
                    }
                }
            }
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.stats)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
    )
}
