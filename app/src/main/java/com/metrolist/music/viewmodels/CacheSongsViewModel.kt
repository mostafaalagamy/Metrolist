package com.metrolist.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.constants.SongSortDescendingKey
import com.metrolist.music.constants.SongSortType
import com.metrolist.music.constants.SongSortTypeKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.Song
import com.metrolist.music.extensions.reversed
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CacheSongsViewModel @Inject constructor(
    database: MusicDatabase
) : ViewModel() {
    
    val cachedSongs = database.songs()
        .map { songs -> songs.filter { it.isCached } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun sortSongs(songs: List<Song>, sortType: SongSortType, descending: Boolean): List<Song> {
        return when (sortType) {
            SongSortType.CREATE_DATE -> songs.sortedBy { it.song.createDate }
            SongSortType.NAME -> songs.sortedBy { it.song.title }
            SongSortType.ARTIST -> songs.sortedBy { song -> 
                song.artists.joinToString("") { it.name }
            }
            SongSortType.PLAY_TIME -> songs.sortedBy { it.song.totalPlayTime }
        }.let { if (descending) it.reversed() else it }
    }
}
