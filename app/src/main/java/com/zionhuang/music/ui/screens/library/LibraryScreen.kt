package com.zionhuang.music.ui.screens.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.zionhuang.music.ui.component.ChipsRow
import com.zionhuang.music.R
import com.zionhuang.music.constants.LibraryFilter
import com.zionhuang.music.constants.LibraryFilterKey
import com.zionhuang.music.utils.rememberEnumPreference

@Composable
fun LibraryScreen(
    navController: NavController,
){
    var filter by rememberEnumPreference(LibraryFilterKey, LibraryFilter.PLAYLISTS)

    val filterContent = @Composable {
        Row {
            ChipsRow(
                chips = listOf(
                    LibraryFilter.PLAYLISTS to stringResource(R.string.filter_playlists),
                    LibraryFilter.SONGS to stringResource(R.string.filter_songs),
                    LibraryFilter.ALBUMS to stringResource(R.string.filter_albums),
                    LibraryFilter.ARTISTS to stringResource(R.string.filter_artists)
                ),
                currentValue = filter,
                onValueUpdate = { filter = it },
                modifier = Modifier.weight(1f)
            )
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when (filter) {
            LibraryFilter.PLAYLISTS -> LibraryPlaylistsScreen(navController, filterContent)
            LibraryFilter.SONGS -> LibrarySongsScreen(navController, filterContent)
            LibraryFilter.ALBUMS -> LibraryAlbumsScreen(navController, filterContent)
            LibraryFilter.ARTISTS -> LibraryArtistsScreen(navController, filterContent)
        }
    }
}