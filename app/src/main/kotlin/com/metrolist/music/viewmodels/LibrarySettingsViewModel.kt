package com.metrolist.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.repositories.BlockedContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibrarySettingsViewModel @Inject constructor(
    private val repository: BlockedContentRepository
) : ViewModel() {

    val blockedSongs = repository.blockedSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blockedArtists = repository.blockedArtists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blockedAlbums = repository.blockedAlbums
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun unblockSong(songId: String) {
        viewModelScope.launch {
            repository.unblockSong(songId)
        }
    }

    fun unblockArtist(artistId: String) {
        viewModelScope.launch {
            repository.unblockArtist(artistId)
        }
    }

    fun unblockAlbum(albumId: String) {
        viewModelScope.launch {
            repository.unblockAlbum(albumId)
        }
    }
}
