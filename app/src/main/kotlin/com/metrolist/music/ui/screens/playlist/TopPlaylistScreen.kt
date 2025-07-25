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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastSumBy
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import androidx.compose.ui.layout.ContentScale
import com.metrolist.music.LocalDownloadUtil
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.MyTopFilter
import com.metrolist.music.db.entities.Song
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.extensions.togglePlayPause
import com.metrolist.music.playback.ExoDownloadService
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.ui.component.EmptyPlaceholder
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.BorderedIconButton
import com.metrolist.music.ui.component.BorderedFloatingActionButton
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.SongListItem
import com.metrolist.music.ui.component.SortHeader
import com.metrolist.music.ui.component.shimmer.ListItemPlaceHolder
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.ui.component.shimmer.TextPlaceholder
import com.metrolist.music.ui.menu.SelectionSongMenu
import com.metrolist.music.ui.menu.SongMenu
import com.metrolist.music.ui.utils.ItemWrapper
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.makeTimeString
import com.metrolist.music.viewmodels.TopPlaylistViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
fun TopPlaylistScreen(
    navController: NavController,
    viewModel: TopPlaylistViewModel = hiltViewModel(),
) {
    // --- ORIGINAL LOGIC (INTACT) ---
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val maxSize = viewModel.top

    val songs by viewModel.topSongs.collectAsState(null)
    val likeLength = remember(songs) { songs?.fastSumBy { it.song.duration } ?: 0 }

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }
    
    var inSelectMode by rememberSaveable { mutableStateOf(false) }
    val selection = rememberSaveable(
        saver = listSaver<MutableList<Int>, Int>({ it.toList() }, { it.toMutableStateList() })
    ) { mutableStateListOf() }
    val onExitSelectionMode = { inSelectMode = false; selection.clear() }
    if (inSelectMode) { BackHandler(onBack = onExitSelectionMode) }

    // Download state management
    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember { mutableStateOf(Download.STATE_STOPPED) }
    
    LaunchedEffect(songs) {
        val songList = songs?.map { it.song.id }
        if (songList.isNullOrEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState = if (songList.all { downloads[it]?.state == Download.STATE_COMPLETED }) {
                Download.STATE_COMPLETED
            } else if (songList.all {
                    downloads[it]?.state == Download.STATE_QUEUED ||
                            downloads[it]?.state == Download.STATE_DOWNLOADING ||
                            downloads[it]?.state == Download.STATE_COMPLETED
                }) {
                Download.STATE_DOWNLOADING
            } else {
                Download.STATE_STOPPED
            }
        }
    }

    if (isSearching) {
        BackHandler {
            isSearching = false
            query = TextFieldValue()
        }
    }

    val sortType by viewModel.topPeriod.collectAsState()
    val name = stringResource(R.string.my_top) + " $maxSize"

    val filteredSongs = remember(songs, query) {
        if (query.text.isEmpty()) songs?.mapIndexed { index, song -> index to song } ?: emptyList()
        else songs?.mapIndexed { index, song -> index to song }?.filter { (_, song) ->
            song.song.title.contains(query.text, ignoreCase = true) ||
                    song.artists.any { it.name.contains(query.text, ignoreCase = true) }
        } ?: emptyList()
    }

    // --- NEW UI STATES ---
    val defaultColor = MaterialTheme.colorScheme.surface
    var dominantColor by remember { mutableStateOf(defaultColor) }
    val animatedBackgroundColor by animateColorAsState(dominantColor, tween(500), label = "background_color")
    
    LaunchedEffect(songs) {
        songs?.firstOrNull()?.song?.thumbnailUrl?.let { thumbnailUrl ->
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
        if (songs == null) {
            TopPlaylistScreenSkeleton()
        } else {
            LazyColumn(
                state = lazyListState,
                contentPadding = LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues()
            ) {
                if (songs!!.isEmpty()) {
                    item {
                        EmptyPlaceholder(
                            icon = R.drawable.music_note,
                            text = stringResource(R.string.playlist_is_empty),
                        )
                    }
                } else {
                    // Header (only shown when not searching)
                    if (!isSearching) {
                        item {
                            TopPlaylistHeader(
                                name = name,
                                songs = songs!!,
                                likeLength = likeLength,
                                listState = lazyListState,
                                downloadState = downloadState,
                                onDownloadClick = {
                                    songs!!.forEach { song ->
                                        val downloadRequest = DownloadRequest.Builder(song.song.id, song.song.id.toUri())
                                            .setCustomCacheKey(song.song.id)
                                            .setData(song.song.title.toByteArray())
                                            .build()
                                        DownloadService.sendAddDownload(context, ExoDownloadService::class.java, downloadRequest, false)
                                    }
                                },
                                onRemoveDownloadClick = {
                                    songs!!.forEach { song ->
                                        DownloadService.sendRemoveDownload(context, ExoDownloadService::class.java, song.song.id, false)
                                    }
                                }
                            )
                        }
                        item {
                            TopPlaylistActionControls(
                                onPlayClick = {
                                    playerConnection.playQueue(
                                        ListQueue(title = name, items = songs!!.map { it.toMediaItem() })
                                    )
                                },
                                onShuffleClick = {
                                    playerConnection.playQueue(
                                        ListQueue(title = name, items = songs!!.shuffled().map { it.toMediaItem() })
                                    )
                                },
                                onQueueClick = {
                                    playerConnection.addToQueue(items = songs!!.map { it.toMediaItem() })
                                },
                                onDownloadClick = {
                                    when (downloadState) {
                                        Download.STATE_COMPLETED -> {
                                            songs?.forEach { song ->
                                                DownloadService.sendRemoveDownload(context, ExoDownloadService::class.java, song.song.id, false)
                                            }
                                        }
                                        Download.STATE_DOWNLOADING -> {
                                            songs?.forEach { song ->
                                                DownloadService.sendRemoveDownload(context, ExoDownloadService::class.java, song.song.id, false)
                                            }
                                        }
                                        else -> {
                                            songs?.forEach { song ->
                                                val downloadRequest = DownloadRequest.Builder(song.song.id, song.song.id.toUri())
                                                    .setCustomCacheKey(song.song.id)
                                                    .setData(song.song.title.toByteArray())
                                                    .build()
                                                DownloadService.sendAddDownload(context, ExoDownloadService::class.java, downloadRequest, false)
                                            }
                                        }
                                    }
                                },
                                downloadState = downloadState
                            )
                        }
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 16.dp),
                            ) {
                                SortHeader(
                                    sortType = sortType,
                                    sortDescending = false,
                                    onSortTypeChange = { viewModel.topPeriod.value = it },
                                    onSortDescendingChange = {},
                                    sortTypeText = { sortType ->
                                        when (sortType) {
                                            MyTopFilter.ALL_TIME -> R.string.all_time
                                            MyTopFilter.DAY -> R.string.past_24_hours
                                            MyTopFilter.WEEK -> R.string.past_week
                                            MyTopFilter.MONTH -> R.string.past_month
                                            MyTopFilter.YEAR -> R.string.past_year
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    showDescending = false,
                                )
                            }
                        }
                    }

                    // Songs list
                    itemsIndexed(items = filteredSongs, key = { index, _ -> index }) { index, (originalIndex, song) ->
                        val onCheckedChange: (Boolean) -> Unit = { if (it) selection.add(originalIndex) else selection.remove(originalIndex) }
                        SongListItem(
                            song = song,
                            albumIndex = index + 1,
                            isActive = song.song.id == mediaMetadata?.id,
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
                                                onDismiss = menuState::dismiss
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
                                        else if (song.song.id == mediaMetadata?.id) playerConnection.player.togglePlayPause()
                                        else playerConnection.playQueue(
                                            ListQueue(
                                                title = name,
                                                items = songs!!.map { it.toMediaItem() },
                                                startIndex = songs!!.indexOfFirst { it.id == song.id }
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
        }

        // Custom collapsing top bar
        TopPlaylistCollapsingTopAppBar(
            playlistTitle = name,
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
            allSelected = songs?.size == selection.size,
            onSelectAllClick = {
                if (selection.size == songs?.size) selection.clear()
                else {
                    selection.clear()
                    songs?.indices?.let { selection.addAll(it) }
                }
            },
            onSelectionMenuClick = {
                menuState.show {
                    SelectionSongMenu(
                        songSelection = selection.mapNotNull { songs?.getOrNull(it) },
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
private fun TopPlaylistHeader(
    name: String,
    songs: List<Song>,
    likeLength: Int,
    listState: LazyListState,
    downloadState: Int,
    onDownloadClick: () -> Unit,
    onRemoveDownloadClick: () -> Unit
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
        AsyncImage(
            model = songs.first().song.thumbnailUrl,
            contentDescription = "Playlist Thumbnail",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(200.dp).clip(RoundedCornerShape(12.dp)).shadow(16.dp, RoundedCornerShape(12.dp))
        )
        Spacer(Modifier.height(16.dp))
        Column(
            modifier = Modifier.graphicsLayer { alpha = textAlpha },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = pluralStringResource(R.plurals.n_song, songs.size, songs.size),
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = makeTimeString(likeLength * 1000L),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TopPlaylistActionControls(
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onQueueClick: () -> Unit,
    onDownloadClick: () -> Unit,
    downloadState: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left side - queue and download buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BorderedIconButton(
                onClick = onQueueClick,
                modifier = Modifier.size(40.dp)
            ) { Icon(painterResource(R.drawable.queue_music), "Queue", modifier = Modifier.size(24.dp)) }
            when (downloadState) {
                Download.STATE_COMPLETED -> BorderedIconButton(
                    onClick = onDownloadClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(painterResource(R.drawable.offline), "Downloaded", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
                Download.STATE_DOWNLOADING -> BorderedIconButton(
                    onClick = onDownloadClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
                else -> BorderedIconButton(
                    onClick = onDownloadClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(painterResource(R.drawable.download), "Download", modifier = Modifier.size(24.dp))
                }
            }
        }
        
        // Right side - circular radio and shuffle buttons
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
private fun TopPlaylistCollapsingTopAppBar(
    playlistTitle: String, showTitle: Boolean, backgroundColor: Color,
    inSelectMode: Boolean, selectionCount: Int, onExitSelectionMode: () -> Unit,
    isSearching: Boolean, query: TextFieldValue, onQueryChange: (TextFieldValue) -> Unit, focusRequester: FocusRequester,
    onSearchClose: () -> Unit, onSearchConfirmed: () -> Unit,
    onNavIconClick: () -> Unit, onNavIconLongClick: () -> Unit,
    allSelected: Boolean, onSelectAllClick: () -> Unit, onSelectionMenuClick: () -> Unit
) {
    val animatedColor by animateColorAsState(if (showTitle || inSelectMode || isSearching) backgroundColor.copy(alpha = 0.8f) else Color.Transparent, label = "TopBarColor")

    TopAppBar(
        modifier = Modifier.background(animatedColor),
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        title = {
            when {
                inSelectMode -> Text(pluralStringResource(R.plurals.n_selected, selectionCount, selectionCount))
                isSearching -> TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text(stringResource(R.string.search), style = MaterialTheme.typography.titleLarge) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                )
                showTitle -> Text(playlistTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                Icon(painterResource(navIcon), null, tint = MaterialTheme.colorScheme.onSurface)
            }
        },
        actions = {
            when {
                inSelectMode -> {
                    Checkbox(checked = allSelected, onCheckedChange = { onSelectAllClick() })
                    IconButton(enabled = selectionCount > 0, onClick = onSelectionMenuClick) {
                        Icon(painterResource(R.drawable.more_vert), null)
                    }
                }
                !isSearching -> {
                    IconButton(onClick = onSearchConfirmed) { Icon(painterResource(R.drawable.search), null) }
                }
            }
        }
    )
}

@Composable
private fun TopPlaylistScreenSkeleton() {
    ShimmerHost {
        LazyColumn(contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().height(320.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Spacer(Modifier.size(200.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)))
                    Spacer(Modifier.height(16.dp))
                    TextPlaceholder()
                    Spacer(Modifier.height(8.dp))
                    TextPlaceholder()
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Spacer(Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)))
                        Spacer(Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)))
                    }
                }
            }
            items(7) {
                ListItemPlaceHolder(modifier = Modifier.padding(horizontal = 8.dp))
            }
        }
    }
}
