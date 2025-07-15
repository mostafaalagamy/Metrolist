package com.metrolist.music.ui.component

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import coil.compose.AsyncImage
import com.metrolist.music.BuildConfig
import com.metrolist.music.R
import com.metrolist.music.constants.MiniPlayerHeight
import com.metrolist.music.constants.NavigationBarHeight
import com.metrolist.music.constants.SearchSource
import com.metrolist.music.extensions.fastAny
import com.metrolist.music.extensions.fastFirstOrNull
import com.metrolist.music.extensions.fastForEach
import com.metrolist.music.ui.screens.LocalSearchScreen
import com.metrolist.music.ui.screens.NavigationTab
import com.metrolist.music.ui.screens.OnlineSearchScreen
import com.metrolist.music.ui.screens.Screens
import com.metrolist.music.ui.screens.navigationBuilder
import com.metrolist.music.ui.utils.backToMain
import kotlinx.coroutines.launch
import java.net.URLEncoder
import kotlin.math.roundToPx

@Composable
fun PhoneContent(
    navController: NavHostController,
    navigationItems: List<Screens>,
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    searchSource: SearchSource,
    onSearchSourceChange: (SearchSource) -> Unit,
    searchBarFocusRequester: FocusRequester,
    shouldShowTopBar: Boolean,
    currentTitleRes: Int?,
    navigationBarHeight: Dp,
    playerBottomSheetState: androidx.compose.material3.rememberModalBottomSheetState.BottomSheetState,
    bottomInset: Dp,
    bottomInsetDp: Dp,
    pureBlack: Boolean,
    slimNav: Boolean,
    pauseSearchHistory: Boolean,
    topLevelScreens: List<String>,
    tabOpenedFromShortcut: NavigationTab?,
    defaultOpenTab: NavigationTab,
    latestVersionName: String?,
    accountImageUrl: String?,
    showAccountDialog: Boolean,
    onAccountDialogToggle: (Boolean) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val searchBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    
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
                        IconButton(onClick = { onAccountDialogToggle(true) }) {
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
        },
        content = { innerPadding ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                SearchBar(
                    query = query.text,
                    onQueryChange = onQueryChange,
                    onSearch = { searchText ->
                        navController.navigate("search/${URLEncoder.encode(searchText, "UTF-8")}")
                        if (!pauseSearchHistory) {
                            // Add to search history
                            // Note: This would need database access
                        }
                    },
                    active = active,
                    onActiveChange = onActiveChange,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search),
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp)
                        )
                    },
                    leadingIcon = {
                        IconButton(
                            onClick = {
                                when {
                                    active -> onActiveChange(false)
                                    !navigationItems.fastAny { it.route == navController.currentBackStackEntry?.destination?.route } -> {
                                        navController.navigateUp()
                                    }
                                    else -> onActiveChange(true)
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (active || !navigationItems.fastAny { it.route == navController.currentBackStackEntry?.destination?.route }) {
                                        R.drawable.arrow_back
                                    } else {
                                        R.drawable.search
                                    }
                                ),
                                contentDescription = null
                            )
                        }
                    },
                    trailingIcon = {
                        Row {
                            if (active) {
                                if (query.text.isNotEmpty()) {
                                    IconButton(
                                        onClick = { onQueryChange(TextFieldValue("")) }
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.close),
                                            contentDescription = null
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        onSearchSourceChange(
                                            if (searchSource == SearchSource.ONLINE) SearchSource.LOCAL else SearchSource.ONLINE
                                        )
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            when (searchSource) {
                                                SearchSource.LOCAL -> R.drawable.library_music
                                                SearchSource.ONLINE -> R.drawable.language
                                            }
                                        ),
                                        contentDescription = null
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .focusRequester(searchBarFocusRequester)
                        .align(Alignment.TopCenter),
                    colors = if (pureBlack && active) {
                        SearchBarDefaults.colors(
                            containerColor = Color.Black,
                            dividerColor = Color.DarkGray,
                            inputFieldColors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.Gray,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                cursorColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            )
                        )
                    } else {
                        SearchBarDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    }
                ) {
                    Crossfade(
                        targetState = searchSource,
                        label = "search_source",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = if (!playerBottomSheetState.isDismissed) MiniPlayerHeight else 0.dp)
                            .navigationBarsPadding()
                    ) { currentSearchSource ->
                        when (currentSearchSource) {
                            SearchSource.LOCAL -> LocalSearchScreen(
                                query = query.text,
                                navController = navController,
                                onDismiss = { onActiveChange(false) },
                                pureBlack = pureBlack
                            )
                            SearchSource.ONLINE -> OnlineSearchScreen(
                                query = query.text,
                                onQueryChange = onQueryChange,
                                navController = navController,
                                onSearch = { searchText ->
                                    navController.navigate("search/${URLEncoder.encode(searchText, "UTF-8")}")
                                    if (!pauseSearchHistory) {
                                        // Add to search history
                                    }
                                },
                                onDismiss = { onActiveChange(false) },
                                pureBlack = pureBlack
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Box {
                BottomSheetPlayer(
                    state = playerBottomSheetState,
                    navController = navController,
                    pureBlack = pureBlack
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
                                            playerBottomSheetState.progress.coerceIn(0f, 1f)
                                val hideOffset =
                                    (bottomInset + NavigationBarHeight) * (1 - navigationBarHeight / NavigationBarHeight)
                                IntOffset(
                                    x = 0,
                                    y = (slideOffset + hideOffset).roundToPx(),
                                )
                            }
                        },
                    containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    navigationItems.fastForEach { screen ->
                        val isSelected =
                            navController.currentBackStackEntry?.destination?.hierarchy?.any { it.route == screen.route } == true

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
                                if (screen.route == Screens.Search.route) {
                                    onActiveChange(true)
                                } else if (isSelected) {
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
                
                val baseBg = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer
                val insetBg = if (playerBottomSheetState.progress > 0f) Color.Transparent else baseBg

                Box(
                    modifier = Modifier
                        .background(insetBg)
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .height(bottomInsetDp)
                )
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(searchBarScrollBehavior.nestedScrollConnection)
    ) {
        var transitionDirection = AnimatedContentTransitionScope.SlideDirection.Left

        if (navigationItems.fastAny { it.route == navController.currentBackStackEntry?.destination?.route }) {
            // Handle transition direction based on tab navigation
            // This is simplified for now
        }

        NavHost(
            navController = navController,
            startDestination = when (tabOpenedFromShortcut ?: defaultOpenTab) {
                NavigationTab.HOME -> Screens.Home
                NavigationTab.LIBRARY -> Screens.Library
                else -> Screens.Home
            }.route,
            enterTransition = {
                if (initialState.destination.route in topLevelScreens && targetState.destination.route in topLevelScreens) {
                    fadeIn(tween(250))
                } else {
                    fadeIn(tween(250)) + slideInHorizontally { it / 2 }
                }
            },
            exitTransition = {
                if (initialState.destination.route in topLevelScreens && targetState.destination.route in topLevelScreens) {
                    fadeOut(tween(200))
                } else {
                    fadeOut(tween(200)) + slideOutHorizontally { -it / 2 }
                }
            },
            popEnterTransition = {
                if (initialState.destination.route in topLevelScreens && targetState.destination.route in topLevelScreens) {
                    fadeIn(tween(250))
                } else {
                    fadeIn(tween(250)) + slideInHorizontally { -it / 2 }
                }
            },
            popExitTransition = {
                if (initialState.destination.route in topLevelScreens && targetState.destination.route in topLevelScreens) {
                    fadeOut(tween(200))
                } else {
                    fadeOut(tween(200)) + slideOutHorizontally { it / 2 }
                }
            },
        ) {
            navigationBuilder(
                navController,
                searchBarScrollBehavior,
                latestVersionName
            )
        }
    }
}