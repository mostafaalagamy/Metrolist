package com.metrolist.music.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
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
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalDownloadUtil
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.db.entities.Album
import com.metrolist.music.extensions.togglePlayPause
import com.metrolist.music.playback.ExoDownloadService
import com.metrolist.music.playback.queues.LocalAlbumRadio
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.BorderedIconButton
import com.metrolist.music.ui.component.BorderedFloatingActionButton
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.NavigationTitle
import com.metrolist.music.ui.component.SongListItem
import com.metrolist.music.ui.component.YouTubeGridItem
import com.metrolist.music.ui.component.shimmer.ListItemPlaceHolder
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.ui.component.shimmer.TextPlaceholder
import com.metrolist.music.ui.menu.AlbumMenu
import com.metrolist.music.ui.menu.SelectionSongMenu
import com.metrolist.music.ui.menu.SongMenu
import com.metrolist.music.ui.menu.YouTubeAlbumMenu
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.ui.utils.ItemWrapper
import com.metrolist.music.ui.utils.adaptiveTopBarColors
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.AlbumViewModel
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
fun AlbumScreen(
    navController: NavController,
    viewModel: AlbumViewModel = hiltViewModel(),
) {
    // --- ORIGINAL LOGIC (INTACT) ---
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return

    val scope = rememberCoroutineScope()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val playlistId by viewModel.playlistId.collectAsState()
    val albumWithSongs by viewModel.albumWithSongs.collectAsState()
    val otherVersions by viewModel.otherVersions.collectAsState()
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)

    val wrappedSongs = remember(albumWithSongs, hideExplicit) {
        val filteredSongs = if (hideExplicit) {
            albumWithSongs?.songs?.filter { !it.song.explicit } ?: emptyList()
        } else {
            albumWithSongs?.songs ?: emptyList()
        }
        filteredSongs.map { item -> ItemWrapper(item) }.toMutableStateList()
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

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember { mutableStateOf(Download.STATE_STOPPED) }

    LaunchedEffect(albumWithSongs) {
        val songs = albumWithSongs?.songs?.map { it.id }
        if (songs.isNullOrEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState = if (songs.all { downloads[it]?.state == Download.STATE_COMPLETED }) {
                Download.STATE_COMPLETED
            } else if (songs.all {
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

    // --- NEW UI STATES ---
    val defaultColor = MaterialTheme.colorScheme.surface
    var dominantColor by remember { mutableStateOf(defaultColor) }
    val animatedBackgroundColor by animateColorAsState(dominantColor, tween(500), label = "background_color")
    
    LaunchedEffect(albumWithSongs?.album?.thumbnailUrl) {
        albumWithSongs?.album?.thumbnailUrl?.let { thumbnailUrl ->
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
        if (albumWithSongs == null || albumWithSongs!!.songs.isEmpty()) {
            AlbumScreenSkeleton()
        } else {
            val albumData = albumWithSongs!!
            LazyColumn(
                state = lazyListState,
                contentPadding = LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues()
            ) {
                // Header
                item {
                    AlbumHeader(
                        albumEntity = albumData.album,
                        artists = albumData.artists,
                        navController = navController,
                        listState = lazyListState
                    )
                }

                // Action Controls
                item {
                    AlbumActionControls(
                        downloadState = downloadState,
                        isLiked = albumData.album.bookmarkedAt != null,
                        onPlayClick = {
                            playerConnection.service.getAutomix(playlistId)
                            playerConnection.playQueue(LocalAlbumRadio(albumData))
                        },
                        onShuffleClick = {
                            playerConnection.service.getAutomix(playlistId)
                            playerConnection.playQueue(LocalAlbumRadio(albumData.copy(songs = albumData.songs.shuffled())))
                        },
                        onRadioClick = {
                            // Radio functionality using LocalAlbumRadio
                            playerConnection.service.getAutomix(playlistId)
                            playerConnection.playQueue(LocalAlbumRadio(albumData.copy(songs = albumData.songs.shuffled())))
                        },
                        onLikeClick = {
                            database.query { update(albumData.album.toggleLike()) }
                        },
                        onDownloadClick = {
                            albumData.songs.forEach { song ->
                                val request = DownloadRequest.Builder(song.id, song.id.toUri())
                                    .setCustomCacheKey(song.id)
                                    .setData(song.song.title.toByteArray())
                                    .build()
                                DownloadService.sendAddDownload(context, ExoDownloadService::class.java, request, false)
                            }
                        },
                        onRemoveDownloadClick = {
                            albumData.songs.forEach { song ->
                                DownloadService.sendRemoveDownload(context, ExoDownloadService::class.java, song.id, false)
                            }
                        },
                        onMenuClick = {
                            menuState.show {
                                AlbumMenu(
                                    originalAlbum = Album(albumData.album, albumData.artists),
                                    navController = navController,
                                    onDismiss = menuState::dismiss
                                )
                            }
                        }
                    )
                }

                // Songs list
                if (wrappedSongs.isNotEmpty()) {
                    itemsIndexed(items = wrappedSongs, key = { _, song -> song.item.id }) { index, songWrapper ->
                        val onCheckedChange: (Boolean) -> Unit = { checked -> 
                            if (checked) selection.add(index) else selection.remove(index) 
                        }
                        SongListItem(
                            song = songWrapper.item,
                            albumIndex = index + 1,
                            isActive = songWrapper.item.id == mediaMetadata?.id,
                            isPlaying = isPlaying,
                            showInLibraryIcon = true,
                            trailingContent = {
                                if (inSelectMode) {
                                    Checkbox(checked = index in selection, onCheckedChange = onCheckedChange)
                                } else {
                                    IconButton(onClick = {
                                        menuState.show {
                                            SongMenu(
                                                originalSong = songWrapper.item,
                                                navController = navController,
                                                onDismiss = menuState::dismiss,
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
                                        if (inSelectMode) onCheckedChange(index !in selection)
                                        else if (songWrapper.item.id == mediaMetadata?.id) playerConnection.player.togglePlayPause()
                                        else {
                                            playerConnection.service.getAutomix(playlistId)
                                            playerConnection.playQueue(LocalAlbumRadio(albumData, startIndex = index))
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
                        )
                    }
                }

                // Other versions
                if (otherVersions.isNotEmpty()) {
                    item {
                        NavigationTitle(title = stringResource(R.string.other_versions))
                    }
                    item {
                        LazyRow {
                            items(items = otherVersions, key = { it.id }) { item ->
                                YouTubeGridItem(
                                    item = item,
                                    isActive = mediaMetadata?.album?.id == item.id,
                                    isPlaying = isPlaying,
                                    coroutineScope = scope,
                                    modifier = Modifier
                                        .combinedClickable(
                                            onClick = { navController.navigate("album/${item.id}") },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    YouTubeAlbumMenu(
                                                        albumItem = item,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        )
                                        .animateItem()
                                )
                            }
                        }
                    }
                }
            }
        }

        // Custom collapsing top bar
        AlbumCollapsingTopAppBar(
            albumTitle = albumWithSongs?.album?.title.orEmpty(),
            showTitle = showTopBarTitle,
            backgroundColor = animatedBackgroundColor,
            inSelectMode = inSelectMode,
            selectionCount = selection.size,
            onExitSelectionMode = onExitSelectionMode,
            onNavIconClick = { if (inSelectMode) onExitSelectionMode() else navController.navigateUp() },
            onNavIconLongClick = { if (!inSelectMode) navController.backToMain() },
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
                        songSelection = selection.mapNotNull { wrappedSongs.getOrNull(it)?.item },
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
private fun AlbumHeader(
    albumEntity: com.metrolist.music.db.entities.AlbumEntity,
    artists: List<com.metrolist.music.db.entities.ArtistEntity>,
    navController: NavController,
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
        AsyncImage(
            model = albumEntity.thumbnailUrl,
            contentDescription = "Album Art",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(200.dp).clip(RoundedCornerShape(12.dp)).shadow(16.dp, RoundedCornerShape(12.dp))
        )
        Spacer(Modifier.height(16.dp))
        Column(
            modifier = Modifier.graphicsLayer { alpha = textAlpha },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = albumEntity.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = buildAnnotatedString {
                    artists.fastForEachIndexed { index, artist ->
                        val link = LinkAnnotation.Clickable(artist.id) { navController.navigate("artist/${artist.id}") }
                        withLink(link) {
                            withStyle(style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.primary).toSpanStyle()) {
                                append(artist.name)
                            }
                        }
                        if (index < artists.lastIndex) append(", ")
                    }
                },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            albumEntity.year?.let {
                Text(
                    text = it.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AlbumActionControls(
    downloadState: Int,
    isLiked: Boolean,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onRadioClick: () -> Unit,
    onLikeClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onRemoveDownloadClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left side - more, download and like buttons (reversed order)
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
                        painter = painterResource(R.drawable.more_vert),
                        contentDescription = "More options",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            when (downloadState) {
                Download.STATE_COMPLETED -> Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = CircleShape
                        )
                ) {
                    IconButton(
                        onClick = onRemoveDownloadClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.offline),
                            contentDescription = "Downloaded",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Download.STATE_DOWNLOADING -> Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                ) {
                    IconButton(
                        onClick = onRemoveDownloadClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                else -> Box(
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
                        onClick = onDownloadClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.download),
                            contentDescription = "Download",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(
                        width = 1.dp,
                        color = if (isLiked)
                            MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
                    .background(
                        color = if (isLiked)
                            MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        else
                            Color.Transparent,
                        shape = CircleShape
                    )
            ) {
                IconButton(
                    onClick = onLikeClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(if (isLiked) R.drawable.favorite else R.drawable.favorite_border),
                        contentDescription = "Like",
                        tint = if (isLiked) MaterialTheme.colorScheme.error else LocalContentColor.current,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Right side - circular radio, shuffle and play buttons
        Row(verticalAlignment = Alignment.CenterVertically) {
            FloatingActionButton(
                onClick = onRadioClick,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(painterResource(R.drawable.radio), "Radio")
            }
            Spacer(Modifier.width(8.dp))
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
private fun AlbumCollapsingTopAppBar(
    albumTitle: String, showTitle: Boolean, backgroundColor: Color,
    inSelectMode: Boolean, selectionCount: Int, onExitSelectionMode: () -> Unit,
    onNavIconClick: () -> Unit, onNavIconLongClick: () -> Unit,
    allSelected: Boolean, onSelectAllClick: () -> Unit, onSelectionMenuClick: () -> Unit
) {
    val animatedColor by animateColorAsState(if (showTitle || inSelectMode) backgroundColor.copy(alpha = 0.8f) else Color.Transparent, label = "TopBarColor")
    
    // Calculate adaptive colors based on the background color using Player.kt logic
    val adaptiveColors = adaptiveTopBarColors(animatedColor)

    TopAppBar(
        modifier = Modifier.background(animatedColor),
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        title = {
            if (inSelectMode) {
                Text(
                    text = pluralStringResource(R.plurals.n_selected, selectionCount, selectionCount),
                    color = adaptiveColors.titleColor
                )
            } else if (showTitle) {
                Text(
                    text = albumTitle, 
                    maxLines = 1, 
                    overflow = TextOverflow.Ellipsis,
                    color = adaptiveColors.titleColor
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavIconClick, onLongClick = onNavIconLongClick) {
                Icon(
                    painter = painterResource(if (inSelectMode) R.drawable.close else R.drawable.arrow_back), 
                    contentDescription = null,
                    tint = adaptiveColors.iconColor
                )
            }
        },
        actions = {
            if (inSelectMode) {
                Checkbox(checked = allSelected, onCheckedChange = { onSelectAllClick() })
                IconButton(enabled = selectionCount > 0, onClick = onSelectionMenuClick) {
                    Icon(
                        painter = painterResource(R.drawable.more_vert), 
                        contentDescription = null,
                        tint = adaptiveColors.iconColor
                    )
                }
            }
        }
    )
}

@Composable
private fun AlbumScreenSkeleton() {
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
                        repeat(3) { Spacer(Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)))
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
