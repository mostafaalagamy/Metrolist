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
    
    private val _sortType = MutableStateFlow(SongSortType.CREATE_DATE)
    private val _sortDescending = MutableStateFlow(true)

    val cachedSongs = database.songs()
        .map { songs -> 
            songs.filter { song -> 
                song.song.dateDownload != null
            }.sortedWith(getComparator(_sortType.value, _sortDescending.value))
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private fun getComparator(
        sortType: SongSortType, 
        descending: Boolean
    ): Comparator<Song> {
        val comparator = when (sortType) {
            SongSortType.CREATE_DATE -> compareBy { it.song.dateDownload ?: it.song.inLibrary }
            SongSortType.NAME -> compareBy { it.song.title }
            SongSortType.ARTIST -> compareBy { song -> 
                song.artists.joinToString("") { it.name }
            }
            SongSortType.PLAY_TIME -> compareBy { it.song.totalPlayTime }
        }
        return if (descending) comparator.reversed() else comparator
    }

    fun updateSort(sortType: SongSortType, descending: Boolean) {
        _sortType.value = sortType
        _sortDescending.value = descending
    }
}
