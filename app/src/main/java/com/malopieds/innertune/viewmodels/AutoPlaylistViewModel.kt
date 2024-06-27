package com.malopieds.innertune.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.offline.Download
import com.malopieds.innertune.constants.PlaylistSongSortDescendingKey
import com.malopieds.innertune.constants.PlaylistSongSortType
import com.malopieds.innertune.constants.PlaylistSongSortTypeKey
import com.malopieds.innertune.constants.SongSortType
import com.malopieds.innertune.db.MusicDatabase
import com.malopieds.innertune.extensions.reversed
import com.malopieds.innertune.extensions.toEnum
import com.malopieds.innertune.playback.DownloadUtil
import com.malopieds.innertune.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AutoPlaylistViewModel  @Inject constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    downloadUtil: DownloadUtil,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val playlist = savedStateHandle.get<String>("playlist")!!
    @OptIn(ExperimentalCoroutinesApi::class)
    val likedSongs =
        if (playlist == "liked") {
            combine(
                database.likedSongs(SongSortType.CREATE_DATE, true),
                context.dataStore.data
                    .map {
                        it[PlaylistSongSortTypeKey].toEnum(PlaylistSongSortType.CUSTOM) to (it[PlaylistSongSortDescendingKey] ?: true)
                    }
                    .distinctUntilChanged()
            ) { songs, (sortType, sortDescending) ->
                when (sortType) {
                    PlaylistSongSortType.CUSTOM -> songs
                    PlaylistSongSortType.CREATE_DATE -> songs.sortedBy { it.id }
                    PlaylistSongSortType.NAME -> songs.sortedBy { it.song.title }
                    PlaylistSongSortType.ARTIST -> songs.sortedBy { song ->
                        song.artists.joinToString { it.name }
                    }
                    PlaylistSongSortType.PLAY_TIME -> songs.sortedBy { it.song.totalPlayTime }
                }.reversed(sortDescending && sortType != PlaylistSongSortType.CUSTOM)
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        .stateIn(viewModelScope, SharingStarted.Lazily, null)
        } else {
            downloadUtil.downloads.flatMapLatest { downloads ->
                database.allSongs()
                    .flowOn(Dispatchers.IO)
                    .map { songs ->
                        songs.filter {
                            downloads[it.id]?.state == Download.STATE_COMPLETED
                        }
                    }
        }
    }
}