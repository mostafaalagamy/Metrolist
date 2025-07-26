package com.metrolist.music.ui.screens.playlist

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import androidx.compose.ui.layout.ContentScale
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.AlbumThumbnailSize
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.ThumbnailCornerRadius
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.db.entities.PlaylistSongMap
import com.metrolist.music.extensions.metadata
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.extensions.togglePlayPause
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.AutoResizeText
import com.metrolist.music.ui.component.FontSizeRange
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.BorderedIconButton
import com.metrolist.music.ui.component.BorderedFloatingActionButton
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.NavigationTitle
import com.metrolist.music.ui.component.YouTubeListItem
import com.metrolist.music.ui.component.PlaylistThumbnail
import com.metrolist.music.ui.component.shimmer.ListItemPlaceHolder
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.ui.component.shimmer.TextPlaceholder
import com.metrolist.music.ui.menu.YouTubePlaylistMenu
import com.metrolist.music.ui.menu.YouTubeSongMenu
import com.metrolist.music.ui.menu.SelectionMediaMetadataMenu
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.ui.utils.adaptiveTopBarColors
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.OnlinePlaylistViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Helper function to asynchronously extract the dominant color from an image URL.
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OnlinePlaylistScreen(
    navController: NavController,
    viewModel: OnlinePlaylistViewModel = hiltViewModel(),
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.playlistSongs.collectAsState()
    val dbPlaylist by viewModel.dbPlaylist.collectAsState()
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(lazyListState) {
        snapshotFlow {
            val layoutInfo = lazyListState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1

            lastVisibleItemIndex > (totalItemsNumber - 5)
        }.collect { shouldLoadMore ->
            if (shouldLoadMore && viewModel.continuation != null) {
                viewModel.loadMoreSongs()
            }
        }
    }

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    val filteredSongs = remember(songs, query) {
        if (query.text.isEmpty()) songs.mapIndexed { index, song -> index to song }
        else songs.mapIndexed { index, song -> index to song }.filter { (_, song) ->
            song.title.contains(query.text, ignoreCase = true) ||
                    song.artists.fastAny { it.name.contains(query.text, ignoreCase = true) }
        }
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearching) { if (isSearching) focusRequester.requestFocus() }
    if (isSearching) {
        BackHandler { isSearching = false; query = TextFieldValue() }
    }
    var inSelectMode by rememberSaveable { mutableStateOf(false) }
    val selection = rememberSaveable(
        saver = listSaver<MutableList<Int>, Int>(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) { mutableStateListOf() }
    val onExitSelectionMode = { inSelectMode = false; selection.clear() }
    if (inSelectMode) { BackHandler(onBack = onExitSelectionMode) }

    // UI states for new design
    val defaultColor = MaterialTheme.colorScheme.surface
    var dominantColor by remember { mutableStateOf(defaultColor) }
    val animatedBackgroundColor by animateColorAsState(dominantColor, tween(500), label = "background_color")
    LaunchedEffect(playlist?.thumbnail) {
        dominantColor = fetchDominantColor(context, playlist?.thumbnail, defaultColor)
    }
    val showTopBarTitle by remember { derivedStateOf { lazyListState.firstVisibleItemIndex > 0 } }

    // New visual structure
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(animatedBackgroundColor.copy(alpha = 0.4f), MaterialTheme.colorScheme.surface)))
    ) {
        // Show skeleton or actual content
        if (playlist == null) {
            OnlinePlaylistScreenSkeleton()
        } else {
            val playlistData = playlist!!
            LazyColumn(
                state = lazyListState,
                contentPadding = LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues()
            ) {
                // Header (only shown if not searching)
                if (!isSearching) {
                    item {
                        PlaylistHeader(playlist = playlistData, listState = lazyListState, navController = navController)
                    }
                    item {
                        PlaylistActionControls(
                            playlist = playlistData,
                            onShuffleClick = { playlistData.shuffleEndpoint?.let { playerConnection.playQueue(YouTubeQueue(it)) } },
                            onRadioClick = { playlistData.radioEndpoint?.let { playerConnection.playQueue(YouTubeQueue(it)) } },
                            onFavoriteClick = {
                                database.transaction {
                                    if (dbPlaylist?.playlist?.bookmarkedAt != null) {
                                        dbPlaylist?.let {
                                            update(it.playlist.toggleLike())
                                        }
                                    } else {
                                        val existingPlaylist = dbPlaylist?.playlist
                                        if (existingPlaylist != null) {
                                            update(existingPlaylist.toggleLike())
                                        } else {
                                            val playlistEntity = PlaylistEntity(
                                                name = playlistData.title,
                                                browseId = playlistData.id,
                                                isEditable = playlistData.isEditable,
                                                playEndpointParams = playlistData.playEndpoint?.params,
                                                shuffleEndpointParams = playlistData.shuffleEndpoint?.params,
                                                radioEndpointParams = playlistData.radioEndpoint?.params
                                            ).toggleLike()
                                            insert(playlistEntity)
                                            songs.map(SongItem::toMediaMetadata).onEach(::insert).mapIndexed { index, song ->
                                                PlaylistSongMap(songId = song.id, playlistId = playlistEntity.id, position = index)
                                            }.forEach(::insert)
                                        }
                                    }
                                }
                            },
                            isFavorited = dbPlaylist?.playlist?.bookmarkedAt != null,
                            onMenuClick = {
                                menuState.show {
                                    YouTubePlaylistMenu(
                                        playlist = playlistData,
                                        songs = songs,
                                        coroutineScope = coroutineScope,
                                        onDismiss = menuState::dismiss,
                                        selectAction = { inSelectMode = true },
                                        canSelect = true,
                                    )
                                }
                            }
                        )
                    }
                }
                // Song list
                items(items = filteredSongs, key = { (index, _) -> index }) { (index, song) ->
                    val onCheckedChange: (Boolean) -> Unit = { if (it) selection.add(index) else selection.remove(index) }
                    YouTubeListItem(
                        item = song,
                        isActive = mediaMetadata?.id == song.id,
                        isPlaying = isPlaying,
                        isSelected = false,
                        trailingContent = {
                            if (inSelectMode) {
                                Checkbox(checked = index in selection, onCheckedChange = onCheckedChange)
                            } else {
                                IconButton(onClick = { menuState.show { YouTubeSongMenu(song = song, navController = navController, onDismiss = menuState::dismiss) } }) {
                                    Icon(painterResource(R.drawable.more_vert), null)
                                }
                            }
                        },
                        modifier = Modifier
                            .combinedClickable(
                                enabled = !hideExplicit || !song.explicit,
                                onClick = {
                                    if (inSelectMode) onCheckedChange(index !in selection)
                                    else if (song.id == mediaMetadata?.id) playerConnection.player.togglePlayPause()
                                    else {
                                        playerConnection.service.getAutomix(playlistId = playlistData.id)
                                        playerConnection.playQueue(YouTubeQueue(song.endpoint ?: WatchEndpoint(videoId = song.id), song.toMediaMetadata()))
                                    }
                                },
                                onLongClick = {
                                    if (!inSelectMode) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        inSelectMode = true
                                        onCheckedChange(true)
                                    }
                                }
                            )
                            .alpha(if (hideExplicit && song.explicit) 0.3f else 1f)
                            .animateItem()
                    )
                }

                // Loading indicator for infinite scroll
                if (viewModel.continuation != null) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }
        }

        // Custom collapsing top app bar
        PlaylistCollapsingTopAppBar(
            playlistTitle = playlist?.title.orEmpty(),
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
            allSelected = selection.size == songs.size,
            onSelectAllClick = {
                if (selection.size == songs.size) selection.clear()
                else {
                    selection.clear()
                    selection.addAll(songs.mapIndexedNotNull { index, song -> if (hideExplicit && song.explicit) null else index })
                }
            },
            onSelectionMenuClick = {
                menuState.show {
                    SelectionMediaMetadataMenu(
                        songSelection = selection.mapNotNull { songs.getOrNull(it)?.toMediaItem()?.metadata },
                        onDismiss = menuState::dismiss,
                        clearAction = onExitSelectionMode,
                        currentItems = emptyList()
                    )
                }
            }
        )

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime))
                .align(Alignment.BottomCenter)
        )
    }
}

