package com.metrolist.music.ui.screens.playlist

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.SongSortDescendingKey
import com.metrolist.music.constants.SongSortType
import com.metrolist.music.constants.SongSortTypeKey
import com.metrolist.music.db.entities.Song
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.extensions.togglePlayPause
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.ui.component.EmptyPlaceholder
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.BorderedIconButton
import com.metrolist.music.ui.component.BorderedFloatingActionButton
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.SongListItem
import com.metrolist.music.ui.component.SortHeader
import com.metrolist.music.ui.component.PlaylistThumbnail
import com.metrolist.music.ui.component.shimmer.ListItemPlaceHolder
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.ui.component.shimmer.TextPlaceholder
import com.metrolist.music.ui.menu.SelectionSongMenu
import com.metrolist.music.ui.menu.SongMenu
import com.metrolist.music.ui.utils.ItemWrapper
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.ui.utils.adaptiveTopBarColors
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.CachePlaylistViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * Helper function to extract dominant color from image URL
 */
private suspend fun fetchDominantColor(context: Context, imageUrl: String?, defaultColor: Color): Color {
    if (imageUrl == null) return defaultColor
    return try {
        val request = ImageRequest.Builder(context)
            .data(imageUrl).size(Size(128, 128)).allowHardware(false)
            .build()
        val bitmap = (coil.ImageLoader(context).execute(request).drawable as? BitmapDrawable)?.bitmap
        bitmap?.let {
            withContext(Dispatchers.Default) { Palette.from(it).generate() }
                .getDominantColor(androidx.core.graphics.ColorUtils.setAlphaComponent(defaultColor.hashCode(), 0))
                .let { colorInt -> Color(colorInt) }
        } ?: defaultColor
    } catch (e: Exception) {
        defaultColor
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CachePlaylistScreen(
    navController: NavController,
    viewModel: CachePlaylistViewModel = hiltViewModel(),
) {
    // --- ORIGINAL LOGIC (INTACT) ---
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val cachedSongs by viewModel.cachedSongs.collectAsState()

    val (sortType, onSortTypeChange) = rememberEnumPreference(SongSortTypeKey, SongSortType.CREATE_DATE)
    val (sortDescending, onSortDescendingChange) = rememberPreference(SongSortDescendingKey, true)
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)

    val wrappedSongs = remember(cachedSongs, sortType, sortDescending) {
        val sortedSongs = when (sortType) {
            SongSortType.CREATE_DATE -> cachedSongs.sortedBy { it.song.dateDownload ?: LocalDateTime.MIN }
            SongSortType.NAME -> cachedSongs.sortedBy { it.song.title }
            SongSortType.ARTIST -> cachedSongs.sortedBy { song ->
                song.song.artistName ?: song.artists.joinToString(separator = "") { it.name }
            }
            SongSortType.PLAY_TIME -> cachedSongs.sortedBy { it.song.totalPlayTime }
        }.let { if (sortDescending) it.reversed() else it }
        sortedSongs
    }

    var inSelectMode by rememberSaveable { mutableStateOf(false) }
    val selection = rememberSaveable(
        saver = listSaver<MutableList<Int>, Int>({ it.toList() }, { it.toMutableStateList() })
    ) { mutableStateListOf() }
    val onExitSelectionMode = { inSelectMode = false; selection.clear() }
    if (inSelectMode) { BackHandler(onBack = onExitSelectionMode) }

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }

    if (isSearching) {
        BackHandler {
            isSearching = false
            query = TextFieldValue()
        }
    }

    val filteredSongs = remember(wrappedSongs, query) {
        if (query.text.isEmpty()) wrappedSongs.mapIndexed { index, song -> index to song }
        else wrappedSongs.mapIndexed { index, song -> index to song }.filter { (_, song) ->
            song.title.contains(query.text, true) ||
                song.artists.any { it.name.contains(query.text, true) }
        }
    }

    // --- NEW UI STATES ---
    val defaultColor = MaterialTheme.colorScheme.surface
    var dominantColor by remember { mutableStateOf(defaultColor) }
    val animatedBackgroundColor by animateColorAsState(dominantColor, tween(500), label = "background_color")

    LaunchedEffect(filteredSongs) {
        filteredSongs.firstOrNull()?.second?.thumbnailUrl?.let { thumbnailUrl ->
            dominantColor = fetchDominantColor(context, thumbnailUrl, defaultColor)
        }
    }

    val lazyListState = rememberLazyListState()
    val showTopBarTitle by remember { derivedStateOf { lazyListState.firstVisibleItemIndex > 0 } }

    // --- NEW VISUAL STRUCTURE ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(animatedBackgroundColor.copy(alpha = 0.4f), MaterialTheme.colorScheme.surface)))
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues()
        ) {
            if (filteredSongs.isEmpty()) {
                item {
                    if (isSearching) {
                        EmptyPlaceholder(
                            icon = R.drawable.search,
                            text = stringResource(R.string.no_results_found)
                        )
                    } else {
                        EmptyPlaceholder(
                            icon = R.drawable.music_note,
                            text = stringResource(R.string.playlist_is_empty)
                        )
                    }
                }
            } else {
                // Header (only shown when not searching)
                if (!isSearching) {
                    item {
                        CachePlaylistHeader(
                            songs = filteredSongs.map { it.second },
                            listState = lazyListState
                        )
                    }
                    item {
                        CachePlaylistActionControls(
                            onPlayClick = {
                                playerConnection.playQueue(
                                    ListQueue(title = "Cache Songs", items = filteredSongs.map { it.second.toMediaItem() })
                                )
                            },
                            onShuffleClick = {
                                playerConnection.playQueue(
                                    ListQueue(title = "Cache Songs", items = filteredSongs.shuffled().map { it.second.toMediaItem() })
                                )
                            },
                            sortType = sortType,
                            sortDescending = sortDescending,
                            onSortTypeChange = onSortTypeChange,
                            onSortDescendingChange = onSortDescendingChange
                        )
                    }
                }

                // Songs list
                itemsIndexed(filteredSongs, key = { _, (originalIndex, _) -> originalIndex }) { index, (originalIndex, song) ->
                        val onCheckedChange: (Boolean) -> Unit = { if (it) selection.add(originalIndex) else selection.remove(originalIndex) }
                        SongListItem(
                            song = song,
                            isActive = song.id == mediaMetadata?.id,
                            isPlaying = isPlaying,
                            showInLibraryIcon = true,
                            trailingContent = {
                                if (inSelectMode) {
                                    Checkbox(checked = originalIndex in selection, onCheckedChange = onCheckedChange)
                                } else {
                                    IconButton(onClick = {
                                        menuState.show {
                                            SongMenu(
                                                originalSong = song,
                                                navController = navController,
                                                onDismiss = menuState::dismiss,
                                                isFromCache = true,
                                            )
                                        }
                                    }) {
                                        Icon(painterResource(R.drawable.more_vert), null)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (inSelectMode) onCheckedChange(originalIndex !in selection)
                                        else if (song.id == mediaMetadata?.id) playerConnection.player.togglePlayPause()
                                        else playerConnection.playQueue(
                                            ListQueue(
                                                title = "Cache Songs",
                                                items = cachedSongs.map { it.toMediaItem() },
                                                startIndex = cachedSongs.indexOfFirst { it.id == song.id }
                                            )
                                        )
                                    },
                                    onLongClick = {
                                        if (!inSelectMode) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            inSelectMode = true
                                            onCheckedChange(true)
                                        }
                                    }
                                )
                                .animateItem()
                        )
                    }
                }
            }

        // Custom collapsing top bar
        CachePlaylistCollapsingTopAppBar(
            playlistTitle = stringResource(R.string.cached_playlist),
            showTitle = showTopBarTitle,
            backgroundColor = animatedBackgroundColor,
            inSelectMode = inSelectMode,
            selectionCount = selection.size,
            onExitSelectionMode = onExitSelectionMode,
            isSearching = isSearching,
            query = query,
            onQueryChange = { query = it },
            focusRequester = focusRequester,
            onSearchClose = { isSearching = false; query = TextFieldValue() },
            onSearchConfirmed = { isSearching = true },
            onNavIconClick = { navController.navigateUp() },
            onNavIconLongClick = { navController.backToMain() },
            allSelected = wrappedSongs.size == selection.size,
            onSelectAllClick = {
                if (selection.size == wrappedSongs.size) selection.clear()
                else {
                    selection.clear()
                    selection.addAll(wrappedSongs.indices)
                }
            },
            onSelectionMenuClick = {
                menuState.show {
                    SelectionSongMenu(
                        songSelection = selection.mapNotNull { wrappedSongs.getOrNull(it) },
                        onDismiss = menuState::dismiss,
                        clearAction = onExitSelectionMode
                    )
                }
            }
        )
    }
}

