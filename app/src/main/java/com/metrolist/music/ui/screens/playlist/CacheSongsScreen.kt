package com.metrolist.music.ui.screens.playlist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.db.entities.Song
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.extensions.togglePlayPause
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.ui.component.*
import com.metrolist.music.ui.menu.SelectionSongMenu
import com.metrolist.music.ui.menu.SongMenu
import com.metrolist.music.ui.utils.ItemWrapper
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.viewmodels.HistoryViewModel

@OptIn(ExperimentalFoundationApi::class
@Composable fun CacheSongsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
val context = LocalContext.current
val menuState = LocalMenuState.current
val playerConnection = LocalPlayerConnection.current ?: return
val haptic = LocalHapticFeedback.current

val isPlaying by playerConnection.isPlaying.collectAsState()
val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
val events by viewModel.events.collectAsState()

val allSongs = remember(events) {
    events.values.flatten().map { it.song }.distinctBy { it.id }
}

val wrappedSongs = remember(allSongs) {
    mutableStateListOf<ItemWrapper<Song>>().apply {
        addAll(allSongs.map { ItemWrapper(it) })
    }
}

var selection by remember { mutableStateOf(false) }
var isSearching by remember { mutableStateOf(false) }
var query by remember { mutableStateOf(TextFieldValue()) }
val focusRequester = remember { FocusRequester() }
val lazyListState = rememberLazyListState()

val filteredSongs = remember(wrappedSongs, query) {
    if (query.text.isEmpty()) wrappedSongs
    else wrappedSongs.filter { wrapper ->
        val song = wrapper.item
        song.title.contains(query.text, true) ||
            song.artists.any { it.name.contains(query.text, true) }
    }
}

Column(modifier = Modifier.fillMaxSize()) {
    if (!isSearching && filteredSongs.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = filteredSongs.first().item.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Cache Songs", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = pluralStringResource(
                        id = R.plurals.n_song,
                        count = filteredSongs.size,
                        filteredSongs.size
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    if (filteredSongs.isEmpty()) {
        EmptyPlaceholder(
            icon = R.drawable.music_note,
            text = stringResource(R.string.playlist_is_empty)
        )
    } else {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(filteredSongs, key = { _, song -> song.item.id }) { index, songWrapper ->
                SongListItem(
                    song = songWrapper.item,
                    isActive = songWrapper.item.id == mediaMetadata?.id,
                    isPlaying = isPlaying,
                    isSelected = songWrapper.isSelected && selection,
                    showInLibraryIcon = true,
                    trailingContent = {
                        IconButton(onClick = {
                            menuState.show {
                                SongMenu(
                                    originalSong = songWrapper.item,
                                    navController = navController,
                                    onDismiss = menuState::dismiss,
                                )
                            }
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .combinedClickable(
                            onClick = {
                                if (!selection) {
                                    if (songWrapper.item.id == mediaMetadata?.id) {
                                        playerConnection.player.togglePlayPause()
                                    } else {
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title = "Cache Songs",
                                                items = allSongs.map { it.toMediaItem() },
                                                startIndex = index
                                            )
                                        )
                                    }
                                } else {
                                    songWrapper.isSelected = !songWrapper.isSelected
                                }
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (!selection) {
                                    selection = true
                                    wrappedSongs.forEach { it.isSelected = false }
                                    songWrapper.isSelected = true
                                }
                            }
                        )
                )
            }
        }

        HideOnScrollFAB(
            visible = filteredSongs.isNotEmpty(),
            lazyListState = lazyListState,
            icon = R.drawable.shuffle,
            onClick = {
                playerConnection.playQueue(
                    ListQueue(
                        title = "Cache Songs",
                        items = filteredSongs.map { it.item.toMediaItem() }.shuffled()
                    )
                )
            }
        )
    }
}

TopAppBar(
    title = {
        when {
            selection -> {
                val count = wrappedSongs.count { it.isSelected }
                Text(
                    text = pluralStringResource(R.plurals.n_song, count, count),
                    style = MaterialTheme.typography.titleLarge
                )
            }

            isSearching -> {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search),
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleLarge,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(focusRequester)
                )
            }

            else -> {
                Text("Cache Songs", style = MaterialTheme.typography.titleLarge)
            }
        }
    },
    navigationIcon = {
        IconButton(onClick = {
            when {
                isSearching -> {
                    isSearching = false
                    query = TextFieldValue()
                }

                selection -> {
                    selection = false
                }

                else -> {
                    navController.navigateUp()
                }
            }
        }, onLongClick = {
            if (!isSearching && !selection) {
                navController.backToMain()
            }
        }) {
            Icon(
                painter = painterResource(
                    if (selection) R.drawable.close else R.drawable.arrow_back
                ),
                contentDescription = null
            )
        }
    },
    actions = {
        if (selection) {
            val count = wrappedSongs.count { it.isSelected }
            IconButton(onClick = {
                if (count == wrappedSongs.size) {
                    wrappedSongs.forEach { it.isSelected = false }
                } else {
                    wrappedSongs.forEach { it.isSelected = true }
                }
            }) {
                Icon(
                    painter = painterResource(
                        if (count == wrappedSongs.size) R.drawable.deselect else R.drawable.select_all
                    ),
                    contentDescription = null
                )
            }

            IconButton(onClick = {
                menuState.show {
                    SelectionSongMenu(
                        songSelection = wrappedSongs.filter { it.isSelected }.map { it.item },
                        onDismiss = menuState::dismiss,
                        clearAction = { selection = false }
                    )
                }
            }) {
                Icon(
                    painter = painterResource(R.drawable.more_vert),
                    contentDescription = null
                )
            }
        } else if (!isSearching) {
            IconButton(onClick = { isSearching = true }) {
                Icon(
                    painter = painterResource(R.drawable.search),
                    contentDescription = null
                )
            }
        }
    },
    scrollBehavior = scrollBehavior
)

}

