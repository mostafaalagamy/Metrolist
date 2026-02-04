/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import android.app.Activity
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import com.metrolist.music.constants.DarkModeKey
import com.metrolist.music.constants.PureBlackKey
import com.metrolist.music.eq.data.EQProfileRepository
import com.metrolist.music.ui.screens.artist.ArtistAlbumsScreen
import com.metrolist.music.ui.screens.artist.ArtistItemsScreen
import com.metrolist.music.ui.screens.artist.ArtistScreen
import com.metrolist.music.ui.screens.artist.ArtistSongsScreen
import com.metrolist.music.ui.screens.equalizer.EqScreen
import com.metrolist.music.ui.screens.library.LibraryScreen
import com.metrolist.music.ui.screens.playlist.AutoPlaylistScreen
import com.metrolist.music.ui.screens.playlist.CachePlaylistScreen
import com.metrolist.music.ui.screens.playlist.LocalPlaylistScreen
import com.metrolist.music.ui.screens.playlist.OnlinePlaylistScreen
import com.metrolist.music.ui.screens.playlist.TopPlaylistScreen
import com.metrolist.music.ui.screens.search.OnlineSearchResult
import com.metrolist.music.ui.screens.search.SearchScreen
import com.metrolist.music.ui.screens.settings.AboutScreen
import com.metrolist.music.ui.screens.settings.AccountSettings
import com.metrolist.music.ui.screens.settings.AppearanceSettings
import com.metrolist.music.ui.screens.settings.BackupAndRestore
import com.metrolist.music.ui.screens.settings.ContentSettings
import com.metrolist.music.ui.screens.settings.DarkMode
import com.metrolist.music.ui.screens.settings.DiscordLoginScreen
import com.metrolist.music.ui.screens.settings.PlayerSettings
import com.metrolist.music.ui.screens.settings.PrivacySettings
import com.metrolist.music.ui.screens.settings.RomanizationSettings
import com.metrolist.music.ui.screens.settings.SettingsScreen
import com.metrolist.music.ui.screens.settings.StorageSettings
import com.metrolist.music.ui.screens.settings.UpdaterScreen
import com.metrolist.music.ui.screens.settings.integrations.DiscordSettings
import com.metrolist.music.ui.screens.settings.integrations.IntegrationScreen
import com.metrolist.music.ui.screens.settings.integrations.LastFMSettings
import com.metrolist.music.ui.screens.wrapped.WrappedScreen
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.navigationBuilder(
    navController: NavHostController,
    scrollBehavior: TopAppBarScrollBehavior,
    latestVersionName: String,
    activity: Activity,
    snackbarHostState: SnackbarHostState
) {
    composable(route = Screens.Home.route, content = {
        HomeScreen(navController = navController, snackbarHostState = snackbarHostState)
    })

    composable(route = Screens.Search.route, content = {
        val pureBlackEnabled by rememberPreference(PureBlackKey, defaultValue = false)
        val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
        val isSystemInDarkTheme = isSystemInDarkTheme()
        val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
            if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
        }
        val pureBlack = remember(pureBlackEnabled, useDarkTheme) {
            pureBlackEnabled && useDarkTheme
        }
        SearchScreen(
            navController = navController,
            pureBlack = pureBlack
        )
    })

    composable(route = Screens.Library.route, content = {
        LibraryScreen(navController)
    })

    composable(route = "history", content = {
        HistoryScreen(navController)
    })

    composable(route = "stats", content = {
        StatsScreen(navController)
    })

    composable(route = "mood_and_genres", content = {
        MoodAndGenresScreen(navController, scrollBehavior)
    })

    composable(route = "account", content = {
        AccountScreen(navController, scrollBehavior)
    })

    composable(route = "new_release", content = {
        NewReleaseScreen(navController, scrollBehavior)
    })

    composable(route = "charts_screen", content = {
        ChartsScreen(navController)
    })

    composable(
        route = "browse/{browseId}",
        arguments = listOf(
            navArgument("browseId") {
                type = NavType.StringType
            }
        ),
        content = { backStackEntry ->
            BrowseScreen(
                navController,
                scrollBehavior,
                backStackEntry.arguments?.getString("browseId")
            )
        }
    )

    composable(
        route = "search/{query}",
        arguments = listOf(
            navArgument("query") {
                type = NavType.StringType
            },
        ),
        enterTransition = {
            fadeIn(tween(250))
        },
        exitTransition = {
            if (targetState.destination.route?.startsWith("search/") == true) {
                fadeOut(tween(200))
            } else {
                fadeOut(tween(200)) + slideOutHorizontally { offset -> -offset / 2 }
            }
        },
        popEnterTransition = {
            if (initialState.destination.route?.startsWith("search/") == true) {
                fadeIn(tween(250))
            } else {
                fadeIn(tween(250)) + slideInHorizontally { offset -> -offset / 2 }
            }
        },
        popExitTransition = {
            fadeOut(tween(200))
        },
        content = {
            OnlineSearchResult(navController)
        }
    )

    composable(
        route = "album/{albumId}",
        arguments = listOf(
            navArgument("albumId") {
                type = NavType.StringType
            },
        ),
        content = {
            AlbumScreen(navController, scrollBehavior)
        }
    )

    composable(
        route = "artist/{artistId}",
        arguments = listOf(
            navArgument("artistId") {
                type = NavType.StringType
            },
        ),
        content = {
            ArtistScreen(navController, scrollBehavior)
        }
    )

    composable(
        route = "artist/{artistId}/songs",
        arguments = listOf(
            navArgument("artistId") {
                type = NavType.StringType
            },
        ),
        content = {
            ArtistSongsScreen(navController, scrollBehavior)
        }
    )

    composable(
        route = "artist/{artistId}/albums",
        arguments = listOf(
            navArgument("artistId") {
                type = NavType.StringType
            }
        ),
        content = {
            ArtistAlbumsScreen(navController, scrollBehavior)
        }
    )

    composable(
        route = "artist/{artistId}/items?browseId={browseId}?params={params}",
        arguments = listOf(
            navArgument("artistId") {
                type = NavType.StringType
            },
            navArgument("browseId") {
                type = NavType.StringType
                nullable = true
            },
            navArgument("params") {
                type = NavType.StringType
                nullable = true
            },
        ),
        content = {
            ArtistItemsScreen(navController, scrollBehavior)
        }
    )

    composable(
        route = "online_playlist/{playlistId}",
        arguments = listOf(
            navArgument("playlistId") {
                type = NavType.StringType
            },
        ),
        content = {
            OnlinePlaylistScreen(navController, scrollBehavior)
        }
    )

    composable(
        route = "local_playlist/{playlistId}",
        arguments = listOf(
            navArgument("playlistId") {
                type = NavType.StringType
            },
        ),
        content = {
            LocalPlaylistScreen(navController, scrollBehavior)
        }
    )

    composable(
        route = "auto_playlist/{playlist}",
        arguments = listOf(
            navArgument("playlist") {
                type = NavType.StringType
            },
        ),
        content = {
            AutoPlaylistScreen(navController, scrollBehavior)
        }
    )

    composable(
        route = "cache_playlist/{playlist}",
        arguments = listOf(
            navArgument("playlist") {
                type = NavType.StringType
            },
        ),
        content = {
            CachePlaylistScreen(navController, scrollBehavior)
        }
    )

    composable(
        route = "top_playlist/{top}",
        arguments = listOf(
            navArgument("top") {
                type = NavType.StringType
            },
        ),
        content = {
            TopPlaylistScreen(navController, scrollBehavior)
        }
    )

    composable(
        route = "youtube_browse/{browseId}?params={params}",
        arguments = listOf(
            navArgument("browseId") {
                type = NavType.StringType
                nullable = true
            },
            navArgument("params") {
                type = NavType.StringType
                nullable = true
            },
        ),
        content = {
            YouTubeBrowseScreen(navController)
        }
    )

    composable(route = "settings", content = {
        SettingsScreen(navController, scrollBehavior, latestVersionName)
    })

    composable(route = "settings/appearance", content = {
        AppearanceSettings(navController, scrollBehavior, activity, snackbarHostState)
    })

    composable(route = "settings/content", content = {
        ContentSettings(navController, scrollBehavior)
    })

    composable(route = "settings/content/romanization", content = {
        RomanizationSettings(navController, scrollBehavior)
    })

    composable(route = "settings/player", content = {
        PlayerSettings(navController, scrollBehavior)
    })

    composable(route = "settings/storage", content = {
        StorageSettings(navController, scrollBehavior)
    })

    composable(route = "settings/privacy", content = {
        PrivacySettings(navController, scrollBehavior)
    })

    composable(route = "settings/backup_restore", content = {
        BackupAndRestore(navController, scrollBehavior)
    })

    composable(route = "settings/integrations", content = {
        IntegrationScreen(navController, scrollBehavior)
    })

    composable(route = "settings/integrations/discord", content = {
        DiscordSettings(navController, scrollBehavior)
    })

    composable(route = "settings/integrations/lastfm", content = {
        LastFMSettings(navController, scrollBehavior)
    })

    composable(route = "settings/discord/login", content = {
        DiscordLoginScreen(navController)
    })

    composable(route = "settings/updater", content = {
        UpdaterScreen(navController, scrollBehavior)
    })

    composable(route = "settings/about", content = {
        AboutScreen(navController, scrollBehavior)
    })

    composable(route = "login", content = {
        LoginScreen(navController)
    })

    composable(route = "wrapped", content = {
        WrappedScreen(navController)
    })

    dialog("equalizer") {
        EqScreen()
    }
}