// New UI auxiliary components

@Composable
private fun PlaylistHeader(
    playlist: com.metrolist.innertube.models.PlaylistItem,
    listState: LazyListState,
    navController: NavController
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
            thumbnails = listOfNotNull(playlist.thumbnail),
            size = 200.dp,
            placeHolder = {
                Icon(
                    painter = painterResource(R.drawable.queue_music),
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
            Text(playlist.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            playlist.author?.let { author ->
                Text(
                    text = buildAnnotatedString {
                        if (author.id != null) {
                            withLink(LinkAnnotation.Clickable(author.id!!) { navController.navigate("artist/${author.id}") }) {
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append(author.name) }
                            }
                        } else {
                            append(author.name)
                        }
                    },
                    style = MaterialTheme.typography.bodyLarge, maxLines = 1
                )
            }
            playlist.songCountText?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PlaylistActionControls(
    playlist: com.metrolist.innertube.models.PlaylistItem,
    onShuffleClick: () -> Unit,
    onRadioClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onMenuClick: () -> Unit,
    isFavorited: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left side: Menu and Import buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
                    .background(
                        color = Color.Transparent,
                        shape = CircleShape
                    )
            ) {
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painterResource(R.drawable.more_vert),
                        "Menu",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(
                        width = 1.dp,
                        color = if (isFavorited)
                            MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
                    .background(
                        color = if (isFavorited)
                            MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        else
                            Color.Transparent,
                        shape = CircleShape
                    )
            ) {
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painterResource(if (isFavorited) R.drawable.favorite else R.drawable.favorite_border),
                        "Favorite",
                        tint = if (isFavorited) MaterialTheme.colorScheme.error else LocalContentColor.current,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Right side - action buttons
        Row(verticalAlignment = Alignment.CenterVertically) {
            playlist.radioEndpoint?.let { radioEndpoint ->
                FloatingActionButton(
                    onClick = onRadioClick,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp),
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(painterResource(R.drawable.radio), "Radio")
                }
                Spacer(Modifier.width(8.dp))
            }

            FloatingActionButton(
                onClick = onShuffleClick,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(painterResource(R.drawable.shuffle), "Shuffle")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistCollapsingTopAppBar(
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

@Composable
private fun OnlinePlaylistScreenSkeleton() {
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
                    // Left side: Menu and Import buttons (larger buttons)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)))
                        Spacer(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)))
                    }
                    // Right side: Radio and Shuffle buttons (smaller buttons)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Spacer(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)))
                        Spacer(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)))
                    }
                }
            }
            items(7) {
                ListItemPlaceHolder(modifier = Modifier.padding(horizontal = 8.dp))
            }
        }
    }
}
