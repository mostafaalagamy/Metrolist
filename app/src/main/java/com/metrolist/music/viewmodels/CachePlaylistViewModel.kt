package com.metrolist.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import com.metrolist.music.di.PlayerCache
import androidx.media3.datasource.cache.SimpleCache

@HiltViewModel
class CachePlaylistViewModel @Inject constructor(
    private val database: MusicDatabase,
    @PlayerCache private val playerCache: SimpleCache
) : ViewModel() {

    val cachedSongs: StateFlow<List<Song>> =
        database.observeDownloadedSongs()
            .map { songs ->
                val cachedIds = playerCache.keys.mapNotNull { it?.toString() }.toSet()
                songs.filter { cachedIds.contains(it.id) }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
}
