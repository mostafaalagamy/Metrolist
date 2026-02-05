package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.metrolist.music.R
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.viewmodels.LibrarySettingsViewModel
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.ui.component.EmptyPlaceholder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrarySettings(
    navController: NavController,
    viewModel: LibrarySettingsViewModel = hiltViewModel()
) {
    val blockedSongs by viewModel.blockedSongs.collectAsState()
    val blockedArtists by viewModel.blockedArtists.collectAsState()
    val blockedAlbums by viewModel.blockedAlbums.collectAsState()
    
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        R.string.blocked_songs,
        R.string.blocked_artists,
        R.string.blocked_albums
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.blocked_content)) },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, titleRes ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(stringResource(titleRes)) }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> {
                    if (blockedSongs.isEmpty()) {
                        EmptyPlaceholder(
                            icon = R.drawable.block,
                            text = stringResource(R.string.no_blocked_content)
                        )
                    } else {
                        LazyColumn {
                            items(blockedSongs, key = { it.songId }) { song ->
                                ListItem(
                                    headlineContent = { Text(song.songTitle) },
                                    supportingContent = song.artistName?.let { { Text(it) } },
                                    trailingContent = {
                                        TextButton(onClick = { viewModel.unblockSong(song.songId) }) {
                                            Text(stringResource(R.string.unblock))
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    if (blockedArtists.isEmpty()) {
                        EmptyPlaceholder(
                            icon = R.drawable.block,
                            text = stringResource(R.string.no_blocked_content)
                        )
                    } else {
                        LazyColumn {
                            items(blockedArtists, key = { it.artistId }) { artist ->
                                ListItem(
                                    headlineContent = { Text(artist.artistName) },
                                    trailingContent = {
                                        TextButton(onClick = { viewModel.unblockArtist(artist.artistId) }) {
                                            Text(stringResource(R.string.unblock))
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                2 -> {
                    if (blockedAlbums.isEmpty()) {
                        EmptyPlaceholder(
                            icon = R.drawable.block,
                            text = stringResource(R.string.no_blocked_content)
                        )
                    } else {
                        LazyColumn {
                            items(blockedAlbums, key = { it.albumId }) { album ->
                                ListItem(
                                    headlineContent = { Text(album.albumTitle) },
                                    supportingContent = album.artistName?.let { { Text(it) } },
                                    trailingContent = {
                                        TextButton(onClick = { viewModel.unblockAlbum(album.albumId) }) {
                                            Text(stringResource(R.string.unblock))
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
}
