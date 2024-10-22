package com.metrolist.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor() : ViewModel() {
    val playlists = MutableStateFlow<List<PlaylistItem>?>(null)
    val albums = MutableStateFlow<List<AlbumItem>?>(null)

    init {
        viewModelScope.launch {
            YouTube.likedPlaylists().onSuccess {
                playlists.value = it
            }.onFailure {
                reportException(it)
            }
             YouTube.libraryAlbums().onSuccess {
                albums.value = it
            }.onFailure {
                reportException(it)
            }
        }
    }
}
