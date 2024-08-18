package com.metrolist.music.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.offline.Download
import com.metrolist.music.constants.PlaylistSongSortDescendingKey
import com.metrolist.music.constants.PlaylistSongSortType
import com.metrolist.music.constants.PlaylistSongSortTypeKey
import com.metrolist.music.constants.SongSortType
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.extensions.reversed
import com.metrolist.music.extensions.toEnum
import com.metrolist.music.playback.DownloadUtil
import com.metrolist.music.utils.dataStore
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
import java.text.Collator
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AutoPlaylistViewModel
    @Inject
    constructor(
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
                            it[PlaylistSongSortTypeKey].toEnum(PlaylistSongSortType.CUSTOM) to
                                (it[PlaylistSongSortDescendingKey] ?: true)
                        }.distinctUntilChanged(),
                ) { songs, (sortType, sortDescending) ->
                    when (sortType) {
                            PlaylistSongSortType.CUSTOM -> songs
                            PlaylistSongSortType.CREATE_DATE -> songs.sortedBy { it.id }
                            PlaylistSongSortType.NAME -> songs.sortedBy { it.song.title }
                            PlaylistSongSortType.ARTIST -> {
                            val collator = Collator.getInstance(Locale.getDefault())
                            collator.strength = Collator.PRIMARY
                            songs
                                .sortedWith(compareBy(collator) { song -> song.artists.joinToString("") { it.name } })
                                .groupBy { it.album?.title }
                                .flatMap { (_, songsByAlbum) -> songsByAlbum.sortedBy { it.artists.joinToString("") { it.name } } }
                        }
                        PlaylistSongSortType.PLAY_TIME -> songs.sortedBy { it.song.totalPlayTime }
                    }.reversed(sortDescending && sortType != PlaylistSongSortType.CUSTOM)
                }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
                    .stateIn(viewModelScope, SharingStarted.Lazily, null)
            } else {
                downloadUtil.downloads.flatMapLatest { downloads ->
                    database
                        .allSongs()
                        .flowOn(Dispatchers.IO)
                        .map { songs ->
                            songs.filter {
                                downloads[it.id]?.state == Download.STATE_COMPLETED
                            }
                        }
                }
            }
    }