// --- NEW UI COMPONENTS ---

@Composable
private fun CachePlaylistHeader(
    songs: List<Song>,
    listState: LazyListState
) {
    val scrollOffset = if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset.toFloat() else Float.MAX_VALUE
    val headerHeightPx = with(LocalDensity.current) { 320.dp.toPx() }

    val imageTranslationY = -scrollOffset * 0.2f
    val textAlpha = (1f - (scrollOffset / (headerHeightPx / 2))).coerceIn(0f, 1f)

    Column(
        modifier = Modifier.fillMaxWidth().height(320.dp).graphicsLayer { translationY = imageTranslationY },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PlaylistThumbnail(
            thumbnails = songs.take(4).mapNotNull { it.thumbnailUrl }.filter { it.isNotEmpty() },
            size = 200.dp,
            placeHolder = {
                Icon(
                    painter = painterResource(R.drawable.cached),
                    contentDescription = null,
                    tint = LocalContentColor.current.copy(alpha = 0.8f),
                    modifier = Modifier.size(100.dp)
                )
            },
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(16.dp))
        Column(
            modifier = Modifier.graphicsLayer { alpha = textAlpha },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.cached_playlist),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = pluralStringResource(R.plurals.n_song, songs.size, songs.size),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CachePlaylistActionControls(
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    sortType: SongSortType,
    sortDescending: Boolean,
    onSortTypeChange: (SongSortType) -> Unit,
    onSortDescendingChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left side - Sort controls
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
            modifier = Modifier.weight(1f)
        )

        // Right side - circular shuffle and play buttons
        Row(verticalAlignment = Alignment.CenterVertically) {
            FloatingActionButton(
                onClick = onShuffleClick,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Icon(painterResource(R.drawable.shuffle), "Shuffle")
            }
            Spacer(Modifier.width(12.dp))
            FloatingActionButton(
                onClick = onPlayClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(painterResource(R.drawable.play), "Play")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CachePlaylistCollapsingTopAppBar(
    playlistTitle: String, showTitle: Boolean, backgroundColor: Color,
    inSelectMode: Boolean, selectionCount: Int, onExitSelectionMode: () -> Unit,
    isSearching: Boolean, query: TextFieldValue, onQueryChange: (TextFieldValue) -> Unit, focusRequester: FocusRequester,
    onSearchClose: () -> Unit, onSearchConfirmed: () -> Unit,
    onNavIconClick: () -> Unit, onNavIconLongClick: () -> Unit,
    allSelected: Boolean, onSelectAllClick: () -> Unit, onSelectionMenuClick: () -> Unit
) {
    val animatedColor by animateColorAsState(if (showTitle || inSelectMode || isSearching) backgroundColor.copy(alpha = 0.8f) else Color.Transparent, label = "TopBarColor")
    
    // Calculate adaptive colors based on the background color using Player.kt logic
    val adaptiveColors = adaptiveTopBarColors(animatedColor)

    TopAppBar(
        modifier = Modifier.background(animatedColor),
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        title = {
            when {
                inSelectMode -> Text(
                    text = pluralStringResource(R.plurals.n_selected, selectionCount, selectionCount),
                    color = adaptiveColors.titleColor
                )
                isSearching -> TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { 
                        Text(
                            text = stringResource(R.string.search), 
                            style = MaterialTheme.typography.titleLarge,
                            color = adaptiveColors.subtitleColor
                        ) 
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleLarge.copy(color = adaptiveColors.titleColor),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent, 
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent, 
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = adaptiveColors.titleColor,
                        unfocusedTextColor = adaptiveColors.titleColor,
                        cursorColor = adaptiveColors.actionColor
                    ),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                )
                showTitle -> Text(
                    text = playlistTitle, 
                    maxLines = 1, 
                    overflow = TextOverflow.Ellipsis,
                    color = adaptiveColors.titleColor
                )
            }
        },
        navigationIcon = {
            val navIcon = if (inSelectMode || isSearching) R.drawable.close else R.drawable.arrow_back
            IconButton(
                onClick = {
                    when {
                        inSelectMode -> onExitSelectionMode()
                        isSearching -> onSearchClose()
                        else -> onNavIconClick()
                    }
                },
                onLongClick = { if (!inSelectMode && !isSearching) onNavIconLongClick() }
            ) {
                Icon(
                    painter = painterResource(navIcon), 
                    contentDescription = null, 
                    tint = adaptiveColors.iconColor
                )
            }
        },
        actions = {
            when {
                inSelectMode -> {
                    Checkbox(checked = allSelected, onCheckedChange = { onSelectAllClick() })
                    IconButton(enabled = selectionCount > 0, onClick = onSelectionMenuClick) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert), 
                            contentDescription = null,
                            tint = adaptiveColors.iconColor
                        )
                    }
                }
                !isSearching -> {
                    IconButton(onClick = onSearchConfirmed) { 
                        Icon(
                            painter = painterResource(R.drawable.search), 
                            contentDescription = null,
                            tint = adaptiveColors.iconColor
                        ) 
                    }
                }
            }
        }
    )
}
