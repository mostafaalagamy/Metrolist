package com.metrolist.music.ui.screens.playlist

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastSumBy
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.pages.PlaylistPage
import com.metrolist.innertube.utils.completed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalDownloadUtil
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.LocalSyncUtils
import com.metrolist.music.R
import com.metrolist.music.constants.AlbumThumbnailSize
import com.metrolist.music.constants.PlaylistEditLockKey
import com.metrolist.music.constants.PlaylistSongSortDescendingKey
import com.metrolist.music.constants.PlaylistSongSortType
import com.metrolist.music.constants.PlaylistSongSortTypeKey
import com.metrolist.music.constants.ThumbnailCornerRadius
import com.metrolist.music.db.entities.Playlist
import com.metrolist.music.db.entities.PlaylistSong
import com.metrolist.music.db.entities.PlaylistSongMap
import com.metrolist.music.extensions.move
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.extensions.togglePlayPause
import com.metrolist.music.playback.ExoDownloadService
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.ui.component.AutoResizeText
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.component.EmptyPlaceholder
import com.metrolist.music.ui.component.FontSizeRange
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.BorderedIconButton
import com.metrolist.music.ui.component.BorderedFloatingActionButton
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.SongListItem
import com.metrolist.music.ui.component.SortHeader
import com.metrolist.music.ui.component.TextFieldDialog
import com.metrolist.music.ui.component.PlaylistThumbnail
import com.metrolist.music.ui.menu.PlaylistMenu
import com.metrolist.music.ui.menu.SelectionSongMenu
import com.metrolist.music.ui.menu.SongMenu
import com.metrolist.music.ui.utils.ItemWrapper
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.ui.utils.adaptiveTopBarColors
import com.metrolist.music.utils.makeTimeString
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.LocalPlaylistViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.time.LocalDateTime

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

