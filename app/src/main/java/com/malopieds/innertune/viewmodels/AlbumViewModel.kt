package com.malopieds.innertune.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malopieds.innertube.YouTube
import com.malopieds.innertune.db.MusicDatabase
import com.malopieds.innertune.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumViewModel @Inject constructor(
    database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val albumId = savedStateHandle.get<String>("albumId")!!
    val albumWithSongs = database.albumWithSongs(albumId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            val album = database.album(albumId).first()
            if (album == null || album.album.songCount == 0) {
                YouTube.album(albumId).onSuccess {
                    database.transaction {
                        if (album == null) insert(it)
                        else update(album.album, it)
                    }
                }.onFailure {
                    reportException(it)
                    if (it.message?.contains("NOT_FOUND") == true) {
                        database.query {
                            album?.album?.let(::delete)
                        }
                    }
                }
            }
        }
    }
}
