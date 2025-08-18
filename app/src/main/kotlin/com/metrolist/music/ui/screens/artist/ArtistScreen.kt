package com.metrolist.music.ui.screens.artist

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.util.Logger
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.AppBarHeight
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.db.entities.ArtistEntity
import com.metrolist.music.extensions.togglePlayPause
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.AlbumGridItem
import com.metrolist.music.ui.component.AutoResizeText
import com.metrolist.music.ui.component.FontSizeRange
import com.metrolist.music.ui.component.HideOnScrollFAB
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.NavigationTitle
import com.metrolist.music.ui.component.SongListItem
import com.metrolist.music.ui.component.YouTubeGridItem
import com.metrolist.music.ui.component.YouTubeListItem
import com.metrolist.music.ui.component.shimmer.ButtonPlaceholder
import com.metrolist.music.ui.component.shimmer.ListItemPlaceHolder
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.ui.component.shimmer.TextPlaceholder
import com.metrolist.music.ui.menu.AlbumMenu
import com.metrolist.music.ui.menu.SongMenu
import com.metrolist.music.ui.menu.YouTubeAlbumMenu
import com.metrolist.music.ui.menu.YouTubeArtistMenu
import com.metrolist.music.ui.menu.YouTubePlaylistMenu
import com.metrolist.music.ui.menu.YouTubeSongMenu
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.ui.utils.fadingEdge
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.ui.utils.resize
import com.metrolist.music.viewmodels.ArtistViewModel
import com.valentinilk.shimmer.shimmer
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ArtistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: ArtistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val artistPage = viewModel.artistPage
    val libraryArtist by viewModel.libraryArtist.collectAsState()
    val librarySongs by viewModel.librarySongs.collectAsState()
    val libraryAlbums by viewModel.libraryAlbums.collectAsState()
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)

    val lazyListState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLocal by rememberSaveable { mutableStateOf(false) }
    val density = LocalDensity.current

    // Calculate the offset value outside of the offset lambda
    val systemBarsTopPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    val headerOffset = with(density) {
        -(systemBarsTopPadding + AppBarHeight).roundToPx()
    }

    val transparentAppBar by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset < 100
        }
    }

    LaunchedEffect(libraryArtist) {
        // always show local page for local artists. Show local page remote artist when offline
        showLocal = libraryArtist?.artist?.isLocal == true
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        ) {
            if (artistPage == null && !showLocal) {
                item(key = "shimmer") {
                    ShimmerHost (
                        modifier = Modifier
                            .offset {
                                IntOffset(x = 0, y = headerOffset)
                            }
                    ) {
                        // Artist Image Placeholder
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.1f),
                        ) {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .shimmer()
                                    .background(MaterialTheme.colorScheme.onSurface)
                                    .fadingEdge(
                                        top = systemBarsTopPadding + AppBarHeight,
                                        bottom = 200.dp,
                                    ),
                            )
                        }
                        // Artist Name and Controls Section
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Artist Name Placeholder
                            TextPlaceholder(
                                height = 36.dp,
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .padding(bottom = 16.dp)
                            )

                            // Buttons Row Placeholder
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Subscribe Button Placeholder
                                ButtonPlaceholder(
                                    modifier = Modifier
                                        .width(120.dp)
                                        .height(40.dp)
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                // Right side buttons
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Radio Button Placeholder
                                    ButtonPlaceholder(
                                        modifier = Modifier
                                            .width(100.dp)
                                            .height(40.dp)
                                    )

                                    // Shuffle Button Placeholder
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .shimmer()
                                            .background(
                                                MaterialTheme.colorScheme.onSurface,
                                                RoundedCornerShape(24.dp)
                                            )
                                    )
                                }
                            }
                        }
                        // Songs List Placeholder
                        repeat(6) {
                            ListItemPlaceHolder()
                        }
                    }
                }
            } else {
                item(key = "header") {
                    val thumbnail = artistPage?.artist?.thumbnail ?: libraryArtist?.artist?.thumbnailUrl
                    val artistName = artistPage?.artist?.title ?: libraryArtist?.artist?.name

                    Box {
                        // Artist Image with offset
                        if (thumbnail != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .offset {
                                        IntOffset(x = 0, y = headerOffset)
                                    }
                            ) {
                                AsyncImage(
                                    model = thumbnail.resize(1200, 1200),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.TopCenter)
                                        .fadingEdge(
                                            bottom = 200.dp,
                                        ),
                                )
                            }
                        }

                        // Artist Name and Controls Section - positioned at bottom of image
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    top = if (thumbnail != null) {
                                        // Position content at the bottom part of the image
                                        // Using screen width to calculate aspect ratio height minus overlap
                                        LocalResources.current.displayMetrics.widthPixels.let { screenWidth ->
                                            with(density) {
                                                ((screenWidth / 1.2f) - 144).toDp()
                                            }
                                        }
                                    } else {
                                        16.dp
                                    }
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                // Artist Name
                                Text(
                                    text = artistName ?: "Unknown",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 32.sp,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                // Buttons Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Subscribe Button
                                    OutlinedButton(
                                        onClick = {
                                            database.transaction {
                                                val artist = libraryArtist?.artist
                                                if (artist != null) {
                                                    update(artist.toggleLike())
                                                } else {
                                                    artistPage?.artist?.let {
                                                        insert(
                                                            ArtistEntity(
                                                                id = it.id,
                                                                name = it.title,
                                                                channelId = it.channelId,
                                                                thumbnailUrl = it.thumbnail,
                                                            ).toggleLike()
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (libraryArtist?.artist?.bookmarkedAt != null)
                                                MaterialTheme.colorScheme.surface
                                            else
                                                Color.Transparent
                                        ),
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier.height(40.dp)
                                    ) {
                                        val isSubscribed = libraryArtist?.artist?.bookmarkedAt != null
                                        Text(
                                            text = stringResource(if (isSubscribed) R.string.subscribed else R.string.subscribe),
                                            fontSize = 14.sp,
                                            color = if (!isSubscribed) MaterialTheme.colorScheme.error else LocalContentColor.current
                                        )
                                    }

                                    Spacer(modifier = Modifier.weight(1f))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Radio Button
                                        if (!showLocal) {
                                            artistPage?.artist?.radioEndpoint?.let { radioEndpoint ->
                                                OutlinedButton(
                                                    onClick = {
                                                        playerConnection.playQueue(YouTubeQueue(radioEndpoint))
                                                    },
                                                    shape = RoundedCornerShape(50),
                                                    modifier = Modifier.height(40.dp)
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.radio),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = stringResource(R.string.radio),
                                                        fontSize = 14.sp
                                                    )
                                                }
                                            }
                                        }

                                        // Shuffle Button
                                        if (!showLocal) {
                                            artistPage?.artist?.shuffleEndpoint?.let { shuffleEndpoint ->
                                                IconButton(
                                                    onClick = {
                                                        playerConnection.playQueue(YouTubeQueue(shuffleEndpoint))
                                                    },
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .background(
                                                            MaterialTheme.colorScheme.primary,
                                                            RoundedCornerShape(24.dp)
                                                        )
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.shuffle),
                                                        contentDescription = "Shuffle",
                                                        tint = MaterialTheme.colorScheme.onPrimary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        } else if (librarySongs.isNotEmpty()) {
                                            IconButton(
                                                onClick = {
                                                    val shuffledSongs = librarySongs.shuffled()
                                                    if (shuffledSongs.isNotEmpty()) {
                                                        playerConnection.playQueue(
                                                            ListQueue(
                                                                title = libraryArtist?.artist?.name ?: "Unknown Artist",
                                                                items = shuffledSongs.map { it.toMediaItem() }
                                                            )
                                                        )
                                                    }
                                                },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.primary,
                                                        RoundedCornerShape(24.dp)
                                                    )
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.shuffle),
                                                    contentDescription = "Shuffle",
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }

                if (showLocal) {
                    if (librarySongs.isNotEmpty()) {
                        item {
                            NavigationTitle(
                                title = stringResource(R.string.songs),
                                onClick = {
                                    navController.navigate("artist/${viewModel.artistId}/songs")
                                }
                            )
                        }

                        val filteredLibrarySongs = if (hideExplicit) {
                            librarySongs.filter { !it.song.explicit }
                        } else {
                            librarySongs
                        }
                        itemsIndexed(
                            items = filteredLibrarySongs,
                            key = { _, item -> "local_song_${item.id}" }
                        ) { index, song ->
                            SongListItem(
                                song = song,
                                showInLibraryIcon = true,
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            if (song.id == mediaMetadata?.id) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(
                                                    ListQueue(
                                                        title = libraryArtist?.artist?.name ?: "Unknown Artist",
                                                        items = librarySongs.map { it.toMediaItem() },
                                                        startIndex = index
                                                    )
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
                                    )
                                    .animateItem(),
                            )
                        }
                    }

                    if (libraryAlbums.isNotEmpty()) {
                        item {
                            NavigationTitle(
                                title = stringResource(R.string.albums),
                                onClick = {
                                    navController.navigate("artist/${viewModel.artistId}/albums")
                                }
                            )
                        }

                        item {
                            val filteredLibraryAlbums = if (hideExplicit) {
                                libraryAlbums.filter { !it.album.explicit }
                            } else {
                                libraryAlbums
                            }
                            LazyRow {
                                items(
                                    items = filteredLibraryAlbums,
                                    key = { "local_album_${it.id}" }
                                ) { album ->
                                    AlbumGridItem(
                                        album = album,
                                        isActive = mediaMetadata?.album?.id == album.id,
                                        isPlaying = isPlaying,
                                        coroutineScope = coroutineScope,
                                        modifier = Modifier
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
                                                            onDismiss = menuState::dismiss
                                                        )
                                                    }
                                                }
                                            )
                                            .animateItem()
                                    )
                                }
                            }
                        }
                    }
                } else {
                    artistPage?.sections?.fastForEach { section ->
                        if (section.items.isNotEmpty()) {
                            item {
                                NavigationTitle(
                                    title = section.title,
                                    onClick = section.moreEndpoint?.let {
                                        {
                                            navController.navigate(
                                                "artist/${viewModel.artistId}/items?browseId=${it.browseId}?params=${it.params}",
                                            )
                                        }
                                    },
                                )
                            }
                        }

                        if ((section.items.firstOrNull() as? SongItem)?.album != null) {
                            items(
                                items = section.items.distinctBy { it.id },
                                key = { it.id },
                            ) { song ->
                                YouTubeListItem(
                                    item = song as SongItem,
                                    isActive = mediaMetadata?.id == song.id,
                                    isPlaying = isPlaying,
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                menuState.show {
                                                    YouTubeSongMenu(
                                                        song = song,
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
                                    modifier = Modifier
                                        .combinedClickable(
                                            onClick = {
                                                if (song.id == mediaMetadata?.id) {
                                                    playerConnection.player.togglePlayPause()
                                                } else {
                                                    playerConnection.playQueue(
                                                        YouTubeQueue(
                                                            WatchEndpoint(videoId = song.id),
                                                            song.toMediaMetadata()
                                                        ),
                                                    )
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    YouTubeSongMenu(
                                                        song = song,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        )
                                        .animateItem(),
                                )
                            }
                        } else {
                            item {
                                LazyRow {
                                    items(
                                        items = section.items.distinctBy { it.id },
                                        key = { it.id },
                                    ) { item ->
                                        YouTubeGridItem(
                                            item = item,
                                            isActive = when (item) {
                                                is SongItem -> mediaMetadata?.id == item.id
                                                is AlbumItem -> mediaMetadata?.album?.id == item.id
                                                else -> false
                                            },
                                            isPlaying = isPlaying,
                                            coroutineScope = coroutineScope,
                                            modifier = Modifier
                                                .combinedClickable(
                                                    onClick = {
                                                        when (item) {
                                                            is SongItem ->
                                                                playerConnection.playQueue(
                                                                    YouTubeQueue(
                                                                        WatchEndpoint(videoId = item.id),
                                                                        item.toMediaMetadata()
                                                                    ),
                                                                )

                                                            is AlbumItem -> navController.navigate("album/${item.id}")
                                                            is ArtistItem -> navController.navigate("artist/${item.id}")
                                                            is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                                                        }
                                                    },
                                                    onLongClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        menuState.show {
                                                            when (item) {
                                                                is SongItem ->
                                                                    YouTubeSongMenu(
                                                                        song = item,
                                                                        navController = navController,
                                                                        onDismiss = menuState::dismiss,
                                                                    )

                                                                is AlbumItem ->
                                                                    YouTubeAlbumMenu(
                                                                        albumItem = item,
                                                                        navController = navController,
                                                                        onDismiss = menuState::dismiss,
                                                                    )

                                                                is ArtistItem ->
                                                                    YouTubeArtistMenu(
                                                                        artist = item,
                                                                        onDismiss = menuState::dismiss,
                                                                    )

                                                                is PlaylistItem ->
                                                                    YouTubePlaylistMenu(
                                                                        playlist = item,
                                                                        coroutineScope = coroutineScope,
                                                                        onDismiss = menuState::dismiss,
                                                                    )
                                                            }
                                                        }
                                                    },
                                                )
                                                .animateItem(),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        HideOnScrollFAB(
            visible = librarySongs.isNotEmpty() && libraryArtist?.artist?.isLocal != true,
            lazyListState = lazyListState,
            icon = if (showLocal) R.drawable.language else R.drawable.library_music,
            onClick = {
                showLocal = showLocal.not()
                if (!showLocal && artistPage == null) viewModel.fetchArtistsFromYTM()
            }
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                .align(Alignment.BottomCenter)
        )
    }

    TopAppBar(
        title = { if (!transparentAppBar) Text(artistPage?.artist?.title.orEmpty()) },
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
        actions = {
            IconButton(
                onClick = {
                    viewModel.artistPage?.artist?.shareLink?.let { link ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Artist Link", link)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, R.string.link_copied, Toast.LENGTH_SHORT).show()
                    }
                },
            ) {
                Icon(
                    painterResource(R.drawable.link),
                    contentDescription = null,
                )
            }
        },
        colors = if (transparentAppBar) {
            TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        } else {
            TopAppBarDefaults.topAppBarColors()
        }
    )
}