@SuppressLint("RememberReturnType")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LocalPlaylistScreen(
    navController: NavController,
    viewModel: LocalPlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.playlistSongs.collectAsState()
    val mutableSongs = remember { mutableStateListOf<PlaylistSong>() }
    val playlistLength =
        remember(songs) {
            songs.fastSumBy { it.song.song.duration }
        }
    val (sortType, onSortTypeChange) = rememberEnumPreference(
        PlaylistSongSortTypeKey,
        PlaylistSongSortType.CUSTOM
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(
        PlaylistSongSortDescendingKey,
        true
    )
    var locked by rememberPreference(PlaylistEditLockKey, defaultValue = true)

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isSearching by rememberSaveable { mutableStateOf(false) }

    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }

    val filteredSongs =
        remember(songs, query) {
            if (query.text.isEmpty()) {
                songs
            } else {
                songs.filter { song ->
                    song.song.song.title
                        .contains(query.text, ignoreCase = true) ||
                            song.song.artists
                                .fastAny { it.name.contains(query.text, ignoreCase = true) }
                }
            }
        }

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

    val wrappedSongs = remember(filteredSongs) {
        filteredSongs.map { item -> ItemWrapper(item) }
    }.toMutableStateList()

    if (isSearching) {
        BackHandler {
            isSearching = false
            query = TextFieldValue()
        }
    }

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember {
        mutableStateOf(Download.STATE_STOPPED)
    }

    val editable: Boolean = playlist?.playlist?.isEditable == true

    LaunchedEffect(songs) {
        mutableSongs.apply {
            clear()
            addAll(songs)
        }
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it.song.id]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songs.all {
                        downloads[it.song.id]?.state == Download.STATE_QUEUED ||
                                downloads[it.song.id]?.state == Download.STATE_DOWNLOADING ||
                                downloads[it.song.id]?.state == Download.STATE_COMPLETED
                    }
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }

    var showEditDialog by remember {
        mutableStateOf(false)
    }

    if (showEditDialog) {
        playlist?.playlist?.let { playlistEntity ->
            TextFieldDialog(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.edit),
                        contentDescription = null
                    )
                },
                title = { Text(text = stringResource(R.string.edit_playlist)) },
                onDismiss = { showEditDialog = false },
                initialTextFieldValue = TextFieldValue(
                    playlistEntity.name,
                    TextRange(playlistEntity.name.length)
                ),
                onDone = { name ->
                    database.query {
                        update(
                            playlistEntity.copy(
                                name = name,
                                lastUpdateTime = LocalDateTime.now()
                            )
                        )
                    }
                    viewModel.viewModelScope.launch(Dispatchers.IO) {
                        playlistEntity.browseId?.let { YouTube.renamePlaylist(it, name) }
                    }
                },
            )
        }
    }

    var showRemoveDownloadDialog by remember {
        mutableStateOf(false)
    }

    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(
                        R.string.remove_download_playlist_confirm,
                        playlist?.playlist!!.name
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                TextButton(
                    onClick = { showRemoveDownloadDialog = false },
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }

                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                        if (!editable) {
                            database.transaction {
                                playlist?.id?.let { clearPlaylist(it) }
                            }
                        }
                        songs.forEach { song ->
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                song.song.id,
                                false
                            )
                        }
                    }
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
        )
    }

    var showDeletePlaylistDialog by remember {
        mutableStateOf(false)
    }
    if (showDeletePlaylistDialog) {
        DefaultDialog(
            onDismiss = { showDeletePlaylistDialog = false },
            content = {
                Text(
                    text = stringResource(
                        R.string.delete_playlist_confirm,
                        playlist?.playlist!!.name
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            },
            buttons = {
                TextButton(
                    onClick = {
                        showDeletePlaylistDialog = false
                    }
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        showDeletePlaylistDialog = false
                        database.query {
                            playlist?.let { delete(it.playlist) }
                        }
                        viewModel.viewModelScope.launch(Dispatchers.IO) {
                            playlist?.playlist?.browseId?.let { YouTube.deletePlaylist(it) }
                        }
                        navController.popBackStack()
                    }
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            }
        )
    }

    val headerItems = 2
    val lazyListState = rememberLazyListState()
    var dragInfo by remember {
        mutableStateOf<Pair<Int, Int>?>(null)
    }
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
        scrollThresholdPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    ) { from, to ->
        if (to.index >= headerItems && from.index >= headerItems) {
            val currentDragInfo = dragInfo
            dragInfo = if (currentDragInfo == null) {
                (from.index - headerItems) to (to.index - headerItems)
            } else {
                currentDragInfo.first to (to.index - headerItems)
            }

            mutableSongs.move(from.index - headerItems, to.index - headerItems)
        }
    }

    LaunchedEffect(reorderableState.isAnyItemDragging) {
        if (!reorderableState.isAnyItemDragging) {
            dragInfo?.let { (from, to) ->
                database.transaction {
                    move(viewModel.playlistId, from, to)
                }
                dragInfo = null
            }
        }
    }

    val showTopBarTitle by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0
        }
    }

    var dismissJob: Job? by remember { mutableStateOf(null) }

    // Dynamic color system
    val defaultColor = MaterialTheme.colorScheme.surface
    var dominantColor by remember { mutableStateOf(defaultColor) }
    val animatedBackgroundColor by animateColorAsState(dominantColor, tween(500), label = "background_color")

    LaunchedEffect(playlist?.thumbnails?.firstOrNull()) {
        dominantColor = fetchDominantColor(context, playlist?.thumbnails?.firstOrNull(), defaultColor)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(
                listOf(animatedBackgroundColor.copy(alpha = 0.4f), MaterialTheme.colorScheme.surface)
            )),
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues(),
        ) {
            playlist?.let { playlist ->
                if (playlist.songCount == 0 && playlist.playlist.remoteSongCount == 0) {
                    item {
                        EmptyPlaceholder(
                            icon = R.drawable.music_note,
                            text = stringResource(R.string.playlist_is_empty),
                        )
                    }
                } else {
                    if (!isSearching) {
                        item {
                            LocalPlaylistHeader(
                                playlist = playlist,
                                songs = songs,
                                onShowEditDialog = { showEditDialog = true },
                                onShowRemoveDownloadDialog = { showRemoveDownloadDialog = true },
                                onshowDeletePlaylistDialog = { showDeletePlaylistDialog = true },
                                snackbarHostState = snackbarHostState,
                                listState = lazyListState,
                                modifier = Modifier.animateItem()
                            )
                        }
                    }

                    if (!isSearching) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 16.dp),
                            ) {
                                SortHeader(
                                    sortType = sortType,
                                    sortDescending = sortDescending,
                                    onSortTypeChange = onSortTypeChange,
                                    onSortDescendingChange = onSortDescendingChange,
                                    sortTypeText = { sortType ->
                                        when (sortType) {
                                            PlaylistSongSortType.CUSTOM -> R.string.sort_by_custom
                                            PlaylistSongSortType.CREATE_DATE -> R.string.sort_by_create_date
                                            PlaylistSongSortType.NAME -> R.string.sort_by_name
                                            PlaylistSongSortType.ARTIST -> R.string.sort_by_artist
                                            PlaylistSongSortType.PLAY_TIME -> R.string.sort_by_play_time
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                                if (editable) {
                                    IconButton(
                                        onClick = { locked = !locked },
                                        modifier = Modifier.padding(horizontal = 6.dp),
                                    ) {
                                        Icon(
                                            painter = painterResource(if (locked) R.drawable.lock else R.drawable.lock_open),
                                            contentDescription = null,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!inSelectMode) {
                itemsIndexed(
                    items = if (isSearching) filteredSongs else mutableSongs,
                    key = { _, song -> song.map.id },
                ) { index, song ->
                    ReorderableItem(
                        state = reorderableState,
                        key = song.map.id
                    ) {
                        val currentItem by rememberUpdatedState(song)

                        fun deleteFromPlaylist() {
                            database.transaction {
                                coroutineScope.launch {
                                    playlist?.playlist?.browseId?.let { it1 ->
                                        var setVideoId = getSetVideoId(currentItem.map.songId)
                                        if (setVideoId?.setVideoId != null) {
                                            YouTube.removeFromPlaylist(
                                                it1, currentItem.map.songId, setVideoId.setVideoId!!
                                            )
                                        }
                                    }
                                }
                                move(
                                    currentItem.map.playlistId,
                                    currentItem.map.position,
                                    Int.MAX_VALUE
                                )
                                delete(currentItem.map.copy(position = Int.MAX_VALUE))
                            }
                            dismissJob?.cancel()
                            dismissJob = coroutineScope.launch {
                                val snackbarResult = snackbarHostState.showSnackbar(
                                    message = context.getString(
                                        R.string.removed_song_from_playlist,
                                        currentItem.song.song.title
                                    ),
                                    actionLabel = context.getString(R.string.undo),
                                    duration = SnackbarDuration.Short
                                )
                                if (snackbarResult == SnackbarResult.ActionPerformed) {
                                    database.transaction {
                                        insert(currentItem.map.copy(position = playlistLength))
                                        move(
                                            currentItem.map.playlistId,
                                            playlistLength,
                                            currentItem.map.position
                                        )
                                    }
                                }
                            }
                        }

                        val dismissBoxState =
                            rememberSwipeToDismissBoxState(
                                positionalThreshold = { totalDistance ->
                                    totalDistance
                                },
                                confirmValueChange = { dismissValue ->
                                    if (dismissValue == SwipeToDismissBoxValue.StartToEnd ||
                                        dismissValue == SwipeToDismissBoxValue.EndToStart
                                    ) {
                                        deleteFromPlaylist()
                                    }
                                    true
                                },
                            )

                        val content: @Composable () -> Unit = {
                            SongListItem(
                                song = song.song,
                                isActive = song.song.id == mediaMetadata?.id,
                                isPlaying = isPlaying,
                                showInLibraryIcon = true,
                                trailingContent = {
                                    IconButton(
                                        onClick = {
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = song.song,
                                                    playlistSong = song,
                                                    playlistBrowseId = playlist?.playlist?.browseId,
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

                                    if (sortType == PlaylistSongSortType.CUSTOM && !locked && !inSelectMode && !isSearching && editable) {
                                        IconButton(
                                            onClick = { },
                                            modifier = Modifier.draggableHandle(),
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.drag_handle),
                                                contentDescription = null,
                                            )
                                        }
                                    }
                                },
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            if (song.song.id == mediaMetadata?.id) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(
                                                    ListQueue(
                                                        title = playlist!!.playlist.name,
                                                        items = songs.map { it.song.toMediaItem() },
                                                        startIndex = songs.indexOfFirst { it.map.id == song.map.id },
                                                    ),
                                                )
                                            }
                                        },
                                        onLongClick = {
                                            if (!inSelectMode) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                inSelectMode = true
                                                selection.add(index)
                                            }
                                        },
                                    ),
                            )
                        }

                        if (locked || inSelectMode || !editable) {
                            content()
                        } else {
                            SwipeToDismissBox(
                                state = dismissBoxState,
                                backgroundContent = {},
                            ) {
                                content()
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = wrappedSongs,
                    key = { _, song -> song.item.map.id },
                ) { index, songWrapper ->
                    ReorderableItem(
                        state = reorderableState,
                        key = songWrapper.item.map.id,
                    ) {
                        val currentItem by rememberUpdatedState(songWrapper.item)

                        fun deleteFromPlaylist() {
                            database.transaction {
                                move(
                                    currentItem.map.playlistId,
                                    currentItem.map.position,
                                    Int.MAX_VALUE
                                )
                                delete(currentItem.map.copy(position = Int.MAX_VALUE))
                            }
                            dismissJob?.cancel()
                            dismissJob = coroutineScope.launch {
                                val snackbarResult = snackbarHostState.showSnackbar(
                                    message = context.getString(
                                        R.string.removed_song_from_playlist,
                                        currentItem.song.song.title
                                    ),
                                    actionLabel = context.getString(R.string.undo),
                                    duration = SnackbarDuration.Short
                                )
                                if (snackbarResult == SnackbarResult.ActionPerformed) {
                                    database.transaction {
                                        insert(currentItem.map.copy(position = playlistLength))
                                        move(
                                            currentItem.map.playlistId,
                                            playlistLength,
                                            currentItem.map.position
                                        )
                                    }
                                }
                            }
                        }

                        val dismissBoxState =
                            rememberSwipeToDismissBoxState(
                                positionalThreshold = { totalDistance ->
                                    totalDistance
                                },
                                confirmValueChange = { dismissValue ->
                                    if (dismissValue == SwipeToDismissBoxValue.StartToEnd ||
                                        dismissValue == SwipeToDismissBoxValue.EndToStart
                                    ) {
                                        deleteFromPlaylist()
                                    }
                                    true
                                },
                            )

                        val onCheckedChange: (Boolean) -> Unit = { checked ->
                            if (checked) selection.add(index) else selection.remove(index)
                        }
                        val content: @Composable () -> Unit = {
                            SongListItem(
                                song = songWrapper.item.song,
                                isActive = songWrapper.item.song.id == mediaMetadata?.id,
                                isPlaying = isPlaying,
                                showInLibraryIcon = true,

                                trailingContent = {
                                    if (inSelectMode) {
                                        Checkbox(checked = index in selection, onCheckedChange = onCheckedChange)
                                    } else {
                                    IconButton(
                                        onClick = {
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = songWrapper.item.song,
                                                    playlistBrowseId = playlist?.playlist?.browseId,
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
                                        if (sortType == PlaylistSongSortType.CUSTOM && !locked && !inSelectMode && !isSearching && editable) {
                                            IconButton(
                                                onClick = { },
                                                modifier = Modifier.draggableHandle(),
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.drag_handle),
                                                    contentDescription = null,
                                                )
                                            }
                                        }
                                    }
                                },
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            if (inSelectMode) {
                                                onCheckedChange(index !in selection)
                                            } else if (songWrapper.item.song.id == mediaMetadata?.id) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(
                                                    ListQueue(
                                                        title = playlist!!.playlist.name,
                                                        items = songs.map { it.song.toMediaItem() },
                                                        startIndex = index,
                                                    ),
                                                )
                                            }
                                        },
                                        onLongClick = {
                                            if (!inSelectMode) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                inSelectMode = true
                                                onCheckedChange(true)
                                            }
                                        },
                                    ),
                            )
                        }

                        if (locked || !editable) {
                            content()
                        } else {
                            SwipeToDismissBox(
                                state = dismissBoxState,
                                backgroundContent = {},
                            ) {
                                content()
                            }
                        }
                    }
                }
            }
        }

        val topBarAnimatedColor by animateColorAsState(
            if (showTopBarTitle || inSelectMode || isSearching) animatedBackgroundColor.copy(alpha = 0.8f) else Color.Transparent,
            label = "TopBarColor"
        )

        // Calculate adaptive colors based on the background color using Player.kt logic
        val adaptiveColors = adaptiveTopBarColors(topBarAnimatedColor)

        TopAppBar(
            modifier = Modifier.background(topBarAnimatedColor),
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            title = {
                if (inSelectMode) {
                    Text(
                        text = pluralStringResource(R.plurals.n_selected, selection.size, selection.size),
                        style = MaterialTheme.typography.titleLarge,
                        color = adaptiveColors.titleColor
                    )
                } else if (isSearching) {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
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
                            disabledIndicatorColor = Color.Transparent,
                            focusedTextColor = adaptiveColors.titleColor,
                            unfocusedTextColor = adaptiveColors.titleColor,
                            cursorColor = adaptiveColors.actionColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                } else if (showTopBarTitle) {
                    Text(
                        text = playlist?.playlist?.name.orEmpty(),
                        color = adaptiveColors.titleColor
                    )
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        if (isSearching) {
                            isSearching = false
                            query = TextFieldValue()
                        } else if (inSelectMode) {
                            onExitSelectionMode()
                        } else {
                            navController.navigateUp()
                        }
                    },
                    onLongClick = {
                        if (!isSearching) {
                            navController.backToMain()
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(
                            if (inSelectMode || isSearching) R.drawable.close else R.drawable.arrow_back
                        ),
                        contentDescription = null,
                        tint = adaptiveColors.iconColor
                    )
                }
            },
            actions = {
                if (inSelectMode) {
                    val allSelected = selection.size == filteredSongs.size && selection.isNotEmpty()
                    Checkbox(
                        checked = allSelected,
                        onCheckedChange = {
                            if (allSelected) {
                                selection.clear()
                            } else {
                                selection.clear()
                                selection.addAll(filteredSongs.indices)
                            }
                        }
                    )

                    IconButton(
                        onClick = {
                            menuState.show {
                                SelectionSongMenu(
                                    songSelection = selection.mapNotNull { index ->
                                        filteredSongs.getOrNull(index)?.song
                                    },
                                    songPosition = selection.mapNotNull { index ->
                                        filteredSongs.getOrNull(index)?.map
                                    },
                                    onDismiss = menuState::dismiss,
                                    clearAction = {
                                        onExitSelectionMode()
                                    }
                                )
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = null,
                            tint = adaptiveColors.iconColor
                        )
                    }
                } else if (!isSearching) {
                    IconButton(
                        onClick = { isSearching = true }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = null,
                            tint = adaptiveColors.iconColor
                        )
                    }
                }
            }
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier =
            Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime))
                .align(Alignment.BottomCenter),
        )
    }
}

