package com.zionhuang.music.ui.screens.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.zionhuang.music.ui.component.ChipsRow
import com.zionhuang.music.R
import com.zionhuang.music.constants.LibraryFilter
import com.zionhuang.music.viewmodels.LibraryViewModel

@Composable
fun LibraryScreen(
    navController: NavController,
    viewModel: LibraryViewModel = hiltViewModel(),
){
    val filter by remember { mutableStateOf(viewModel.filter) }

    val filterContent = @Composable {
        Row {
            ChipsRow(
                chips = listOf(
                    LibraryFilter.PLAYLISTS to stringResource(R.string.filter_playlists),
                    LibraryFilter.SONGS to stringResource(R.string.filter_songs),
                    LibraryFilter.ALBUMS to stringResource(R.string.filter_albums),
                    LibraryFilter.ARTISTS to stringResource(R.string.filter_artists)
                ),
                currentValue = filter.value,
                onValueUpdate = {
                    filter.value = if (filter.value == it){
                        LibraryFilter.LIBRARY
                    } else {
                        it
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when (filter.value) {
            LibraryFilter.LIBRARY -> LibraryMixScreen(navController, filterContent)
            LibraryFilter.PLAYLISTS -> LibraryPlaylistsScreen(navController, filterContent)
            LibraryFilter.SONGS -> LibrarySongsScreen(navController, filterContent)
            LibraryFilter.ALBUMS -> LibraryAlbumsScreen(navController, filterContent)
            LibraryFilter.ARTISTS -> LibraryArtistsScreen(navController, filterContent)
        }
    }
}