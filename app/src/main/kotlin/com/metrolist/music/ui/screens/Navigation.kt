package com.metrolist.music.ui.screens

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.metrolist.music.ui.screens.explore.ExploreScreen
import com.metrolist.music.ui.screens.explore.ExploreViewModel
import com.metrolist.music.ui.screens.home.HomeScreen
import com.metrolist.music.ui.screens.home.HomeViewModel
import com.metrolist.music.ui.screens.library.LibraryScreen
import com.metrolist.music.ui.screens.library.LibraryViewModel
import com.metrolist.music.ui.screens.search.SearchScreen
import com.metrolist.music.ui.screens.search.SearchViewModel
import com.metrolist.music.ui.screens.settings.SettingsScreen
import com.metrolist.music.ui.screens.settings.SettingsViewModel
import com.metrolist.music.utils.dataStore
import java.net.URLDecoder

// Define standard M3 Expressive transition durations
private const val NavTransitionDuration = 500 // Slightly longer for expressive feel

// Define standard M3 Expressive enter/exit transitions (Example: Fade + Slide)
private val enterTransition: AnimatedContentTransitionScope<*>.() -> EnterTransition = {
    fadeIn(animationSpec = tween(NavTransitionDuration)) +
            slideInHorizontally(initialOffsetX = { it / 4 }, animationSpec = tween(NavTransitionDuration))
}

private val exitTransition: AnimatedContentTransitionScope<*>.() -> ExitTransition = {
    fadeOut(animationSpec = tween(NavTransitionDuration / 2)) // Faster fade out
}

private val popEnterTransition: AnimatedContentTransitionScope<*>.() -> EnterTransition = {
    fadeIn(animationSpec = tween(NavTransitionDuration))
}

private val popExitTransition: AnimatedContentTransitionScope<*>.() -> ExitTransition = {
    fadeOut(animationSpec = tween(NavTransitionDuration / 2)) +
            slideOutHorizontally(targetOffsetX = { it / 4 }, animationSpec = tween(NavTransitionDuration))
}

fun NavGraphBuilder.navigationBuilder(
    navController: NavHostController,
    startDestination: String,
    pureBlack: Boolean,
) {
    // Main navigation graph
    composable(
        route = Screens.Home.route,
        enterTransition = enterTransition,
        exitTransition = exitTransition,
        popEnterTransition = popEnterTransition,
        popExitTransition = popExitTransition
    ) {
        val viewModel = hiltViewModel<HomeViewModel>()
        val scrollToTop by viewModel.scrollToTop.collectAsState(initial = false)
        LaunchedEffect(scrollToTop) {
            if (scrollToTop) {
                // Reset scroll state or trigger scroll action here
                viewModel.resetScrollToTop()
            }
        }
        HomeScreen(navController = navController, viewModel = viewModel, pureBlack = pureBlack)
    }
    composable(
        route = Screens.Explore.route,
        enterTransition = enterTransition,
        exitTransition = exitTransition,
        popEnterTransition = popEnterTransition,
        popExitTransition = popExitTransition
    ) {
        val viewModel = hiltViewModel<ExploreViewModel>()
        val scrollToTop by viewModel.scrollToTop.collectAsState(initial = false)
        LaunchedEffect(scrollToTop) {
            if (scrollToTop) {
                // Reset scroll state or trigger scroll action here
                viewModel.resetScrollToTop()
            }
        }
        ExploreScreen(navController = navController, viewModel = viewModel, pureBlack = pureBlack)
    }
    composable(
        route = Screens.Library.route,
        enterTransition = enterTransition,
        exitTransition = exitTransition,
        popEnterTransition = popEnterTransition,
        popExitTransition = popExitTransition
    ) {
        val viewModel = hiltViewModel<LibraryViewModel>()
        val scrollToTop by viewModel.scrollToTop.collectAsState(initial = false)
        LaunchedEffect(scrollToTop) {
            if (scrollToTop) {
                // Reset scroll state or trigger scroll action here
                viewModel.resetScrollToTop()
            }
        }
        LibraryScreen(navController = navController, viewModel = viewModel, pureBlack = pureBlack)
    }

    // Search Screen
    composable(
        route = "search/{query}",
        arguments = listOf(navArgument("query") { type = NavType.StringType }),
        enterTransition = enterTransition,
        exitTransition = exitTransition,
        popEnterTransition = popEnterTransition,
        popExitTransition = popExitTransition
    ) { backStackEntry ->
        val query = remember { URLDecoder.decode(backStackEntry.arguments?.getString("query") ?: "", "UTF-8") }
        val viewModel = hiltViewModel<SearchViewModel>()
        SearchScreen(query = query, navController = navController, viewModel = viewModel, pureBlack = pureBlack)
    }

    // Settings Navigation Graph (Example of nested graph)
    navigation(
        startDestination = "settings_main",
        route = "settings",
        enterTransition = { fadeIn(animationSpec = tween(NavTransitionDuration)) },
        exitTransition = { fadeOut(animationSpec = tween(NavTransitionDuration / 2)) }
    ) {
        composable(
            route = "settings_main",
            enterTransition = enterTransition,
            exitTransition = exitTransition,
            popEnterTransition = popEnterTransition,
            popExitTransition = popExitTransition
        ) {
            val viewModel = hiltViewModel<SettingsViewModel>()
            SettingsScreen(navController = navController, viewModel = viewModel, pureBlack = pureBlack)
        }
        // Add other settings screens here if needed
    }
}

