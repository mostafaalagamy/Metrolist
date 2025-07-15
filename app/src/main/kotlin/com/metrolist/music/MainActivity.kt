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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.datastore.preferences.core.edit
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.metrolist.music.constants.AppBarHeight
import com.metrolist.music.constants.CONTENT_TYPE_HEADER
import com.metrolist.music.constants.DefaultOpenTabKey
import com.metrolist.music.constants.DynamicThemeKey
import com.metrolist.music.constants.MiniPlayerHeight
import com.metrolist.music.constants.NavigationBarAnimationSpec
import com.metrolist.music.constants.NavigationBarHeight
import com.metrolist.music.constants.NavigationRailWidth
import com.metrolist.music.ui.component.SideNavigationRail
import com.metrolist.music.utils.DeviceUtils
import com.metrolist.music.constants.PauseSearchHistoryKey
import com.metrolist.music.constants.PureBlackKey
import com.metrolist.music.constants.SearchSource
import com.metrolist.music.constants.SearchSourceKey
import com.metrolist.music.constants.SlimNavigationKey
import com.metrolist.music.constants.SYSTEM_COLOR_BACKGROUND_KEY
import com.metrolist.music.constants.SYSTEM_COLOR_BACKGROUND_STREAM
import com.metrolist.music.constants.SYSTEM_COLOR_PRIMARY_KEY
import com.metrolist.music.constants.SYSTEM_COLOR_PRIMARY_STREAM
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.SearchHistory
import com.metrolist.music.extensions.fastAny
import com.metrolist.music.extensions.fastFirstOrNull
import com.metrolist.music.extensions.fastForEach
import com.metrolist.music.extensions.toEnum
import com.metrolist.music.playback.MusicService
import com.metrolist.music.playback.PlayerConnection
import com.metrolist.music.ui.component.BottomSheet
import com.metrolist.music.ui.component.BottomSheetMenu
import com.metrolist.music.ui.component.BottomSheetPage
import com.metrolist.music.ui.component.BottomSheetPlayer
import com.metrolist.music.ui.component.LocalBottomSheetPageState
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.LocalPlayerAwareWindowInsets
import com.metrolist.music.ui.component.LocalPlayerConnection
import com.metrolist.music.ui.component.NavigationTitle
import com.metrolist.music.ui.component.YouTubeSongMenu
import com.metrolist.music.ui.component.rememberBottomSheetState
import com.metrolist.music.ui.component.shimmer.ShimmerTheme
import com.metrolist.music.ui.component.shimmer.LocalShimmerTheme
import com.metrolist.music.ui.menu.YouTubeSongMenu
import com.metrolist.music.ui.screens.AccountSettingsDialog
import com.metrolist.music.ui.screens.LocalSearchScreen
import com.metrolist.music.ui.screens.NavigationTab
import com.metrolist.music.ui.screens.OnlineSearchScreen
import com.metrolist.music.ui.screens.Screens
import com.metrolist.music.ui.screens.navigationBuilder
import com.metrolist.music.ui.theme.DefaultTheme
import com.metrolist.music.ui.theme.toColorScheme
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.metrolist.music.viewmodels.AccountViewModel
import com.metrolist.music.viewmodels.UIStateViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.net.URLEncoder
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var database: MusicDatabase

    private var playerConnection by mutableStateOf<PlayerConnection?>(null)
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MusicService.MusicBinder) {
                playerConnection = PlayerConnection(this@MainActivity, service, database)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playerConnection?.dispose()
            playerConnection = null
        }
    }

    override fun onStart() {
        super.onStart()
        if (playerConnection == null) {
            bindService(Intent(this, MusicService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        unbindService(serviceConnection)
        playerConnection?.dispose()
        playerConnection = null
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val accountViewModel = hiltViewModel<AccountViewModel>()
            val uiStateViewModel = hiltViewModel<UIStateViewModel>()

            val dynamicTheme by dataStore.data.collectAsState(initial = null)
            val pureBlack by dataStore.data.collectAsState(initial = null)
            val slimNav by dataStore.data.collectAsState(initial = null)
            val defaultOpenTab by dataStore.data.collectAsState(initial = null)
            val pauseSearchHistory by dataStore.data.collectAsState(initial = null)
            val searchSource by dataStore.data.collectAsState(initial = null)

            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()

            val navigationItems = listOf(Screens.Home, Screens.Search, Screens.Library)

            var sharedSong by remember { mutableStateOf<String?>(null) }
            var tabOpenedFromShortcut by remember { mutableStateOf<NavigationTab?>(null) }

            var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
                mutableStateOf(TextFieldValue())
            }
            var active by rememberSaveable { mutableStateOf(false) }
            val onActiveChange: (Boolean) -> Unit = { active = it }
            val onQueryChange: (TextFieldValue) -> Unit = { query = it }
            val searchBarFocusRequester = remember { FocusRequester() }
            val keyboardController = LocalSoftwareKeyboardController.current

            var searchSource by remember {
                mutableStateOf(searchSource?.get(SearchSourceKey)?.toEnum(SearchSource.ONLINE) ?: SearchSource.ONLINE)
            }

            var previousTab by remember { mutableStateOf(Screens.Home.route) }

            val accountImageUrl by accountViewModel.accountImageUrl.collectAsState()
            val latestVersionName by uiStateViewModel.latestVersionName.collectAsState()

            var showAccountDialog by remember { mutableStateOf(false) }

            LaunchedEffect(playerConnection, active, navBackStackEntry) {
                if (playerConnection == null) return@LaunchedEffect
                if (active) return@LaunchedEffect
                val currentTab = navBackStackEntry?.destination?.route
                if (navigationItems.fastAny { it.route == currentTab }) {
                    previousTab = currentTab!!
                }
            }

            LaunchedEffect(Unit) {
                if (intent?.action == Intent.ACTION_VIEW) {
                    val uri = intent.data ?: intent.extras?.getString(Intent.EXTRA_TEXT)?.toUri()
                    if (uri != null) {
                        val path = uri.pathSegments.firstOrNull()
                        when {
                            path == "playlist" -> uri.getQueryParameter("list")?.let { playlistId ->
                                navController.navigate("online_playlist/$playlistId")
                            }
                            path == "channel" || path?.startsWith("@") == true -> uri.lastPathSegment?.let { artistId ->
                                navController.navigate("artist/$artistId")
                            }
                            else -> handleDeepLinkIntent(intent, navController)
                        }
                    }
                } else if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (sharedText != null) {
                        if (playerConnection != null) {
                            sharedSong = sharedText
                        } else {
                            var retryCount = 0
                            while (playerConnection == null && retryCount < 50) {
                                delay(100)
                                retryCount++
                            }
                            if (playerConnection != null) {
                                sharedSong = sharedText
                            }
                        }
                    }
                }
                intent.getStringExtra("shortcut")?.let {
                    tabOpenedFromShortcut = NavigationTab.entries.find { tab -> tab.name == it }
                }
            }

            LaunchedEffect(searchSource) {
                dataStore.edit { settings ->
                    settings[SearchSourceKey] = searchSource.name
                }
            }

            DisposableEffect(active) {
                onDispose {
                    if (!active) {
                        query = TextFieldValue()
                        keyboardController?.hide()
                    }
                }
            }

            playerConnection?.let { connection ->
                val currentTitleRes by connection.currentTitle.collectAsState()
                val shouldShowTopBar = !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } && navBackStackEntry?.destination?.route != "search"

                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val density = LocalDensity.current
                    val windowsInsets = WindowInsets.systemBars
                    val bottomInset = with(density) { windowsInsets.getBottom(density).toDp() }
                    val bottomInsetDp = bottomInset

                    var openSearchImmediately: Boolean by remember {
                        mutableStateOf(intent?.action == Intent.ACTION_SEARCH)
                    }

                    val shouldShowSearchBar =
                        remember(active, navBackStackEntry) {
                            active ||
                                    navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } ||
                                    navBackStackEntry?.destination?.route?.startsWith("search/") == true
                        }

                    // Tablet mode detection
                    val shouldUseSideNavigation = DeviceUtils.shouldUseSideNavigation()
                    val navigationRailWidth = if (shouldUseSideNavigation) DeviceUtils.getNavigationRailWidth() else 0.dp

                    val shouldShowNavigationBar =
                        remember(navBackStackEntry, active, shouldUseSideNavigation) {
                            !shouldUseSideNavigation && (navBackStackEntry?.destination?.route == null ||
                                    navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } &&
                                    !active)
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
                        remember(
                            bottomInset,
                            shouldShowNavigationBar,
                            playerBottomSheetState.isDismissed
                        ) {
                            var bottom = bottomInset
                            if (shouldShowNavigationBar) bottom += NavigationBarHeight
                            if (!playerBottomSheetState.isDismissed) bottom += MiniPlayerHeight
                            windowsInsets
                                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                                .add(WindowInsets(bottom = bottom))
                        }

                    val topLevelScreens = listOf(
                        Screens.Home.route,
                        Screens.Search.route,
                        Screens.Library.route
                    )

                    CompositionLocalProvider(
                        LocalPlayerConnection provides connection,
                        LocalPlayerAwareWindowInsets provides playerAwareWindowInsets,
                        LocalShimmerTheme provides ShimmerTheme,
                    ) {
                        if (shouldUseSideNavigation) {
                            // Tablet layout with side navigation
                            Row(modifier = Modifier.fillMaxSize()) {
                                SideNavigationRail(
                                    navigationItems = navigationItems,
                                    currentDestination = navBackStackEntry?.destination,
                                    onNavigate = { route ->
                                        if (route == Screens.Search.route) {
                                            onActiveChange(true)
                                        } else {
                                            val isSelected = navBackStackEntry?.destination?.hierarchy?.any { it.route == route } == true
                                            if (isSelected) {
                                                navController.backToMain()
                                            } else {
                                                navController.navigate(route) {
                                                    popUpTo(navController.graph.startDestinationId) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        }
                                        previousTab = route
                                    },
                                    pureBlack = pureBlack?.get(PureBlackKey) == true
                                )
                                
                                // Main content area for tablet
                                Box(modifier = Modifier.weight(1f)) {
                                    Scaffold(
                                        topBar = {
                                            if (shouldShowTopBar) {
                                                TopAppBar(
                                                    title = {
                                                        Text(
                                                            text = currentTitleRes?.let { stringResource(it) } ?: "",
                                                            style = MaterialTheme.typography.titleLarge,
                                                        )
                                                    },
                                                    actions = {
                                                        IconButton(onClick = { navController.navigate("history") }) {
                                                            Icon(
                                                                painter = painterResource(R.drawable.history),
                                                                contentDescription = stringResource(R.string.history)
                                                            )
                                                        }
                                                        IconButton(onClick = { navController.navigate("stats") }) {
                                                            Icon(
                                                                painter = painterResource(R.drawable.stats),
                                                                contentDescription = stringResource(R.string.stats)
                                                            )
                                                        }
                                                        IconButton(onClick = { showAccountDialog = true }) {
                                                            BadgedBox(badge = {
                                                                if (latestVersionName != BuildConfig.VERSION_NAME) {
                                                                    Badge()
                                                                }
                                                            }) {
                                                                if (accountImageUrl != null) {
                                                                    AsyncImage(
                                                                        model = accountImageUrl,
                                                                        contentDescription = stringResource(R.string.account),
                                                                        modifier = Modifier
                                                                            .size(24.dp)
                                                                            .clip(CircleShape),
                                                                        placeholder = painterResource(R.drawable.person),
                                                                        error = painterResource(R.drawable.person),
                                                                    )
                                                                } else {
                                                                    Icon(
                                                                        painter = painterResource(R.drawable.person),
                                                                        contentDescription = null,
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    },
                                                )
                                            }
                                        }
                                    ) { innerPadding ->
                                        TabletContent(
                                            navController = navController,
                                            navigationItems = navigationItems,
                                            query = query,
                                            onQueryChange = onQueryChange,
                                            active = active,
                                            onActiveChange = onActiveChange,
                                            searchSource = searchSource,
                                            onSearchSourceChange = { searchSource = it },
                                            searchBarFocusRequester = searchBarFocusRequester,
                                            pureBlack = pureBlack?.get(PureBlackKey) == true,
                                            pauseSearchHistory = pauseSearchHistory?.get(PauseSearchHistoryKey) == true,
                                            playerBottomSheetState = playerBottomSheetState,
                                            topLevelScreens = topLevelScreens,
                                            tabOpenedFromShortcut = tabOpenedFromShortcut,
                                            defaultOpenTab = defaultOpenTab?.get(DefaultOpenTabKey)?.toEnum(NavigationTab.HOME) ?: NavigationTab.HOME,
                                            latestVersionName = latestVersionName,
                                            innerPadding = innerPadding
                                        )
                                    }
                                    
                                    // Bottom sheet player for tablet (positioned at bottom right)
                                    BottomSheetPlayer(
                                        state = playerBottomSheetState,
                                        navController = navController,
                                        pureBlack = pureBlack?.get(PureBlackKey) == true
                                    )
                                }
                            }
                        } else {
                            // Phone layout with bottom navigation (original)
                            PhoneContent(
                                navController = navController,
                                navigationItems = navigationItems,
                                query = query,
                                onQueryChange = onQueryChange,
                                active = active,
                                onActiveChange = onActiveChange,
                                searchSource = searchSource,
                                onSearchSourceChange = { searchSource = it },
                                searchBarFocusRequester = searchBarFocusRequester,
                                shouldShowTopBar = shouldShowTopBar,
                                currentTitleRes = currentTitleRes,
                                navigationBarHeight = navigationBarHeight,
                                playerBottomSheetState = playerBottomSheetState,
                                bottomInset = bottomInset,
                                bottomInsetDp = bottomInsetDp,
                                pureBlack = pureBlack?.get(PureBlackKey) == true,
                                slimNav = slimNav?.get(SlimNavigationKey) == true,
                                pauseSearchHistory = pauseSearchHistory?.get(PauseSearchHistoryKey) == true,
                                topLevelScreens = topLevelScreens,
                                tabOpenedFromShortcut = tabOpenedFromShortcut,
                                defaultOpenTab = defaultOpenTab?.get(DefaultOpenTabKey)?.toEnum(NavigationTab.HOME) ?: NavigationTab.HOME,
                                latestVersionName = latestVersionName,
                                accountImageUrl = accountImageUrl,
                                showAccountDialog = showAccountDialog,
                                onAccountDialogToggle = { showAccountDialog = it }
                            )
                        }

                        BottomSheetMenu(
                            state = LocalMenuState.current,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )

                        BottomSheetPage(
                            state = LocalBottomSheetPageState.current,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )

                        if (showAccountDialog) {
                            AccountSettingsDialog(
                                navController = navController,
                                onDismiss = { showAccountDialog = false },
                                latestVersionName = latestVersionName
                            )
                        }

                        sharedSong?.let { song ->
                            playerConnection?.let {
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
                            try {
                                delay(100)
                                searchBarFocusRequester.requestFocus()
                            } catch (_: Exception) {
                            }
                            openSearchImmediately = false
                        }
                    }
                }
            }
        }
    }

    private fun handleDeepLinkIntent(intent: Intent, navController: NavHostController) {
        val uri = intent.data ?: intent.extras?.getString(Intent.EXTRA_TEXT)?.toUri() ?: return
        when (uri.host) {
            "youtu.be" -> {
                val videoId = uri.lastPathSegment
                navController.navigate("artist/${videoId}")
            }
            "youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com" -> {
                val videoId = uri.getQueryParameter("v")
                val playlistId = uri.getQueryParameter("list")
                val channelId = uri.getQueryParameter("channel")
                when {
                    playlistId != null -> navController.navigate("online_playlist/$playlistId")
                    videoId != null -> navController.navigate("artist/$videoId")
                    channelId != null -> navController.navigate("artist/$channelId")
                    uri.pathSegments.firstOrNull() == "channel" -> uri.lastPathSegment?.let { artistId ->
                        navController.navigate("artist/$artistId")
                    }
                }
            }
        }
    }
}
