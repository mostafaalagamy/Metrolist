package com.metrolist.music

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.imageLoader
import coil.request.ImageRequest
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.music.constants.*
import com.metrolist.music.constants.DarkModeKey
import com.metrolist.music.constants.DefaultOpenTabKey
import com.metrolist.music.constants.DynamicThemeKey
import com.metrolist.music.constants.PauseSearchHistoryKey
import com.metrolist.music.constants.PureBlackKey
import com.metrolist.music.constants.SearchSource
import com.metrolist.music.constants.SearchSourceKey
import com.metrolist.music.constants.SlimNavBarKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.SearchHistory
import com.metrolist.music.extensions.*
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.DownloadUtil
import com.metrolist.music.playback.MusicService
import com.metrolist.music.playback.MusicService.MusicBinder
import com.metrolist.music.playback.PlayerConnection
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.*
import com.metrolist.music.ui.component.rememberBottomSheetState
import com.metrolist.music.ui.component.shimmer.ShimmerTheme
import com.metrolist.music.ui.menu.YouTubeSongMenu
import com.metrolist.music.ui.player.BottomSheetPlayer
import com.metrolist.music.ui.screens.*
import com.metrolist.music.ui.screens.navigationBuilder
import com.metrolist.music.ui.screens.search.LocalSearchScreen
import com.metrolist.music.ui.screens.search.OnlineSearchScreen
import com.metrolist.music.ui.screens.settings.*
import com.metrolist.music.ui.theme.ColorSaver
import com.metrolist.music.ui.theme.DefaultThemeColor
import com.metrolist.music.ui.theme.MetrolistTheme
import com.metrolist.music.ui.theme.extractThemeColor
import com.metrolist.music.ui.utils.appBarScrollBehavior
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.ui.utils.resetHeightOffset
import com.metrolist.music.utils.Updater
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.utils.reportException
import com.valentinilk.shimmer.LocalShimmerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Locale
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var downloadUtil: DownloadUtil

    private var playerConnection by mutableStateOf<PlayerConnection?>(null)
    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?,
            ) {
                if (service is MusicBinder) {
                    playerConnection = PlayerConnection(this@MainActivity, service, database, lifecycleScope)
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                playerConnection?.dispose()
                playerConnection = null
            }
        }

    private var latestVersionName by mutableStateOf(BuildConfig.VERSION_NAME)

    override fun onStart() {
        super.onStart()
        startService(Intent(this, MusicService::class.java))
        bindService(Intent(this, MusicService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        unbindService(serviceConnection)
        super.onStop()
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR
        WindowCompat.setDecorFitsSystemWindows(window, false)

	lifecycleScope.launch {
            dataStore.data
                .map { it[DisableScreenshotKey] ?: false }
                .distinctUntilChanged()
                .collectLatest {
                    if (it) {
                        window.setFlags(
                            WindowManager.LayoutParams.FLAG_SECURE,
                            WindowManager.LayoutParams.FLAG_SECURE,
                        )
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
	}

	    val sharedPreferences = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            val savedLanguage = sharedPreferences.getString("app_language", Locale.getDefault().language) ?: "en"
                updateLanguage(this, savedLanguage)

        intent?.let { handlevideoIdIntent(it) }

        setContent {
            LaunchedEffect(Unit) {
                if (System.currentTimeMillis() - Updater.lastCheckTime > 1.days.inWholeMilliseconds) {
                    Updater.getLatestVersionName().onSuccess {
                        latestVersionName = it
                    }
                }
	    }

            val enableDynamicTheme by rememberPreference(DynamicThemeKey, defaultValue = true)
            val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
            val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
            val isSystemInDarkTheme = isSystemInDarkTheme()
            val useDarkTheme =
                remember(darkTheme, isSystemInDarkTheme) {
                    if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
                }
            LaunchedEffect(useDarkTheme) {
                setSystemBarAppearance(useDarkTheme)
            }
            var themeColor by rememberSaveable(stateSaver = ColorSaver) {
                mutableStateOf(DefaultThemeColor)
            }

            LaunchedEffect(playerConnection, enableDynamicTheme, isSystemInDarkTheme) {
                val playerConnection = playerConnection
                if (!enableDynamicTheme || playerConnection == null) {
                    themeColor = DefaultThemeColor
                    return@LaunchedEffect
                }
                playerConnection.service.currentMediaMetadata.collectLatest { song ->
                    themeColor =
                        if (song != null) {
                            withContext(Dispatchers.IO) {
                                val result =
                                    imageLoader.execute(
                                        ImageRequest
                                            .Builder(this@MainActivity)
                                            .data(song.thumbnailUrl)
                                            .allowHardware(false) // pixel access is not supported on Config#HARDWARE bitmaps
                                            .build(),
                                    )
                                (result.drawable as? BitmapDrawable)?.bitmap?.extractThemeColor() ?: DefaultThemeColor
                            }
                        } else {
                            DefaultThemeColor
                        }
                }
            }

            MetrolistTheme(
                darkTheme = useDarkTheme,
                pureBlack = pureBlack,
                themeColor = themeColor,
            ) {
                BoxWithConstraints(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface),
                ) {
                    val focusManager = LocalFocusManager.current
                    val density = LocalDensity.current
                    val windowsInsets = WindowInsets.systemBars
                    val bottomInset = with(density) { windowsInsets.getBottom(density).toDp() }

                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()

                    val navigationItems = remember { Screens.MainScreens }
		    val (slimNav) = rememberPreference(SlimNavBarKey, defaultValue = false)
                    val defaultOpenTab =
                        remember {
                            dataStore[DefaultOpenTabKey].toEnum(defaultValue = NavigationTab.HOME)
                        }
                    val tabOpenedFromShortcut =
                        remember {
                            when (intent?.action) {
                                ACTION_LIBRARY -> NavigationTab.LIBRARY
                                ACTION_EXPLORE -> NavigationTab.EXPLORE
                                else -> null
                            }
                        }

                    val topLevelScreens =
                        listOf(
                            Screens.Home.route,
                            Screens.Explore.route,
                            Screens.Library.route,
                            "settings",
                        )

                    val (query, onQueryChange) =
                        rememberSaveable(stateSaver = TextFieldValue.Saver) {
                            mutableStateOf(TextFieldValue())
			}

                    var active by rememberSaveable {
                        mutableStateOf(false)
		    }

		    val onActiveChange: (Boolean) -> Unit = { newActive ->
                        active = newActive
                        if (!newActive) {
                            focusManager.clearFocus()
                            if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                                onQueryChange(TextFieldValue())
                            }
                        }
                    }

                    var searchSource by rememberEnumPreference(SearchSourceKey, SearchSource.ONLINE)

                    val searchBarFocusRequester = remember { FocusRequester() }

                    val onSearch: (String) -> Unit = {
                        if (it.isNotEmpty()) {
                            onActiveChange(false)
                            navController.navigate("search/${URLEncoder.encode(it, "UTF-8")}")
                            if (dataStore[PauseSearchHistoryKey] != true) {
                                database.query {
                                    insert(SearchHistory(query = it))
                                }
                            }
                        }
                    }

                    var openSearchImmediately: Boolean by remember {
                        mutableStateOf(intent?.action == ACTION_SEARCH)
		    }

		    val shouldShowSearchBar =
                        remember(active, navBackStackEntry) {
                            active ||
                                navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } ||
                                navBackStackEntry?.destination?.route?.startsWith("search/") == true
			}

		    val shouldShowNavigationBar =
                        remember(navBackStackEntry, active) {
                            navBackStackEntry?.destination?.route == null ||
                                navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } &&
                                !active
			}
			
                    val navigationBarHeight by animateDpAsState(
                        targetValue = if (shouldShowNavigationBar) NavigationBarHeight else 0.dp,
                        animationSpec = NavigationBarAnimationSpec,
                        label = "",
                    )

                    val playerBottomSheetState =
                        rememberBottomSheetState(
                            dismissedBound = 0.dp,
                            collapsedBound = bottomInset + (if (shouldShowNavigationBar) NavigationBarHeight else 0.dp) + MiniPlayerHeight,
                            expandedBound = maxHeight,
                        )

                    val playerAwareWindowInsets =
                        remember(bottomInset, shouldShowNavigationBar, playerBottomSheetState.isDismissed) {
                            var bottom = bottomInset
                            if (shouldShowNavigationBar) bottom += NavigationBarHeight
                            if (!playerBottomSheetState.isDismissed) bottom += MiniPlayerHeight
                            windowsInsets
                                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                                .add(WindowInsets(top = AppBarHeight, bottom = bottom))
                        }

                    val searchBarScrollBehavior =
                        appBarScrollBehavior(
                            canScroll = {
                                navBackStackEntry?.destination?.route?.startsWith("search/") == false &&
                                    (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                            },
                        )
                    val topAppBarScrollBehavior =
                        appBarScrollBehavior(
                            canScroll = {
                                navBackStackEntry?.destination?.route?.startsWith("search/") == false &&
                                    (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                            },
                        )

                    LaunchedEffect(navBackStackEntry) {
                        if (navBackStackEntry?.destination?.route?.startsWith("search/") == true) {
                            val searchQuery =
                                withContext(Dispatchers.IO) {
                                    if (navBackStackEntry
                                            ?.arguments
                                            ?.getString(
                                                "query",
                                            )!!
                                            .contains(
                                                "%",
                                            )
                                    ) {
                                        navBackStackEntry?.arguments?.getString(
                                            "query",
                                        )!!
                                    } else {
                                        URLDecoder.decode(navBackStackEntry?.arguments?.getString("query")!!, "UTF-8")
                                    }
                                }
                            onQueryChange(TextFieldValue(searchQuery, TextRange(searchQuery.length)))
                        } else if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                            onQueryChange(TextFieldValue())
                        }
                        searchBarScrollBehavior.state.resetHeightOffset()
                        topAppBarScrollBehavior.state.resetHeightOffset()
                    }
                    LaunchedEffect(active) {
                        if (active) {
                            searchBarScrollBehavior.state.resetHeightOffset()
                            topAppBarScrollBehavior.state.resetHeightOffset()
			    searchBarFocusRequester.requestFocus()
                        }
                    }

                    LaunchedEffect(playerConnection) {
                        val player = playerConnection?.player ?: return@LaunchedEffect
                        if (player.currentMediaItem == null) {
                            if (!playerBottomSheetState.isDismissed) {
                                playerBottomSheetState.dismiss()
                            }
                        } else {
                            if (playerBottomSheetState.isDismissed) {
                                playerBottomSheetState.collapseSoft()
                            }
                        }
                    }

                    DisposableEffect(playerConnection, playerBottomSheetState) {
                        val player = playerConnection?.player ?: return@DisposableEffect onDispose { }
                        val listener =
                            object : Player.Listener {
                                override fun onMediaItemTransition(
                                    mediaItem: MediaItem?,
                                    reason: Int,
                                ) {
                                    if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED &&
                                        mediaItem != null &&
                                        playerBottomSheetState.isDismissed
                                    ) {
                                        playerBottomSheetState.collapseSoft()
                                    }
                                }
                            }
                        player.addListener(listener)
                        onDispose {
                            player.removeListener(listener)
                        }
                    }

                    val coroutineScope = rememberCoroutineScope()
                    var sharedSong: SongItem? by remember {
                        mutableStateOf(null)
                    }
                    DisposableEffect(Unit) {
                        val listener =
                            Consumer<Intent> { intent ->
                                val uri = intent.data ?: intent.extras?.getString(Intent.EXTRA_TEXT)?.toUri() ?: return@Consumer
                                when (val path = uri.pathSegments.firstOrNull()) {
                                    "playlist" ->
                                        uri.getQueryParameter("list")?.let { playlistId ->
                                            if (playlistId.startsWith("OLAK5uy_")) {
                                                coroutineScope.launch {
                                                    YouTube
                                                        .albumSongs(playlistId)
                                                        .onSuccess { songs ->
                                                            songs.firstOrNull()?.album?.id?.let { browseId ->
                                                                navController.navigate("album/$browseId")
                                                            }
                                                        }.onFailure {
                                                            reportException(it)
                                                        }
                                                }
                                            } else {
                                                navController.navigate("online_playlist/$playlistId")
                                            }
                                        }

				    "browse" ->
					uri.lastPathSegment?.let { browseId ->
                                            navController.navigate("album/$browseId")
					}

                                    "channel", "c" ->
                                        uri.lastPathSegment?.let { artistId ->
                                            navController.navigate("artist/$artistId")
                                        }

                                    else ->
                                        when {
                                            path == "watch" -> uri.getQueryParameter("v")
                                            uri.host == "youtu.be" -> path
                                            else -> null
                                        }?.let { videoId ->
                                    coroutineScope.launch {
                                        withContext(Dispatchers.IO) {
                                            YouTube.queue(listOf(videoId))
                                        }.onSuccess {
                                            playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = it.firstOrNull()?.id), it.firstOrNull()?.toMediaMetadata()))
                                        }.onFailure {
                                            reportException(it)
                                        }
                                    }
                                }
                            }
                        }

                        addOnNewIntentListener(listener)
                        onDispose { removeOnNewIntentListener(listener) }
                    }

		    CompositionLocalProvider(
                        LocalDatabase provides database,
                        LocalContentColor provides contentColorFor(MaterialTheme.colorScheme.surface),
                        LocalPlayerConnection provides playerConnection,
                        LocalPlayerAwareWindowInsets provides playerAwareWindowInsets,
                        LocalDownloadUtil provides downloadUtil,
                        LocalShimmerTheme provides ShimmerTheme,
                    ) {
	                NavHost(
                            navController = navController,
                            startDestination = when (tabOpenedFromShortcut ?: defaultOpenTab) {
                                NavigationTab.HOME -> Screens.Home
                                NavigationTab.EXPLORE -> Screens.Explore
                                NavigationTab.LIBRARY -> Screens.Library
                            }.route,
                            enterTransition = {
                                val currentTab = initialState.destination.route
                                val targetTab = targetState.destination.route
        
                                val direction = when {
                                    targetTab == "settings" -> AnimatedContentTransitionScope.SlideDirection.Left
            
                                    currentTab in topLevelScreens && targetTab in topLevelScreens -> {
                                        val currentIndex = navigationItems.indexOfFirst { it.route == currentTab }
                                        val targetIndex = navigationItems.indexOfFirst { it.route == targetTab }
                                        if (targetIndex > currentIndex) 
                                            AnimatedContentTransitionScope.SlideDirection.Left
                                        else 
                                            AnimatedContentTransitionScope.SlideDirection.Right
                                    }
                                    else -> AnimatedContentTransitionScope.SlideDirection.Left
                                }
        
                                slideIntoContainer(
                                    towards = direction,
                                    animationSpec = tween(200)
                                )
                            },
                            exitTransition = {
                                val currentTab = initialState.destination.route
                                val targetTab = targetState.destination.route
        
                                val direction = when {
                                    targetTab == "settings" -> AnimatedContentTransitionScope.SlideDirection.Left
            
                                    currentTab in topLevelScreens && targetTab in topLevelScreens -> {
                                        val currentIndex = navigationItems.indexOfFirst { it.route == currentTab }
                                        val targetIndex = navigationItems.indexOfFirst { it.route == targetTab }
                                        if (targetIndex > currentIndex) 
                                            AnimatedContentTransitionScope.SlideDirection.Left
                                        else 
                                            AnimatedContentTransitionScope.SlideDirection.Right
                                    }
                                    else -> AnimatedContentTransitionScope.SlideDirection.Left
                                }
        
                                slideOutOfContainer(
                                    towards = direction,
                                    animationSpec = tween(200)
                                )
                            },
                            popEnterTransition = {
                                val currentTab = initialState.destination.route
        
                                if (currentTab == "settings") {
                                    slideIntoContainer(
                                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                        animationSpec = tween(200)
                                    )
                                } else {
                                    slideIntoContainer(
                                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                        animationSpec = tween(200)
                                    )
                                }
                            },
                            popExitTransition = {
                                val currentTab = initialState.destination.route
        
                                if (currentTab == "settings") {
                                    slideOutOfContainer(
                                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                        animationSpec = tween(200)
                                    )
                                } else {
                                    slideOutOfContainer(
                                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                        animationSpec = tween(200)
                                    )
                                }
                            },
                            modifier = Modifier.nestedScroll(
                                if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } ||
                                    navBackStackEntry?.destination?.route?.startsWith("search/") == true
                                ) {
                                    searchBarScrollBehavior.nestedScrollConnection
                                } else {
                                    topAppBarScrollBehavior.nestedScrollConnection
                                }
                            )
                        ) {
                            navigationBuilder(navController, topAppBarScrollBehavior, latestVersionName)
		   	    }

			val currentTitle = remember(navBackStackEntry) {
 			    when (navBackStackEntry?.destination?.route) {
        			Screens.Home.route -> R.string.home
        			Screens.Explore.route -> R.string.explore
        			Screens.Library.route -> R.string.filter_library
        			else -> null
    			    }
			}

                        if (!active && navBackStackEntry?.destination?.route in topLevelScreens && navBackStackEntry?.destination?.route != "settings") {
                            TopAppBar(
                                title = { 
                                    Text(
                                        text = currentTitle?.let { stringResource(it) } ?: "",
                                        style = MaterialTheme.typography.titleLarge,
                                    ) 
                                },
                                actions = {
                                    IconButton(
                                        onClick = { 
                                            onActiveChange(true)
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.search),
                                            contentDescription = stringResource(R.string.search)
                                        )
                                    }
                                    IconButton(
                                        onClick = { 
                                            navController.navigate("settings") 
                                        }
                                    ) {
                                        BadgedBox(
                                            badge = {
                                                if (latestVersionName != BuildConfig.VERSION_NAME) {
                                                    Badge()
                                               }
                                           }
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.settings),
                                                contentDescription = stringResource(R.string.settings),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                },
                                scrollBehavior = searchBarScrollBehavior
                            )
                        } else if (active || navBackStackEntry?.destination?.route?.startsWith("search/") == true) {
                            TopSearch(
                                query = query,
                                onQueryChange = onQueryChange,
                                onSearch = onSearch,
                                active = active,
                                onActiveChange = onActiveChange,
                                scrollBehavior = searchBarScrollBehavior,
                                placeholder = {
                                    Text(
                                        text = stringResource(
                                            when (searchSource) {
                                                SearchSource.LOCAL -> R.string.search_library
                                                SearchSource.ONLINE -> R.string.search_yt_music
                                            }
                                        ),
                                    )
                                },
                                leadingIcon = {
                                    IconButton(
                                        onClick = {
                                            when {
                                                active -> onActiveChange(false)
                                                !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } -> {
                                                    navController.navigateUp()
                                                }
                                                else -> onActiveChange(true)
                                            }
                                        },
                                        onLongClick = {
                                            when {
                                                active -> {}
                                                !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } -> {
                                                    navController.backToMain()
                                                }
                                                else -> {}
                                            }
                                        },
                                    ) {
                                        Icon(
                                            painterResource(
                                                if (active ||
                                                !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }
                                                ) {
                                                    R.drawable.arrow_back
                                                } else {
                                                    R.drawable.search
                                                },
                                            ),
                                            contentDescription = null,
                                        )
                                    }
                                },
                                trailingIcon = {
                                    Row {
                                        if (active) {
                                            if (query.text.isNotEmpty()) {
                                                IconButton(
                                                    onClick = { onQueryChange(TextFieldValue("")) },
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.close),
                                                        contentDescription = null,
                                                    )
                                                }
                                            }
                                            IconButton(
                                                onClick = {
                                                    searchSource =
                                                        if (searchSource == SearchSource.ONLINE) SearchSource.LOCAL else SearchSource.ONLINE
                                                },
                                            ) {
                                                Icon(
                                                    painter = painterResource(
                                                        when (searchSource) {
                                                            SearchSource.LOCAL -> R.drawable.library_music
                                                            SearchSource.ONLINE -> R.drawable.language
                                                        },
                                                    ),
                                                    contentDescription = null,
                                                )
                                            }
                                        }
                                    }
                                },
                                focusRequester = searchBarFocusRequester,
                                modifier = Modifier.align(Alignment.TopCenter),
                            ) {
                                Crossfade(
                                    targetState = searchSource,
                                    label = "",
                                    modifier =
                                        Modifier
                                            .fillMaxSize()
                                            .padding(bottom = if (!playerBottomSheetState.isDismissed) MiniPlayerHeight else 0.dp)
                                            .navigationBarsPadding(),
                                ) { searchSource ->
                                    when (searchSource) {
                                        SearchSource.LOCAL ->
                                            LocalSearchScreen(
                                                query = query.text,
                                                navController = navController,
                                                onDismiss = { onActiveChange(false) },
                                            )

                                        SearchSource.ONLINE ->
                                            OnlineSearchScreen(
                                                query = query.text,
                                                onQueryChange = onQueryChange,
                                                navController = navController,
                                                onSearch = {
                                                    navController.navigate("search/${URLEncoder.encode(it, "UTF-8")}")
                                                    if (dataStore[PauseSearchHistoryKey] != true) {
                                                        database.query {
                                                            insert(SearchHistory(query = it))
                                                        }
                                                    }
                                                },
                                                onDismiss = { onActiveChange(false) },
                                            )
                                    }
                                }
                            }
			}

                        BottomSheetPlayer(
                            state = playerBottomSheetState,
                            navController = navController,
                        )

                        NavigationBar(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .offset {
                                    if (navigationBarHeight == 0.dp) {
                                        IntOffset(
                                            x = 0,
                                            y = (bottomInset + NavigationBarHeight).roundToPx(),
                                        )
                                    } else {
                                        val slideOffset =
                                            (bottomInset + NavigationBarHeight) *
                                                playerBottomSheetState.progress.coerceIn(
                                                    0f,
                                                    1f,
                                                )
                                        val hideOffset =
                                            (bottomInset + NavigationBarHeight) * (1 - navigationBarHeight / NavigationBarHeight)
                                        IntOffset(
                                            x = 0,
                                            y = (slideOffset + hideOffset).roundToPx(),
                                        )
                                    }
                                },
                        ) {
                            navigationItems.fastForEach { screen ->
                                val isSelected = navBackStackEntry?.destination?.hierarchy?.any { it.route == screen.route } == true

                                NavigationBarItem(
                                    selected = isSelected,
                                    icon = {
                                        Icon(
                                            painter = painterResource(
                                                id = if (isSelected) screen.iconIdActive else screen.iconIdInactive
                                            ),
                                            contentDescription = null,
                                        )
                                    },
                                    label = {
                                        if (!slimNav) {
                                            Text(
                                                text = stringResource(screen.titleId),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    },
                                    onClick = {
                                        if (isSelected) {
                                            navController.currentBackStackEntry?.savedStateHandle?.set("scrollToTop", true)
                                            coroutineScope.launch {
                                                searchBarScrollBehavior.state.resetHeightOffset()
                                            }
                                        } else {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                )
                            }
                        }

                        BottomSheetMenu(
                            state = LocalMenuState.current,
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )

                        sharedSong?.let { song ->
                            playerConnection?.let { playerConnection ->
                                Dialog(
                                    onDismissRequest = { sharedSong = null },
                                    properties = DialogProperties(usePlatformDefaultWidth = false),
                                ) {
                                    Surface(
                                        modifier = Modifier.padding(24.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        color = AlertDialogDefaults.containerColor,
                                        tonalElevation = AlertDialogDefaults.TonalElevation,
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            YouTubeSongMenu(
                                                song = song,
                                                navController = navController,
                                                onDismiss = { sharedSong = null },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    LaunchedEffect(shouldShowSearchBar, openSearchImmediately) {
                        if (shouldShowSearchBar && openSearchImmediately) {
                            onActiveChange(true)
                            searchBarFocusRequester.requestFocus()
                            openSearchImmediately = false
                        }
                    }
                }
            }
        }
    }

    private fun handlevideoIdIntent(intent: Intent) {
        val uri = intent.data ?: intent.extras?.getString(Intent.EXTRA_TEXT)?.toUri() ?: return
        when {
            uri.pathSegments.firstOrNull() == "watch" -> uri.getQueryParameter("v")
            uri.host == "youtu.be" -> uri.pathSegments.firstOrNull()
            else -> null
        }?.let { videoId ->
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    YouTube.queue(listOf(videoId))
                }.onSuccess {
                    playerConnection?.playQueue(
                        YouTubeQueue(
                            WatchEndpoint(videoId = it.firstOrNull()?.id),
                            it.firstOrNull()?.toMediaMetadata()
                        )
                    )
                }.onFailure {
                    reportException(it)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
    }
    
    @SuppressLint("ObsoleteSdkInt")
    private fun setSystemBarAppearance(isDark: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView.rootView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            window.statusBarColor = (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            window.navigationBarColor = (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
    }

    companion object {
        const val ACTION_SEARCH = "com.metrolist.music.action.SEARCH"
        const val ACTION_EXPLORE = "com.metrolist.music.action.EXPLORE"
        const val ACTION_LIBRARY = "com.metrolist.music.action.LIBRARY"
    }
}

val LocalDatabase = staticCompositionLocalOf<MusicDatabase> { error("No database provided") }
val LocalPlayerConnection = staticCompositionLocalOf<PlayerConnection?> { error("No PlayerConnection provided") }
val LocalPlayerAwareWindowInsets = compositionLocalOf<WindowInsets> { error("No WindowInsets provided") }
val LocalDownloadUtil = staticCompositionLocalOf<DownloadUtil> { error("No DownloadUtil provided") }
