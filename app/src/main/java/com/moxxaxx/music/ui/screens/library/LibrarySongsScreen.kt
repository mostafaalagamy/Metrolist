package com.moxxaxx.music.ui.screens.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.moxxaxx.music.LocalPlayerAwareWindowInsets
import com.moxxaxx.music.LocalPlayerConnection
import com.moxxaxx.music.R
import com.moxxaxx.music.constants.CONTENT_TYPE_HEADER
import com.moxxaxx.music.constants.CONTENT_TYPE_SONG
import com.moxxaxx.music.constants.SongFilter
import com.moxxaxx.music.constants.SongFilterKey
import com.moxxaxx.music.constants.SongSortDescendingKey
import com.moxxaxx.music.constants.SongSortType
import com.moxxaxx.music.constants.SongSortTypeKey
import com.moxxaxx.music.extensions.toMediaItem
import com.moxxaxx.music.extensions.togglePlayPause
import com.moxxaxx.music.playback.queues.ListQueue
import com.moxxaxx.music.ui.component.ChipsRow
import com.moxxaxx.music.ui.component.HideOnScrollFAB
import com.moxxaxx.music.ui.component.LocalMenuState
import com.moxxaxx.music.ui.component.SongListItem
import com.moxxaxx.music.ui.component.SortHeader
import com.moxxaxx.music.ui.menu.SelectionSongMenu
import com.moxxaxx.music.ui.menu.SongMenu
import com.moxxaxx.music.ui.utils.ItemWrapper
import com.moxxaxx.music.utils.rememberEnumPreference
import com.moxxaxx.music.utils.rememberPreference
import com.moxxaxx.music.viewmodels.LibrarySongsViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibrarySongsScreen(
    navController: NavController,
    onDeselect: () -> Unit,
    viewModel: LibrarySongsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val (sortType, onSortTypeChange) = rememberEnumPreference(SongSortTypeKey, SongSortType.CREATE_DATE)
    val (sortDescending, onSortDescendingChange) = rememberPreference(SongSortDescendingKey, true)

    val songs by viewModel.allSongs.collectAsState()

    val wrappedSongs = songs.map { item -> ItemWrapper(item) }.toMutableList()
    var selection by remember {
        mutableStateOf(false)
    }

    var filter by rememberEnumPreference(SongFilterKey, SongFilter.LIBRARY)

    val lazyListState = rememberLazyListState()

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        ) {
            item(
                key = "filter",
                contentType = CONTENT_TYPE_HEADER,
            ) {
                Row {
                    Spacer(Modifier.width(12.dp))
                    FilterChip(
                        label = { Text(stringResource(R.string.songs)) },
                        selected = true,
                        colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.background),
                        onClick = onDeselect,
                        leadingIcon = {
                            Icon(painter = painterResource(R.drawable.close), contentDescription = "")
                        },
                    )
                    ChipsRow(
                        chips =
                            listOf(
                                SongFilter.LIBRARY to stringResource(R.string.filter_library),
                                SongFilter.LIKED to stringResource(R.string.filter_liked),
                                SongFilter.DOWNLOADED to stringResource(R.string.filter_downloaded),
                            ),
                        currentValue = filter,
                        onValueUpdate = {
                            filter = it
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item(
                key = "header",
                contentType = CONTENT_TYPE_HEADER,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    if (selection) {
                        val count = wrappedSongs.count { it.isSelected }
                        Text(text = pluralStringResource(R.plurals.n_elements, count, count), modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = {
                                if (count == wrappedSongs.size) {
                                    wrappedSongs.forEach { it.isSelected = false }
                                } else {
                                    wrappedSongs.forEach { it.isSelected = true }
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(if (count == wrappedSongs.size) R.drawable.deselect else R.drawable.select_all),
                                contentDescription = null,
                            )
                        }

                        IconButton(
                            onClick = {
                                menuState.show {
                                    SelectionSongMenu(
                                        songSelection = wrappedSongs.filter { it.isSelected }.map { it.item },
                                        onDismiss = menuState::dismiss,
                                        clearAction = { selection = false },
                                    )
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null,
                            )
                        }

                        IconButton(
                            onClick = { selection = false },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.close),
                                contentDescription = null,
                            )
                        }
                    } else {
                        SortHeader(
                            sortType = sortType,
                            sortDescending = sortDescending,
                            onSortTypeChange = onSortTypeChange,
                            onSortDescendingChange = onSortDescendingChange,
                            sortTypeText = { sortType ->
                                when (sortType) {
                                    SongSortType.CREATE_DATE -> R.string.sort_by_create_date
                                    SongSortType.NAME -> R.string.sort_by_name
                                    SongSortType.ARTIST -> R.string.sort_by_artist
                                    SongSortType.PLAY_TIME -> R.string.sort_by_play_time
                                }
                            },
                        )

                        Spacer(Modifier.weight(1f))

                        IconButton(
                            onClick = { selection = !selection },
                            modifier = Modifier.padding(horizontal = 6.dp),
                        ) {
                            Icon(
                                painter = painterResource(if (selection) R.drawable.deselect else R.drawable.select_all),
                                contentDescription = null,
                            )
                        }

                        Text(
                            text = pluralStringResource(R.plurals.n_song, songs.size, songs.size),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }

            itemsIndexed(
                items = wrappedSongs,
                key = { _, item -> item.item.song.id },
                contentType = { _, _ -> CONTENT_TYPE_SONG },
            ) { index, songWrapper ->
                SongListItem(
                    song = songWrapper.item,
                    isActive = songWrapper.item.id == mediaMetadata?.id,
                    isPlaying = isPlaying,
                    trailingContent = {
                        IconButton(
                            onClick = {
                                menuState.show {
                                    SongMenu(
                                        originalSong = songWrapper.item,
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
                    isSelected = songWrapper.isSelected && selection,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (!selection) {
                                        if (songWrapper.item.id == mediaMetadata?.id) {
                                            playerConnection.player.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = context.getString(R.string.queue_all_songs),
                                                    items = songs.map { it.toMediaItem() },
                                                    startIndex = index,
                                                ),
                                            )
                                        }
                                    } else {
                                        songWrapper.isSelected = !songWrapper.isSelected
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        SongMenu(
                                            originalSong = songWrapper.item,
                                            navController = navController,
                                            onDismiss = menuState::dismiss,
                                        )
                                    }
                                },
                            ).animateItemPlacement(),
                )
            }
        }

        HideOnScrollFAB(
            visible = songs.isNotEmpty(),
            lazyListState = lazyListState,
            icon = R.drawable.shuffle,
            onClick = {
                playerConnection.playQueue(
                    ListQueue(
                        title = context.getString(R.string.queue_all_songs),
                        items = songs.shuffled().map { it.toMediaItem() },
                    ),
                )
            },
        )
    }
}
