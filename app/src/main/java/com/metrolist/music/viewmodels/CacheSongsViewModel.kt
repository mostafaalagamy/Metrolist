package com.metrolist.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.constants.SongSortDescendingKey
import com.metrolist.music.constants.SongSortType
import com.metrolist.music.constants.SongSortTypeKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.extensions.reversed
import com.metrolist.music.extensions.toEnum
import com.metrolist.music.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CacheSongsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase
) : ViewModel() {

    val cachedSongs = context.dataStore.data
        .map {
            it[SongSortTypeKey].toEnum(SongSortType.CREATE_DATE) to 
            (it[SongSortDescendingKey] ?: true)
        }
        .distinctUntilChanged()
        .flatMapLatest { (sortType, descending) ->
            database.allSongs()
                .map { songs ->
                    songs.filter { it.song.dateDownload != null }
                        .sortedWith(getComparator(sortType, descending))
                }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private fun getComparator(
        sortType: SongSortType,
        descending: Boolean
    ): Comparator<Song> {
        return when (sortType) {
            SongSortType.CREATE_DATE -> compareBy { it.song.dateDownload ?: it.song.inLibrary }
            SongSortType.NAME -> compareBy { it.song.title }
            SongSortType.ARTIST -> compareBy { song ->
                song.artists.joinToString("") { it.name }
            }
            SongSortType.PLAY_TIME -> compareBy { it.song.totalPlayTime }
        }.let { if (descending) it.reversed() else it }
    }
}
