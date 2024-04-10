package com.zionhuang.music.ui.screens.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.zionhuang.music.LocalDatabase
import com.zionhuang.music.LocalPlayerAwareWindowInsets
import com.zionhuang.music.R
import com.zionhuang.music.constants.CONTENT_TYPE_HEADER
import com.zionhuang.music.constants.CONTENT_TYPE_PLAYLIST
import com.zionhuang.music.constants.GridThumbnailHeight
import com.zionhuang.music.constants.LibraryViewType
import com.zionhuang.music.constants.PlaylistSortDescendingKey
import com.zionhuang.music.constants.PlaylistSortType
import com.zionhuang.music.constants.PlaylistSortTypeKey
import com.zionhuang.music.constants.PlaylistViewTypeKey
import com.zionhuang.music.db.entities.Playlist
import com.zionhuang.music.db.entities.PlaylistEntity
import com.zionhuang.music.ui.component.HideOnScrollFAB
import com.zionhuang.music.ui.component.LocalMenuState
import com.zionhuang.music.ui.component.PlaylistGridItem
import com.zionhuang.music.ui.component.PlaylistListItem
import com.zionhuang.music.ui.component.SortHeader
import com.zionhuang.music.ui.component.TextFieldDialog
import com.zionhuang.music.ui.menu.PlaylistMenu
import com.zionhuang.music.utils.rememberEnumPreference
import com.zionhuang.music.utils.rememberPreference
import com.zionhuang.music.viewmodels.LibraryPlaylistsViewModel
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryPlaylistsScreen(
    navController: NavController,
    filterContent: @Composable () -> Unit,
    viewModel: LibraryPlaylistsViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current

    val coroutineScope = rememberCoroutineScope()

    var viewType by rememberEnumPreference(PlaylistViewTypeKey, LibraryViewType.GRID)
    val (sortType, onSortTypeChange) = rememberEnumPreference(PlaylistSortTypeKey, PlaylistSortType.CREATE_DATE)
    val (sortDescending, onSortDescendingChange) = rememberPreference(PlaylistSortDescendingKey, true)

    val playlists by viewModel.allPlaylists.collectAsState()

    val topSize by viewModel.topValue.collectAsState(initial = 50)
    val likedPlaylist = Playlist(
        playlist = PlaylistEntity(id = UUID.randomUUID().toString(), name = "Liked"),
        songCount = 0,
        thumbnails = emptyList()
    )

    val downloadPlaylist = Playlist(
        playlist = PlaylistEntity(id = UUID.randomUUID().toString(), name = "Offline"),
        songCount = 0,
        thumbnails = emptyList()
    )

    val topSizeInt = topSize.toString().toInt()

    val topPlaylist = Playlist(
        playlist = PlaylistEntity(id = UUID.randomUUID().toString(), name = "My Top $topSize"),
        songCount = 0,
        thumbnails = emptyList()
    )

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()

    var showAddPlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showAddPlaylistDialog) {
        TextFieldDialog(
            icon = { Icon(painter = painterResource(R.drawable.add), contentDescription = null) },
            title = { Text(text = stringResource(R.string.create_playlist)) },
            onDismiss = { showAddPlaylistDialog = false },
            onDone = { playlistName ->
                database.query {
                    insert(
                        PlaylistEntity(
                            name = playlistName
                        )
                    )
                }
            }
        )
    }

    val headerContent = @Composable {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp)
        ) {
            SortHeader(
                sortType = sortType,
                sortDescending = sortDescending,
                onSortTypeChange = onSortTypeChange,
                onSortDescendingChange = onSortDescendingChange,
                sortTypeText = { sortType ->
                    when (sortType) {
                        PlaylistSortType.CREATE_DATE -> R.string.sort_by_create_date
                        PlaylistSortType.NAME -> R.string.sort_by_name
                        PlaylistSortType.SONG_COUNT -> R.string.sort_by_song_count
                    }
                }
            )

            Spacer(Modifier.weight(1f))

            Text(
                text = pluralStringResource(R.plurals.n_playlist, playlists.size, playlists.size),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary
            )

            IconButton(
                onClick = {
                    viewType = viewType.toggle()
                },
                modifier = Modifier.padding(start = 6.dp, end = 6.dp)
            ) {
                Icon(
                    painter = painterResource(
                        when (viewType) {
                            LibraryViewType.LIST -> R.drawable.list
                            LibraryViewType.GRID -> R.drawable.grid_view
                        }
                    ),
                    contentDescription = null
                )
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when (viewType) {
            LibraryViewType.LIST -> {
                LazyColumn(
                    state = lazyListState,
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
                ) {
                    item(
                        key = "filter",
                        contentType = CONTENT_TYPE_HEADER
                    ) {
                        filterContent()
                    }

                    item(
                        key = "header",
                        contentType = CONTENT_TYPE_HEADER
                    ) {
                        headerContent()
                    }


                    item(
                        key = "likedPlaylist",
                        contentType = { CONTENT_TYPE_PLAYLIST }
                    ) {
                        PlaylistListItem(
                            playlist = likedPlaylist,
                            autoPlaylist = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navController.navigate("auto_playlist/liked")
                                }
                                .animateItemPlacement()
                        )
                    }


                    item(
                        key = "downloadedPlaylist",
                        contentType = { CONTENT_TYPE_PLAYLIST }
                    ) {
                        PlaylistListItem(
                            playlist = downloadPlaylist,
                            autoPlaylist = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navController.navigate("auto_playlist/downloaded")
                                }
                                .animateItemPlacement()
                        )
                    }

                    item(
                        key = "TopPlaylist",
                        contentType = { CONTENT_TYPE_PLAYLIST }
                    ) {
                        PlaylistListItem(
                            playlist = topPlaylist,
                            autoPlaylist = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navController.navigate("top_playlist/$topSize")
                                }
                                .animateItemPlacement()
                        )
                    }

                    items(
                        items = playlists,
                        key = { it.id },
                        contentType = { CONTENT_TYPE_PLAYLIST }
                    ) { playlist ->
                        PlaylistListItem(
                            playlist = playlist,
                            trailingContent = {
                                IconButton(
                                    onClick = {
                                        menuState.show {
                                            PlaylistMenu(
                                                playlist = playlist,
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
                                .clickable {
                                    navController.navigate("local_playlist/${playlist.id}")
                                }
                                .animateItemPlacement()
                        )
                    }
                }

                HideOnScrollFAB(
                    lazyListState = lazyListState,
                    icon = R.drawable.add,
                    onClick = {
                        showAddPlaylistDialog = true
                    }
                )
            }

            LibraryViewType.GRID -> {
                LazyVerticalGrid(
                    state = lazyGridState,
                    columns = GridCells.Adaptive(minSize = GridThumbnailHeight + 24.dp),
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
                ) {
                    item(
                        key = "filter",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER
                    ) {
                        filterContent()
                    }

                    item(
                        key = "header",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER
                    ) {
                        headerContent()
                    }

                    item(
                        key = "likedPlaylist",
                        contentType = { CONTENT_TYPE_PLAYLIST }
                    ) {
                        PlaylistGridItem(
                            playlist = likedPlaylist,
                            fillMaxWidth = true,
                            autoPlaylist = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        navController.navigate("auto_playlist/liked")
                                    },
                                )
                                .animateItemPlacement()
                        )
                    }

                    item(
                        key = "downloadedPlaylist",
                        contentType = { CONTENT_TYPE_PLAYLIST }
                    ) {
                        PlaylistGridItem(
                            playlist = downloadPlaylist,
                            fillMaxWidth = true,
                            autoPlaylist = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        navController.navigate("auto_playlist/downloaded")
                                    },
                                )
                                .animateItemPlacement()
                        )
                    }

                    item(
                        key = "TopPlaylist",
                        contentType = { CONTENT_TYPE_PLAYLIST }
                    ) {
                        PlaylistGridItem(
                            playlist = topPlaylist,
                            fillMaxWidth = true,
                            autoPlaylist = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        navController.navigate("top_playlist/$topSize")
                                    },
                                )
                                .animateItemPlacement()
                        )
                    }

                    items(
                        items = playlists,
                        key = { it.id },
                        contentType = { CONTENT_TYPE_PLAYLIST }
                    ) { playlist ->
                        PlaylistGridItem(
                            playlist = playlist,
                            fillMaxWidth = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        navController.navigate("local_playlist/${playlist.id}")
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        menuState.show {
                                            PlaylistMenu(
                                                playlist = playlist,
                                                coroutineScope = coroutineScope,
                                                onDismiss = menuState::dismiss
                                            )
                                        }
                                    }
                                )
                                .animateItemPlacement()
                        )
                    }
                }

                HideOnScrollFAB(
                    lazyListState = lazyGridState,
                    icon = R.drawable.add,
                    onClick = {
                        showAddPlaylistDialog = true
                    }
                )
            }
        }

    }
}
