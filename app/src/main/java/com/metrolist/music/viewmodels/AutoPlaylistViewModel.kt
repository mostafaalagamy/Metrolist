package com.metrolist.music.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.offline.Download
import com.metrolist.music.constants.SongSortDescendingKey
import com.metrolist.music.constants.SongSortType
import com.metrolist.music.constants.SongSortTypeKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.extensions.reversed
import com.metrolist.music.extensions.toEnum
import com.metrolist.music.playback.DownloadUtil
import com.metrolist.music.utils.SyncUtils
import com.metrolist.music.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AutoPlaylistViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    downloadUtil: DownloadUtil,
    savedStateHandle: SavedStateHandle,
    private val syncUtils: SyncUtils,
) : ViewModel() {
    val playlist = savedStateHandle.get<String>("playlist")!!

    @OptIn(ExperimentalCoroutinesApi::class)
    val likedSongs =
        context.dataStore.data
            .map {
                it[SongSortTypeKey].toEnum(SongSortType.CREATE_DATE) to (it[SongSortDescendingKey]
                    ?: true)
            }
            .distinctUntilChanged()
            .flatMapLatest { (sortType, descending) ->
                when (playlist) {
                    "liked" -> database.likedSongs(sortType, descending)
                    "downloaded" -> downloadUtil.downloads.flatMapLatest { downloads ->
                        database.allSongs()
                            .flowOn(Dispatchers.IO)
                            .map { songs ->
                                songs.filter {
                                    downloads[it.id]?.state == Download.STATE_COMPLETED
                                }
                            }
                            .map { songs ->
                                when (sortType) {
                                    SongSortType.CREATE_DATE -> songs.sortedBy {
                                        downloads[it.id]?.updateTimeMs ?: 0L
                                    }

                                    SongSortType.NAME -> songs.sortedBy { it.song.title }
                                    SongSortType.ARTIST -> songs.sortedBy { song ->
                                        song.artists.joinToString(separator = "") { it.name }
                                    }

                                    SongSortType.PLAY_TIME -> songs.sortedBy { it.song.totalPlayTime }
                                }.reversed(descending)
                            }
                    }

                    else -> MutableStateFlow(emptyList())
                }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun syncLikedSongs() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncLikedSongs() }
    }
}
