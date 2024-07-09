package com.malopieds.innertune.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malopieds.innertune.constants.PlaylistSongSortDescendingKey
import com.malopieds.innertune.constants.PlaylistSongSortType
import com.malopieds.innertune.constants.PlaylistSongSortTypeKey
import com.malopieds.innertune.db.MusicDatabase
import com.malopieds.innertune.extensions.reversed
import com.malopieds.innertune.extensions.toEnum
import com.malopieds.innertune.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.Collator
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LocalPlaylistViewModel
    @Inject
    constructor(
        @ApplicationContext context: Context,
        database: MusicDatabase,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        val playlistId = savedStateHandle.get<String>("playlistId")!!
        val playlist =
            database
                .playlist(playlistId)
                .stateIn(viewModelScope, SharingStarted.Lazily, null)
        val playlistSongs =
            combine(
                database.playlistSongs(playlistId),
                context.dataStore.data
                    .map {
                        it[PlaylistSongSortTypeKey].toEnum(PlaylistSongSortType.CUSTOM) to (it[PlaylistSongSortDescendingKey] ?: true)
                    }.distinctUntilChanged(),
            ) { songs, (sortType, sortDescending) ->
                when (sortType) {
                    PlaylistSongSortType.CUSTOM -> songs
                    PlaylistSongSortType.CREATE_DATE -> songs.sortedBy { it.map.id }
                    PlaylistSongSortType.NAME -> songs.sortedBy { it.song.song.title }
                    PlaylistSongSortType.ARTIST -> {
                        val collator = Collator.getInstance(Locale.getDefault())
                        collator.strength = Collator.PRIMARY
                        songs.sortedWith(compareBy(collator) { song -> song.song.artists.joinToString("") { it.name } })
                    }
                    PlaylistSongSortType.PLAY_TIME -> songs.sortedBy { it.song.song.totalPlayTime }
                }.reversed(sortDescending && sortType != PlaylistSongSortType.CUSTOM)
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    }
