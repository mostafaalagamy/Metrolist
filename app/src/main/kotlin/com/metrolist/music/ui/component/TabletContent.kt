package com.metrolist.music.ui.component

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.metrolist.music.R
import com.metrolist.music.constants.MiniPlayerHeight
import com.metrolist.music.constants.PauseSearchHistoryKey
import com.metrolist.music.constants.SearchSource
import com.metrolist.music.db.entities.SearchHistory
import com.metrolist.music.extensions.fastAny
import com.metrolist.music.ui.component.rememberBottomSheetState
import com.metrolist.music.ui.screens.LocalSearchScreen
import com.metrolist.music.ui.screens.NavigationTab
import com.metrolist.music.ui.screens.OnlineSearchScreen
import com.metrolist.music.ui.screens.Screens
import com.metrolist.music.ui.screens.navigationBuilder
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import java.net.URLEncoder

@Composable
fun TabletContent(
    navController: NavHostController,
    navigationItems: List<Screens>,
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    searchSource: SearchSource,
    onSearchSourceChange: (SearchSource) -> Unit,
    searchBarFocusRequester: FocusRequester,
    pureBlack: Boolean,
    pauseSearchHistory: Boolean,
    playerBottomSheetState: androidx.compose.material3.rememberModalBottomSheetState.BottomSheetState,
    topLevelScreens: List<String>,
    tabOpenedFromShortcut: NavigationTab?,
    defaultOpenTab: NavigationTab,
    latestVersionName: String?,
    innerPadding: PaddingValues
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        if (active || navigationItems.fastAny { it.route == navController.currentBackStackEntry?.destination?.route }) {
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
        } else {
            // Main navigation content
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
                }
            ) {
                navigationBuilder(
                    navController,
                    null, // topAppBarScrollBehavior for tablet
                    latestVersionName
                )
            }
        }
    }
}