// New UI auxiliary components for Local Playlist

@Composable
fun LocalPlaylistHeader(
    playlist: Playlist,
    songs: List<PlaylistSong>,
    onShowEditDialog: () -> Unit,
    onShowRemoveDownloadDialog: () -> Unit,
    onshowDeletePlaylistDialog: () -> Unit,
    snackbarHostState: SnackbarHostState,
    listState: LazyListState,
    modifier: Modifier
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val database = LocalDatabase.current
    val syncUtils = LocalSyncUtils.current
    val scope = rememberCoroutineScope()

    val playlistLength = remember(songs) { songs.fastSumBy { it.song.song.duration } }
    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember { mutableIntStateOf(Download.STATE_STOPPED) }
    val liked = playlist.playlist.bookmarkedAt != null
    val editable: Boolean = playlist.playlist.isEditable == true

    LaunchedEffect(songs) {
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState = if (songs.all { downloads[it.song.id]?.state == Download.STATE_COMPLETED }) {
                Download.STATE_COMPLETED
            } else if (songs.all {
                    downloads[it.song.id]?.state == Download.STATE_QUEUED ||
                            downloads[it.song.id]?.state == Download.STATE_DOWNLOADING ||
                            downloads[it.song.id]?.state == Download.STATE_COMPLETED
                }
            ) {
                Download.STATE_DOWNLOADING
            } else {
                Download.STATE_STOPPED
            }
        }
    }

    val scrollOffset = if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset.toFloat() else Float.MAX_VALUE
    val headerHeightPx = with(LocalDensity.current) { 320.dp.toPx() }
    val imageTranslationY = -scrollOffset * 0.2f
    val textAlpha = (1f - (scrollOffset / (headerHeightPx / 2))).coerceIn(0f, 1f)

    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .graphicsLayer { translationY = imageTranslationY },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PlaylistThumbnail(
                thumbnails = playlist.thumbnails,
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
                Text(
                    text = playlist.playlist.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val songCountText = if (playlist.songCount == 0 && playlist.playlist.remoteSongCount != null) {
                    pluralStringResource(R.plurals.n_song, playlist.playlist.remoteSongCount, playlist.playlist.remoteSongCount)
                } else {
                    pluralStringResource(R.plurals.n_song, playlist.songCount, playlist.songCount)
                }
                Text(
                    text = songCountText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Action Controls
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side - action buttons
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (editable) {
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
                                .background(color = Color.Transparent, shape = CircleShape)
                        ) {
                            IconButton(
                                onClick = onshowDeletePlaylistDialog,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.delete),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 1.dp,
                                    color = if (liked)
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                    else
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .background(
                                    color = if (liked)
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                                    else
                                        Color.Transparent,
                                    shape = CircleShape
                                )
                        ) {
                            IconButton(
                                onClick = {
                                    database.transaction {
                                        update(playlist.playlist.toggleLike())
                                    }
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    painter = painterResource(if (liked) R.drawable.favorite else R.drawable.favorite_border),
                                    contentDescription = null,
                                    tint = if (liked) MaterialTheme.colorScheme.error else LocalContentColor.current,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    if (editable) {
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
                                .background(color = Color.Transparent, shape = CircleShape)
                        ) {
                            IconButton(
                                onClick = onShowEditDialog,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.edit),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    if (playlist.playlist.browseId != null) {
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
                                .background(color = Color.Transparent, shape = CircleShape)
                        ) {
                            IconButton(
                                onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        val playlistPage = YouTube.playlist(playlist.playlist.browseId!!)
                                            .completed()
                                            .getOrNull() ?: return@launch
                                        database.transaction {
                                            clearPlaylist(playlist.id)
                                            playlistPage.songs
                                                .map(SongItem::toMediaMetadata)
                                                .onEach(::insert)
                                                .mapIndexed { position, song ->
                                                    PlaylistSongMap(
                                                        songId = song.id,
                                                        playlistId = playlist.id,
                                                        position = position
                                                    )
                                                }
                                                .forEach(::insert)
                                        }
                                    }
                                    scope.launch(Dispatchers.Main) {
                                        snackbarHostState.showSnackbar(context.getString(R.string.playlist_synced))
                                    }
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.sync),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    when (downloadState) {
                        Download.STATE_COMPLETED -> {
                            Box(
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
                                    onClick = onShowRemoveDownloadDialog,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.offline),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Download.STATE_DOWNLOADING -> {
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
                            ) {
                                IconButton(
                                    onClick = {
                                        songs.forEach { song ->
                                            DownloadService.sendRemoveDownload(
                                                context,
                                                ExoDownloadService::class.java,
                                                song.song.id,
                                                false,
                                            )
                                        }
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }

                        else -> {
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
                                    .background(color = Color.Transparent, shape = CircleShape)
                            ) {
                                IconButton(
                                    onClick = {
                                        songs.forEach { song ->
                                            val downloadRequest = DownloadRequest
                                                .Builder(song.song.id, song.song.id.toUri())
                                                .setCustomCacheKey(song.song.id)
                                                .setData(song.song.song.title.toByteArray()).build()
                                            DownloadService.sendAddDownload(
                                                context,
                                                ExoDownloadService::class.java,
                                                downloadRequest,
                                                false,
                                            )
                                        }
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.download),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
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
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                            .background(color = Color.Transparent, shape = CircleShape)
                    ) {
                        IconButton(
                            onClick = {
                                playerConnection.addToQueue(items = songs.map { it.song.toMediaItem() })
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.queue_music),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Add space between left and right controls
                Spacer(modifier = Modifier.weight(0.5f))

                // Right side - circular shuffle and play buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FloatingActionButton(
                        onClick = {
                            playerConnection.playQueue(
                                ListQueue(
                                    title = playlist.playlist.name,
                                    items = songs.shuffled().map { it.song.toMediaItem() },
                                ),
                            )
                        },
                        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(painterResource(R.drawable.shuffle), "Shuffle")
                    }
                    Spacer(Modifier.width(12.dp))
                    FloatingActionButton(
                        onClick = {
                            playerConnection.playQueue(
                                ListQueue(
                                    title = playlist.playlist.name,
                                    items = songs.map { it.song.toMediaItem() },
                                ),
                            )
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(painterResource(R.drawable.play), "Play")
                    }
                }
            }
        }
    }
}
